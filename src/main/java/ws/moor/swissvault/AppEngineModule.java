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
package ws.moor.swissvault;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.RequestScoped;

import javax.servlet.http.HttpServletRequest;

public class AppEngineModule extends AbstractModule {

  @Override
  protected void configure() {
    binder().requireAtInjectOnConstructors();
    binder().requireExactBindingAnnotations();
    binder().disableCircularProxies();
  }

  @Provides
  @Singleton
  private DatastoreService providePersistenceManager() {
    return DatastoreServiceFactory.getDatastoreService();
  }

  @Provides
  @RequestScoped
  private Transaction provideTransaction(DatastoreService datastore, HttpServletRequest request) {
    if (request.getAttribute(Key.get(Transaction.class).toString()) != null) {
      throw new IllegalStateException();
    }
    Transaction transaction = datastore.beginTransaction();
    request.setAttribute(Key.get(Transaction.class).toString(), transaction);
    return transaction;
  }

  @Provides
  @Singleton
  private URLFetchService provideUrlFetchService() {
    return URLFetchServiceFactory.getURLFetchService();
  }

  @Provides
  @Singleton
  private JsonParser provideJsonParser() {
    return new JsonParser();
  }
}
