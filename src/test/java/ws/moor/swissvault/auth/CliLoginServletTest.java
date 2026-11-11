/*
 * Copyright 2026 Patrick Moor <patrick@moor.ws>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ws.moor.swissvault.auth;

import com.google.common.base.Optional;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonParser;
import com.google.inject.util.Providers;
import org.junit.Before;
import org.junit.Test;
import ws.moor.swissvault.util.UriBuilder;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class CliLoginServletTest {

  private static final SecretKeySpec HMAC_KEY = new SecretKeySpec(new byte[32], "HmacSHA256");
  private static final String EXPECTED_CSP =
      "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

  private Clock fixedClock;
  private UriBuilder uriBuilder;
  private AuthHelper authHelper;
  private OAuthStateHelper stateHelper;
  private AuthCookieFactory cookieFactory;

  @Before
  public void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
    uriBuilder = new UriBuilder("http", "localhost:8080");
    authHelper = new AuthHelper(HttpClient.newHttpClient(), new JsonParser(), uriBuilder, "secret", "client-id");
    stateHelper = new OAuthStateHelper(false);
    cookieFactory = new AuthCookieFactory(false, HMAC_KEY, fixedClock);
  }

  @Test
  public void testUnauthenticatedGetRedirectsToGoogleOAuth() throws IOException {
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.absent()), fixedClock);

    FakeResponse resp = new FakeResponse();
    HttpServletRequest req = createFakeRequest(Map.of(), Map.of());

    servlet.doGet(req, resp.asHttpServletResponse());

    assertThat(resp.redirectLocation).isNotNull();
    assertThat(resp.redirectLocation).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
    assertThat(resp.redirectLocation).contains("scope=openid");
    assertThat(resp.redirectLocation).contains("state=CLI_LOGIN:");

    assertThat(resp.headers.get(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    assertThat(resp.headers.get(HttpHeaders.CONTENT_SECURITY_POLICY)).contains(EXPECTED_CSP);

    List<String> setCookies = resp.headers.get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).isNotNull();
    assertThat(setCookies.get(0)).startsWith("oauth_state=CLI_LOGIN:");
  }

  @Test
  public void testAuthenticatedGetRendersConfirmationFormWithoutToken() throws IOException {
    UserId user = UserId.fromString("user-alice");
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.of(user)), fixedClock);

    FakeResponse resp = new FakeResponse();
    HttpServletRequest req = createFakeRequest(Map.of(), Map.of());

    servlet.doGet(req, resp.asHttpServletResponse());

    assertThat(resp.errorCode).isNull();
    assertThat(resp.redirectLocation).isNull();
    assertThat(resp.headers.get(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    assertThat(resp.headers.get(HttpHeaders.CONTENT_SECURITY_POLICY)).contains(EXPECTED_CSP);

    String body = resp.writerOutput.toString();
    assertThat(body).contains("<h2>CLI Authentication</h2>");
    assertThat(body).contains("Logged in as user-alice");
    assertThat(body).contains("<form method=\"post\" action=\"/cli-login\">");
    assertThat(body).contains("<button type=\"submit\">Generate CLI Token</button>");
    assertThat(body).doesNotContain("<textarea");
  }

  @Test
  public void testPostSecFetchSiteChecks() throws IOException {
    UserId user = UserId.fromString("user-alice");
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.of(user)), fixedClock);

    String[] blockedSites = {"cross-site", "same-site", "none", "CROSS-SITE", "invalid"};
    for (String site : blockedSites) {
      FakeResponse resp = new FakeResponse();
      HttpServletRequest req = createFakeRequest(Map.of("Sec-Fetch-Site", site), Map.of());
      servlet.doPost(req, resp.asHttpServletResponse());
      assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  @Test
  public void testPostUnauthenticatedReturnsForbidden() throws IOException {
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.absent()), fixedClock);

    FakeResponse resp = new FakeResponse();
    HttpServletRequest req = createFakeRequest(Map.of("Sec-Fetch-Site", "same-origin"), Map.of());
    servlet.doPost(req, resp.asHttpServletResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
  }

  @Test
  public void testPostAuthenticatedMintsToken() throws IOException {
    UserId user = UserId.fromString("user-bob");
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.of(user)), fixedClock);

    FakeResponse resp = new FakeResponse();
    HttpServletRequest req = createFakeRequest(Map.of("Sec-Fetch-Site", "same-origin"), Map.of());
    servlet.doPost(req, resp.asHttpServletResponse());

    assertThat(resp.errorCode).isNull();
    assertThat(resp.headers.get(HttpHeaders.CACHE_CONTROL)).contains("no-store");
    assertThat(resp.headers.get(HttpHeaders.CONTENT_SECURITY_POLICY)).contains(EXPECTED_CSP);

    String body = resp.writerOutput.toString();
    assertThat(body).contains("<h2>CLI Authentication Token</h2>");
    assertThat(body).contains("Logged in as user-bob");
    assertThat(body).contains("<textarea readonly rows=\"4\" cols=\"80\">");
    assertThat(body).contains("user-bob:");
    assertThat(body).contains("Expires:");
    assertThat(body).contains("(valid for 60 minutes)");
  }

  @Test
  public void testPostWithoutSecFetchSiteAllowedWhenAuthenticated() throws IOException {
    UserId user = UserId.fromString("user-carol");
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.of(user)), fixedClock);

    FakeResponse resp = new FakeResponse();
    HttpServletRequest req = createFakeRequest(Map.of(), Map.of()); // No Sec-Fetch-Site header
    servlet.doPost(req, resp.asHttpServletResponse());

    assertThat(resp.errorCode).isNull();
    String body = resp.writerOutput.toString();
    assertThat(body).contains("<textarea readonly rows=\"4\" cols=\"80\">");
    assertThat(body).contains("user-carol:");
  }

  @Test
  public void testHtmlEscapingUserId() throws IOException {
    UserId xssUser = UserId.fromString("user<script>alert(1)</script>&\"'");
    CliLoginServlet servlet = new CliLoginServlet(
        authHelper, stateHelper, cookieFactory, Providers.of(Optional.of(xssUser)), fixedClock);

    FakeResponse respGet = new FakeResponse();
    servlet.doGet(createFakeRequest(Map.of(), Map.of()), respGet.asHttpServletResponse());
    assertThat(respGet.writerOutput.toString()).doesNotContain("<script>");
    assertThat(respGet.writerOutput.toString()).contains("&lt;script&gt;alert(1)&lt;/script&gt;&amp;&quot;&#39;");

    FakeResponse respPost = new FakeResponse();
    servlet.doPost(createFakeRequest(Map.of("Sec-Fetch-Site", "same-origin"), Map.of()), respPost.asHttpServletResponse());
    assertThat(respPost.writerOutput.toString()).doesNotContain("<script>");
    assertThat(respPost.writerOutput.toString()).contains("&lt;script&gt;alert(1)&lt;/script&gt;&amp;&quot;&#39;");
  }

  private static class FakeResponse {
    Integer errorCode;
    String errorMessage;
    String redirectLocation;
    final Map<String, List<String>> headers = new HashMap<>();
    final CharArrayWriter writerOutput = new CharArrayWriter();

    HttpServletResponse asHttpServletResponse() {
      return (HttpServletResponse) Proxy.newProxyInstance(
          HttpServletResponse.class.getClassLoader(),
          new Class<?>[]{HttpServletResponse.class},
          (proxy, method, args) -> {
            String name = method.getName();
            if ("sendError".equals(name)) {
              errorCode = (Integer) args[0];
              if (args.length > 1) {
                errorMessage = (String) args[1];
              }
              return null;
            } else if ("sendRedirect".equals(name)) {
              redirectLocation = (String) args[0];
              return null;
            } else if ("setHeader".equals(name)) {
              List<String> list = new ArrayList<>();
              list.add((String) args[1]);
              headers.put((String) args[0], list);
              return null;
            } else if ("addHeader".equals(name)) {
              headers.computeIfAbsent((String) args[0], k -> new ArrayList<>()).add((String) args[1]);
              return null;
            } else if ("getWriter".equals(name)) {
              return new PrintWriter(writerOutput);
            } else if ("setContentType".equals(name)) {
              return null;
            }
            return null;
          });
    }
  }

  private static HttpServletRequest createFakeRequest(Map<String, String> headers, Map<String, String> params) {
    return (HttpServletRequest) Proxy.newProxyInstance(
        HttpServletRequest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class},
        (proxy, method, args) -> {
          String name = method.getName();
          if ("getHeader".equals(name)) {
            return headers.get(args[0]);
          } else if ("getParameter".equals(name)) {
            return params.get(args[0]);
          } else if ("getCookies".equals(name)) {
            return null;
          }
          return null;
        });
  }
}
