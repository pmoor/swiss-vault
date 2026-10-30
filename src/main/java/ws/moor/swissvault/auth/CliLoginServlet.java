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
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Singleton
class CliLoginServlet extends HttpServlet {

  public static final String PATH = "/cli-login";

  private static final DateTimeFormatter EXPIRATION_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  private final AuthHelper authHelper;
  private final AuthCookieFactory authCookieFactory;
  private final Provider<Optional<UserId>> userIdProvider;
  private final Clock clock;

  @Inject
  CliLoginServlet(AuthHelper authHelper,
                  AuthCookieFactory authCookieFactory,
                  @AuthenticatedUser Provider<Optional<UserId>> userIdProvider,
                  Clock clock) {
    this.authHelper = authHelper;
    this.authCookieFactory = authCookieFactory;
    this.userIdProvider = userIdProvider;
    this.clock = clock;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Optional<UserId> userId = userIdProvider.get();

    if (!userId.isPresent()) {
      resp.sendRedirect(authHelper.createRedirectUri(OAuthState.CLI_LOGIN).toString());
      return;
    }

    resp.setContentType("text/html; charset=UTF-8");
    String cookieHeader = authCookieFactory.createCookieString(userId.get());
    String authValue = cookieHeader.substring("auth=".length(), cookieHeader.indexOf(';'));

    Instant now = clock.instant();
    Instant expiresAt = now.plus(authCookieFactory.getCookieExpiration());
    String expiresAtFormatted = EXPIRATION_FORMATTER.format(expiresAt);
    long expirationMinutes = authCookieFactory.getCookieExpiration().toMinutes();

    PrintWriter out = resp.getWriter();
    out.println("<!doctype html>");
    out.println("<html><head><meta charset=\"utf-8\"><title>Swiss Vault - CLI Login</title>");
    out.println("<style>");
    out.println("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f5f5f5; margin: 0; padding: 40px 20px; }");
    out.println(".container { max-width: 520px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }");
    out.println("h2 { margin-top: 0; color: #333; }");
    out.println("p { color: #555; line-height: 1.5; }");
    out.println(".user { background: #e8f0fe; color: #1a73e8; padding: 4px 8px; border-radius: 4px; font-weight: bold; }");
    out.println(".meta { font-size: 13px; color: #5f6368; margin-top: 16px; margin-bottom: 4px; }");
    out.println(".expiration { font-weight: 600; color: #d93025; }");
    out.println(".token-box { width: 100%; height: 70px; font-family: monospace; font-size: 13px; margin-top: 8px; box-sizing: border-box; padding: 10px; background: #f8f9fa; border: 1px solid #dadce0; border-radius: 4px; word-break: break-all; }");
    out.println("</style>");
    out.println("</head><body>");
    out.println("<div class=\"container\">");
    out.println("<h2>CLI Authentication Token</h2>");
    out.println("<p>Logged in as <span class=\"user\">" + escapeHtml(userId.get().asString()) + "</span></p>");
    out.println("<p>Copy and paste this token into your terminal:</p>");
    out.println("<textarea readonly class=\"token-box\" onclick=\"this.select()\">" + escapeHtml(authValue) + "</textarea>");
    out.println("<p class=\"meta\"><strong>Expires:</strong> <span class=\"expiration\">" + escapeHtml(expiresAtFormatted) + "</span> (valid for " + expirationMinutes + " minutes)</p>");
    out.println("<p class=\"meta\">Note: This is a short-lived session token. You will need to log in again once it expires.</p>");
    out.println("</div></body></html>");
  }

  private static String escapeHtml(String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
