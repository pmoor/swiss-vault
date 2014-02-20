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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Singleton
class OAuthCallbackServlet extends HttpServlet {

  public static final String PATH = "/oauth2callback";

  private final AuthHelper authHelper;
  private final AuthCookieFactory authCookieFactory;

  @Inject
  OAuthCallbackServlet(AuthHelper authHelper, AuthCookieFactory authCookieFactory) {
    this.authHelper = authHelper;
    this.authCookieFactory = authCookieFactory;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String code = req.getParameter("code");
    UserId userId = authHelper.determineUserId(code);
    Cookie cookie = authCookieFactory.createCookie(userId);

    resp.addCookie(cookie);
    resp.sendRedirect(req.getParameter("state"));
  }
}
