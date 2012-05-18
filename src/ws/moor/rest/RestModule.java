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
package ws.moor.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import ws.moor.rest.annotations.RequestBody;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

public class RestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RestCallInvocator.class);
  }

  @RequestScoped
  @Provides
  @RequestBody
  String provideRequestBody(HttpServletRequest request) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ByteStreams.copy(request.getInputStream(), baos);
    return new String(baos.toByteArray(), Charset.defaultCharset());
  }
  
  @RequestScoped
  @Provides
  @RequestBody
  JsonArray provideRequestArray(JsonParser jsonParser, HttpServletRequest request) throws IOException {
    return jsonParser.parse(new InputStreamReader(request.getInputStream())).getAsJsonArray();
  }

  @RequestScoped
  @Provides
  @RequestBody
  JsonObject provideRequestObject(JsonParser jsonParser, HttpServletRequest request) throws IOException {
    return jsonParser.parse(new InputStreamReader(request.getInputStream())).getAsJsonObject();
  }
  
  @RequestScoped
  @Provides
  RestCallContext provideRestCallContext(HttpServletRequest request) {
    return (RestCallContext) request.getAttribute(Key.get(RestCallContext.class).toString());
  }
  
  @RequestScoped
  @Provides
  Class<? extends RestAction> provideActionClass(RestCallContext context) {
    return context.actionClass;
  }

  @RequestScoped
  @Provides
  ImmutableMap<String, String> provideParameters(RestCallContext context) {
    return context.parameters;
  }

  @RequestScoped
  @Provides
  RestAction provideAction(Injector injector, RestCallContext context) {
    return injector.getInstance(context.actionClass);
  }

  @RequestScoped
  @Provides
  List<Class<? extends RestFilter>> provideFilterClasses(RestCallContext context) {
    return context.filterClasses;
  }
}
