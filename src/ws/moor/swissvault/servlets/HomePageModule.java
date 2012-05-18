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

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HomePageModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(HomePageServlet.class);
    try {
      Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(
          "com.google.inject.servlet.InternalServletModule$BackwardsCompatibleServletContextProvider");
      bind(clazz);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    install(new ServletModule() {
      @Override
      protected void configureServlets() {
        serve("/").with(HomePageServlet.class);
        serve("/favicon.ico").with(new HttpServlet() {
          @Override
          protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no favicon yet");
          }
        });
      }
    });
  }
}
