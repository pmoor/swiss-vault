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
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import ws.moor.rest.RestAction;
import ws.moor.rest.annotations.Execute;
import ws.moor.rest.annotations.Get;
import ws.moor.swissvault.persistence.SecretRepository;
import ws.moor.swissvault.persistence.model.DbSecretProperty;
import ws.moor.swissvault.rest.filters.AccessType;
import ws.moor.swissvault.rest.filters.AuthorizationRequired;
import ws.moor.swissvault.util.Obfuscator;

@AuthorizationRequired(type = AccessType.KEY_ACL_PRESENT)
@Get(path = "/keys/{keyId}/secrets/{secretId}/properties")
public class ListProperties implements RestAction {

  private final Obfuscator obfuscator;
  private final SecretRepository secretRepository;
  private final ParameterExtractor parameterExtractor;
  private final Transaction transaction;

  @Inject
  ListProperties(Obfuscator obfuscator, SecretRepository secretRepository, ParameterExtractor parameterExtractor,
                 Transaction transaction) {
    this.obfuscator = obfuscator;
    this.parameterExtractor = parameterExtractor;
    this.secretRepository = secretRepository;
    this.transaction = transaction;
  }

  @Execute
  public JsonArray list() {
    long keyId = parameterExtractor.getKeyId();
    long secretId = parameterExtractor.getSecretId();

    JsonArray response = new JsonArray();
    for (DbSecretProperty secretProperty : secretRepository.listSecretProperties(transaction, keyId, secretId)) {
      response.add(secretProperty.toJsonObject(obfuscator));
    }
    return response;
  }
}
