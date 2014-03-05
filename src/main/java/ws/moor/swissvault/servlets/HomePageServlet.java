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
package ws.moor.swissvault.servlets;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import ws.moor.swissvault.auth.AuthHelper;
import ws.moor.swissvault.auth.AuthenticatedUser;
import ws.moor.swissvault.auth.UserId;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

@Singleton
public class HomePageServlet extends HttpServlet {

  private static final URL mainPageResourceUrl = Resources.getResource(HomePageServlet.class, "/templates/main.html");

  private final AuthHelper authHelper;
  private final Provider<Optional<UserId>> userIdProvider;

  @Inject
  HomePageServlet(AuthHelper authHelper, @AuthenticatedUser Provider<Optional<UserId>> userIdProvider) {
    this.userIdProvider = userIdProvider;
    this.authHelper = authHelper;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if (userIdProvider.get().isPresent()) {
      resp.setContentType("text/html");
      resp.setCharacterEncoding("utf-8");
      Resources.copy(mainPageResourceUrl, resp.getOutputStream());
    } else {
      resp.sendRedirect(authHelper.createRedirectUri().toString());
    }
  }
}
