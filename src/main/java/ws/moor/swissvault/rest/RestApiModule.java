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
package ws.moor.swissvault.rest;

import com.google.inject.AbstractModule;
import ws.moor.rest.RestModule;
import ws.moor.rest.RestServletModule;
import ws.moor.swissvault.rest.filters.AuthenticationFilter;
import ws.moor.swissvault.rest.filters.TransactionFilter;

public class RestApiModule extends AbstractModule {
  @Override
  protected void configure() {
    install(new RestModule());
    install(new RestServletModule("/api/*") {
      @Override protected void configureActions() {
        addAction(ListKeys.class);
        addAction(CreateKey.class);
        addAction(GetKey.class);
        addAction(UpdateKey.class);
        addAction(DeleteKey.class);

        addAction(ListSecrets.class);
        addAction(CreateSecret.class);
        addAction(UpdateSecret.class);
        addAction(DeleteSecret.class);

        addAction(ListProperties.class);
        addAction(CreateProperty.class);
        addAction(UpdateProperty.class);
        addAction(DeleteProperty.class);

        addFilter(AuthenticationFilter.class);
        addFilter(TransactionFilter.class);
      }
    });
  }
}
