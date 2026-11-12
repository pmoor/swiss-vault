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
import org.junit.Before;
import org.junit.Test;
import ws.moor.testing.FakeHttpServletRequest;
import ws.moor.testing.FakeHttpServletResponse;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.time.Clock;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class LogoutServletTest {

  private static final SecretKeySpec HMAC_KEY = new SecretKeySpec(new byte[32], "HmacSHA256");

  private AuthCookieFactory insecureFactory;
  private AuthCookieFactory secureFactory;

  @Before
  public void setUp() {
    insecureFactory = new AuthCookieFactory(false, HMAC_KEY, Clock.systemUTC());
    secureFactory = new AuthCookieFactory(true, HMAC_KEY, Clock.systemUTC());
  }

  @Test
  public void testLogoutClearsCookieAndRendersLoggedOutPage() throws IOException {
    LogoutServlet servlet = new LogoutServlet(insecureFactory);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest();

    servlet.doGet(req.asRequest(), resp.asResponse());

    List<String> setCookies = resp.headers.get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).containsExactly("auth=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
    assertThat(resp.contentType).isEqualTo("text/html; charset=UTF-8");

    String html = resp.getBody();
    assertThat(html).contains("<title>Swiss Vault - Logged Out</title>");
    assertThat(html).contains("<h2>Logged Out</h2>");
    assertThat(html).contains("<p>You have been logged out of Swiss Vault.</p>");
    assertThat(html).contains("<p><a href=\"/\">Log in again</a></p>");
  }

  @Test
  public void testLogoutWithSecureOnlySetsSecureFlag() throws IOException {
    LogoutServlet servlet = new LogoutServlet(secureFactory);
    FakeHttpServletResponse resp = new FakeHttpServletResponse();
    FakeHttpServletRequest req = new FakeHttpServletRequest();

    servlet.doGet(req.asRequest(), resp.asResponse());

    List<String> setCookies = resp.headers.get(HttpHeaders.SET_COOKIE);
    assertThat(setCookies).containsExactly("__Host-auth=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax; Secure");
    assertThat(resp.contentType).isEqualTo("text/html; charset=UTF-8");

    String html = resp.getBody();
    assertThat(html).contains("<a href=\"/\">Log in again</a>");
  }
}
