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

import com.google.appengine.api.datastore.Transaction;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import ws.moor.rest.RestAction;
import ws.moor.rest.annotations.Execute;
import ws.moor.rest.annotations.Post;
import ws.moor.rest.annotations.RequestBody;
import ws.moor.swissvault.persistence.SecretRepository;
import ws.moor.swissvault.persistence.model.DbSecret;
import ws.moor.swissvault.rest.filters.AccessType;
import ws.moor.swissvault.rest.filters.AuthorizationRequired;
import ws.moor.swissvault.util.Obfuscator;

@AuthorizationRequired(type = AccessType.KEY_ACL_PRESENT)
@Post(path = "/keys/{keyId}/secrets")
public class CreateSecret implements RestAction {

  private final Obfuscator obfuscator;
  private final SecretRepository secretRepository;
  private final ParameterExtractor parameterExtractor;
  private final Transaction transaction;

  @Inject
  CreateSecret(
      Obfuscator obfuscator, SecretRepository secretRepository, ParameterExtractor parameterExtractor,
      Transaction transaction) {
    this.obfuscator = obfuscator;
    this.parameterExtractor = parameterExtractor;
    this.secretRepository = secretRepository;
    this.transaction = transaction;
  }

  @Execute
  public JsonObject create(@RequestBody JsonObject json) {
    long keyId = parameterExtractor.getKeyId();

    DbSecret dbSecret = secretRepository.newSecret(transaction, keyId, json);
    transaction.commit();

    return dbSecret.toJsonObject(obfuscator);
  }
}
