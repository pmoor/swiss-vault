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
package ws.moor.swissvault.persistence.model;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.JsonObject;
import ws.moor.swissvault.domain.EncryptedValue;
import ws.moor.swissvault.util.Obfuscator;

import java.util.Date;

public class DbSecret {

  public final Entity entity;

  private final Property<Date> creationDate = Property.date("creation-date");
  private final Property<EncryptedValue> name = Property.encryptedValue("name");
  private final Property<EncryptedValue> description = Property.encryptedValue("description");

  public DbSecret(Entity entity) {
    this.entity = entity;
  }

  public static DbSecret newDbSecret(
      long keyId, EncryptedValue name, EncryptedValue description) {
    DbSecret dbSecret = new DbSecret(new Entity("Secret", KeyFactory.createKey("Key", keyId)));
    dbSecret.creationDate.set(dbSecret.entity, new Date());
    dbSecret.name.set(dbSecret.entity, name);
    dbSecret.description.set(dbSecret.entity, description);
    return dbSecret;
  }

//  public Reference getLocation(Obfuscator obfuscator) {
//    return new Reference(
//        URI.create(String.format("http://localhost:8080/api/keys/%s/secrets/%s",
//            obfuscator.obfuscateLong(entity.getKey().getParent().getId()),
//            obfuscator.obfuscateLong(entity.getKey().getId()))));
//  }

  public JsonObject toJsonObject(Obfuscator obfuscator) {
    JsonObject object = new JsonObject();
    object.addProperty("id", obfuscator.obfuscateLong(entity.getKey().getId()));
    object.addProperty("creation-date", creationDate.get(entity).toString());
    object.add("name", name.get(entity).toJsonObject());
    object.add("description", description.get(entity).toJsonObject());
    return object;
  }

  public void setName(EncryptedValue name) {
    this.name.set(entity, name);
  }

  public void setDescription(EncryptedValue description) {
    this.description.set(entity, description);
  }
}
