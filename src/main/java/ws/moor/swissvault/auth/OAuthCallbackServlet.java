/*
 * Copyright 2012 Patrick Moor <patrick@moor.ws>
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
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import ws.moor.swissvault.util.UriBuilder;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
class OAuthCallbackServlet extends HttpServlet {

  public static final String PATH = "/oauth2callback";
  private static final Logger logger = Logger.getLogger(OAuthCallbackServlet.class.getName());

  private final AuthHelper authHelper;
  private final OAuthStateHelper oAuthStateHelper;
  private final AuthCookieFactory authCookieFactory;
  private final UriBuilder uriBuilder;

  @Inject
  OAuthCallbackServlet(AuthHelper authHelper,
                      OAuthStateHelper oAuthStateHelper,
                      AuthCookieFactory authCookieFactory,
                      UriBuilder uriBuilder) {
    this.authHelper = authHelper;
    this.oAuthStateHelper = oAuthStateHelper;
    this.authCookieFactory = authCookieFactory;
    this.uriBuilder = uriBuilder;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // Clear/expire the state cookie on callback to prevent replay
    resp.addHeader(HttpHeaders.SET_COOKIE, oAuthStateHelper.createClearCookieHeader());

    Optional<OAuthState> state = oAuthStateHelper.validateAndExtractState(req);
    if (!state.isPresent()) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid OAuth state");
      return;
    }

    String code = req.getParameter("code");
    if (Strings.isNullOrEmpty(code)) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing authorization code");
      return;
    }

    UserId userId;
    try {
      userId = authHelper.determineUserId(code);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to verify authorization code", e);
      resp.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Failed to verify authorization code");
      return;
    }

    String cookie = authCookieFactory.createCookieString(userId);

    resp.addHeader(HttpHeaders.SET_COOKIE, cookie);

    resp.sendRedirect(uriBuilder.forPath(state.get().getPath()).toString());
  }
}
