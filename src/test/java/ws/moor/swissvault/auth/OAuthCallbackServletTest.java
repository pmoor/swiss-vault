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

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonParser;
import org.junit.Before;
import org.junit.Test;
import ws.moor.swissvault.util.UriBuilder;
import ws.moor.testing.FakeHttpServletRequest;
import ws.moor.testing.FakeHttpServletResponse;

import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.truth.Truth.assertThat;

public class OAuthCallbackServletTest {

  private static final SecretKeySpec HMAC_KEY = new SecretKeySpec(new byte[32], "HmacSHA256");

  private UriBuilder uriBuilder;
  private OAuthStateHelper stateHelper;
  private AuthCookieFactory cookieFactory;
  private FakeAuthHelper authHelper;
  private OAuthCallbackServlet servlet;

  @Before
  public void setUp() {
    uriBuilder = new UriBuilder("http", "localhost:8080");
    stateHelper = new OAuthStateHelper(false);
    cookieFactory = new AuthCookieFactory(false, HMAC_KEY, Clock.systemUTC());
    authHelper = new FakeAuthHelper(uriBuilder);
    servlet = new OAuthCallbackServlet(authHelper, stateHelper, cookieFactory, uriBuilder);
  }

  @Test
  public void testMissingStateParameterReturns400() throws Exception {
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "valid_code")
        .withCookies(new Cookie("oauth_state", "HOME:1234567890abcdef1234567890abcdef"));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(resp.redirectLocation).isNull();
    assertThat(authHelper.determineUserIdCalled.get()).isFalse();
    assertThat(resp.headers.get(HttpHeaders.SET_COOKIE)).contains("oauth_state=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
  }

  @Test
  public void testMissingCookieReturns400() throws Exception {
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "valid_code")
        .withParameter("state", "HOME:1234567890abcdef1234567890abcdef");

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(resp.redirectLocation).isNull();
    assertThat(authHelper.determineUserIdCalled.get()).isFalse();
  }

  @Test
  public void testMismatchedStateReturns400() throws Exception {
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "valid_code")
        .withParameter("state", "HOME:1234567890abcdef1234567890abcdef")
        .withCookies(new Cookie("oauth_state", "HOME:othernonce1234567890abcdef"));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(resp.redirectLocation).isNull();
    assertThat(authHelper.determineUserIdCalled.get()).isFalse();
  }

  @Test
  public void testLegacyStaticStateReturns400() throws Exception {
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "attacker_code")
        .withParameter("state", "HOME")
        .withCookies(new Cookie("oauth_state", "HOME"));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(resp.redirectLocation).isNull();
    assertThat(authHelper.determineUserIdCalled.get()).isFalse();
  }

  @Test
  public void testMissingCodeReturns400() throws Exception {
    String state = stateHelper.createStateValue(OAuthState.HOME);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("state", state)
        .withCookies(new Cookie("oauth_state", state));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
    assertThat(resp.redirectLocation).isNull();
    assertThat(authHelper.determineUserIdCalled.get()).isFalse();
  }

  @Test
  public void testSuccessfulCallbackForHome() throws Exception {
    String state = stateHelper.createStateValue(OAuthState.HOME);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "valid_code")
        .withParameter("state", state)
        .withCookies(new Cookie("oauth_state", state));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isNull();
    assertThat(resp.redirectLocation).isEqualTo("http://localhost:8080/");
    assertThat(authHelper.determineUserIdCalled.get()).isTrue();

    List<String> setCookies = resp.headers.get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).hasSize(2);
    assertThat(setCookies.get(0)).isEqualTo("oauth_state=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
    assertThat(setCookies.get(1)).startsWith("auth=user-123:");
  }

  @Test
  public void testFailedDetermineUserIdReturns502BadGateway() throws Exception {
    String state = stateHelper.createStateValue(OAuthState.HOME);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "invalid_code")
        .withParameter("state", state)
        .withCookies(new Cookie("oauth_state", state));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isEqualTo(HttpServletResponse.SC_BAD_GATEWAY);
    assertThat(resp.errorMessage).isEqualTo("Failed to verify authorization code");
    assertThat(resp.redirectLocation).isNull();
    assertThat(authHelper.determineUserIdCalled.get()).isTrue();
  }

  @Test
  public void testSuccessfulCallbackSecureCookies() throws Exception {
    OAuthStateHelper secureStateHelper = new OAuthStateHelper(true);
    AuthCookieFactory secureCookieFactory = new AuthCookieFactory(true, HMAC_KEY, Clock.systemUTC());
    OAuthCallbackServlet secureServlet = new OAuthCallbackServlet(
        authHelper, secureStateHelper, secureCookieFactory, uriBuilder);

    String state = secureStateHelper.createStateValue(OAuthState.HOME);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "valid_code")
        .withParameter("state", state)
        .withCookies(new Cookie("__Host-oauth_state", state));

    secureServlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isNull();
    assertThat(resp.redirectLocation).isEqualTo("http://localhost:8080/");

    List<String> setCookies = resp.headers.get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).hasSize(2);
    assertThat(setCookies.get(0)).isEqualTo("__Host-oauth_state=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax; Secure");
    assertThat(setCookies.get(1)).startsWith("__Host-auth=user-123:");
    assertThat(setCookies.get(1)).endsWith("; Secure");
  }

  @Test
  public void testSuccessfulCallbackForCliLogin() throws Exception {
    String state = stateHelper.createStateValue(OAuthState.CLI_LOGIN);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest()
        .withParameter("code", "valid_code")
        .withParameter("state", state)
        .withCookies(new Cookie("oauth_state", state));

    servlet.doGet(req.asRequest(), resp.asResponse());

    assertThat(resp.errorCode).isNull();
    assertThat(resp.redirectLocation).isEqualTo("http://localhost:8080/cli-login");
    assertThat(authHelper.determineUserIdCalled.get()).isTrue();

    List<String> setCookies = resp.headers.get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).hasSize(2);
    assertThat(setCookies.get(0)).isEqualTo("oauth_state=; Max-Age=0; Path=/; HttpOnly; SameSite=Lax");
    assertThat(setCookies.get(1)).startsWith("auth=user-123:");
  }

  private static class FakeAuthHelper extends AuthHelper {
    final AtomicBoolean determineUserIdCalled = new AtomicBoolean(false);

    FakeAuthHelper(UriBuilder uriBuilder) {
      super(HttpClient.newHttpClient(), new JsonParser(), uriBuilder, "secret", "client-id");
    }

    @Override
    public UserId determineUserId(String code) throws IOException {
      determineUserIdCalled.set(true);
      if ("valid_code".equals(code)) {
        return UserId.fromString("user-123");
      }
      throw new IOException("invalid code");
    }
  }
}
