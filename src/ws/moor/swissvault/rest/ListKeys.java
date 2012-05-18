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

import com.google.gson.JsonArray;
import com.google.inject.Inject;
import ws.moor.rest.RestAction;
import ws.moor.rest.annotations.Execute;
import ws.moor.rest.annotations.Get;
import ws.moor.swissvault.auth.AuthenticatedUser;
import ws.moor.swissvault.auth.UserId;
import ws.moor.swissvault.persistence.KeyRepository;
import ws.moor.swissvault.persistence.model.DbKey;
import ws.moor.swissvault.rest.filters.AccessType;
import ws.moor.swissvault.rest.filters.AuthorizationRequired;
import ws.moor.swissvault.util.Obfuscator;

@AuthorizationRequired(type = AccessType.USER_ID_PRESENT)
@Get(path = "/keys")
public class ListKeys implements RestAction {

  private final KeyRepository keyRepository;
  private final UserId userId;
  private final Obfuscator obfuscator;

  @Inject
  ListKeys(KeyRepository keyRepository, Obfuscator obfuscator, @AuthenticatedUser UserId userId) {
    this.keyRepository = keyRepository;
    this.obfuscator = obfuscator;
    this.userId = userId;
  }

  @Execute
  JsonArray list() {
    JsonArray response = new JsonArray();
    for (DbKey key : keyRepository.getKeysWithUserAcl(userId)) {
      response.add(key.toJsonObject(obfuscator, userId));
    }
    return response;
  }
}
