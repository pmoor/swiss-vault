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

import com.google.appengine.api.datastore.Transaction;
import com.google.inject.Key;
import ws.moor.rest.RestFilter;
import ws.moor.rest.annotations.PostFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TransactionFilter implements RestFilter {

  @PostFilter
  public void post(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    Transaction transaction = (Transaction) request.getAttribute(Key.get(Transaction.class).toString());
    if (transaction != null && transaction.isActive()) {
      transaction.rollback();
    }
  }
}
