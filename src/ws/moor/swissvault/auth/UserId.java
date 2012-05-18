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
package ws.moor.swissvault.auth;

public class UserId {

  private final String userId;

  private UserId(String userId) {
    this.userId = userId;
  }

  public static UserId fromString(String userId) {
    return new UserId(userId);
  }

  public String asString() {
    return userId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    return userId.equals(((UserId) o).userId);
  }

  @Override
  public int hashCode() {
    return userId.hashCode();
  }
}
