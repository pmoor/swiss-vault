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
import com.google.inject.Inject;
import ws.moor.rest.RestAction;
import ws.moor.rest.annotations.Delete;
import ws.moor.rest.annotations.Execute;
import ws.moor.swissvault.persistence.KeyRepository;
import ws.moor.swissvault.rest.filters.AccessType;
import ws.moor.swissvault.rest.filters.AuthorizationRequired;

@AuthorizationRequired(type = AccessType.KEY_ACL_PRESENT)
@Delete(path = "/keys/{keyId}")
public class DeleteKey implements RestAction {

  private final KeyRepository keyRepository;
  private final ParameterExtractor parameterExtractor;
  private final Transaction transaction;

  @Inject
  DeleteKey(KeyRepository keyRepository, ParameterExtractor parameterExtractor, Transaction transaction) {
    this.keyRepository = keyRepository;
    this.parameterExtractor = parameterExtractor;
    this.transaction = transaction;
  }

  @Execute
  public void remove() {
    long keyId = parameterExtractor.getKeyId();
    keyRepository.delete(transaction, keyId);
    transaction.commit();
  }
}
