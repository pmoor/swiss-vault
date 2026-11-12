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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Singleton
class CliLoginServlet extends HttpServlet {

  public static final String PATH = "/cli-login";

  private static final String CONTENT_SECURITY_POLICY_VALUE =
      "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'self'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'";

  private static final DateTimeFormatter EXPIRATION_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  private final AuthHelper authHelper;
  private final OAuthStateHelper oAuthStateHelper;
  private final AuthCookieFactory authCookieFactory;
  private final Provider<Optional<UserId>> userIdProvider;
  private final Clock clock;

  @Inject
  CliLoginServlet(AuthHelper authHelper,
                  OAuthStateHelper oAuthStateHelper,
                  AuthCookieFactory authCookieFactory,
                  @AuthenticatedUser Provider<Optional<UserId>> userIdProvider,
                  Clock clock) {
    this.authHelper = authHelper;
    this.oAuthStateHelper = oAuthStateHelper;
    this.authCookieFactory = authCookieFactory;
    this.userIdProvider = userIdProvider;
    this.clock = clock;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    resp.setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, CONTENT_SECURITY_POLICY_VALUE);

    Optional<UserId> userId = userIdProvider.get();

    if (!userId.isPresent()) {
      String state = oAuthStateHelper.createStateValue(OAuthState.CLI_LOGIN);
      resp.addHeader(HttpHeaders.SET_COOKIE, oAuthStateHelper.createCookieHeader(state));
      resp.sendRedirect(authHelper.createRedirectUri(state).toString());
      return;
    }

    resp.setContentType("text/html; charset=UTF-8");
    PrintWriter out = resp.getWriter();
    out.println("<!doctype html>");
    out.println("<html><head><meta charset=\"utf-8\"><title>Swiss Vault - CLI Login</title></head>");
    out.println("<body>");
    out.println("<h2>CLI Authentication</h2>");
    out.println("<p>Logged in as " + escapeHtml(userId.get().asString()) + "</p>");
    out.println("<form method=\"post\" action=\"/cli-login\"><button type=\"submit\">Generate CLI Token</button></form>");
    out.println("</body></html>");
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    resp.setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, CONTENT_SECURITY_POLICY_VALUE);

    String secFetchSite = req.getHeader("Sec-Fetch-Site");
    if (secFetchSite != null && !secFetchSite.trim().equalsIgnoreCase("same-origin")) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
      return;
    }

    Optional<UserId> userId = userIdProvider.get();
    if (!userId.isPresent()) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
      return;
    }

    resp.setContentType("text/html; charset=UTF-8");
    String authValue = authCookieFactory.createTokenValue(userId.get());

    Instant now = clock.instant();
    Instant expiresAt = now.plus(authCookieFactory.getCookieExpiration());
    String expiresAtFormatted = EXPIRATION_FORMATTER.format(expiresAt);
    long expirationMinutes = authCookieFactory.getCookieExpiration().toMinutes();

    PrintWriter out = resp.getWriter();
    out.println("<!doctype html>");
    out.println("<html><head><meta charset=\"utf-8\"><title>Swiss Vault - CLI Login</title></head>");
    out.println("<body>");
    out.println("<h2>CLI Authentication Token</h2>");
    out.println("<p>Logged in as " + escapeHtml(userId.get().asString()) + "</p>");
    out.println("<p>Copy and paste this token into your terminal:</p>");
    out.println("<textarea readonly rows=\"4\" cols=\"80\">" + escapeHtml(authValue) + "</textarea>");
    out.println("<p><strong>Expires:</strong> " + escapeHtml(expiresAtFormatted) + " (valid for " + expirationMinutes + " minutes)</p>");
    out.println("<p>Note: This is a short-lived session token. You will need to log in again once it expires.</p>");
    out.println("</body></html>");
  }

  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
