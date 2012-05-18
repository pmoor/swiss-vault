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
package ws.moor.swissvault.rest.filters;

import com.google.appengine.api.datastore.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.servlet.RequestScoped;
import ws.moor.swissvault.auth.AuthenticatedUser;
import ws.moor.swissvault.auth.UserId;
import ws.moor.swissvault.domain.PermissionDeniedException;
import ws.moor.swissvault.persistence.model.DbKeyAcl;
import ws.moor.swissvault.rest.ParameterExtractor;

@RequestScoped
public class AclService {

  private final DatastoreService datastoreService;
  private final Optional<UserId> userIdOptional;
  private final Provider<Transaction> transactionProvider;
  private final ParameterExtractor parameterExtractor;

  @Inject
  AclService(
      DatastoreService datastoreService,
      @AuthenticatedUser Optional<UserId> userIdOptional,
      Provider<Transaction> transactionProvider,
      ParameterExtractor parameterExtractor) {
    this.datastoreService = datastoreService;
    this.userIdOptional = userIdOptional;
    this.transactionProvider = transactionProvider;
    this.parameterExtractor = parameterExtractor;
  }
  
  void assertHasAclForKey() {
    assertHasUserId();

    loadAcl(parameterExtractor.getKeyId());
  }

  public void assertHasUserId() {
    if (!userIdOptional.isPresent()) {
      throw new PermissionDeniedException("no user id present");
    }
  }

  private DbKeyAcl loadAcl(long keyId) throws PermissionDeniedException {
    try {
      Key aclKey = KeyFactory.createKey(
          KeyFactory.createKey("Key", keyId),
          "KeyAcl", userIdOptional.get().asString());
      return new DbKeyAcl(datastoreService.get(transactionProvider.get(), aclKey));
    } catch (EntityNotFoundException e) {
      throw new PermissionDeniedException("no acl found");
    }
  }
}
