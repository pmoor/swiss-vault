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
import com.google.appengine.api.datastore.Text;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import ws.moor.swissvault.auth.UserId;
import ws.moor.swissvault.domain.EncryptedValue;

import java.util.Date;
import java.util.List;

public class Property<T> {

  private final String name;
  private final Function<T, Object> serialize;
  private final Function<Object, T> deserialize;

  @SuppressWarnings("unchecked")
  private Property(String name) {
    this(name, (Function<T, Object>) Functions.identity(), (Function<Object, T>) Functions.identity());
  }

  private Property(String name, Function<T, Object> serialize, Function<Object, T> deserialize) {
    this.name = name;
    this.serialize = serialize;
    this.deserialize = deserialize;
  }

  public T get(Entity entity) {
    return deserialize.apply(entity.getProperty(name));
  }

  public void set(Entity entity, T value) {
    entity.setProperty(name, serialize.apply(value));
  }

  public String getName() {
    return name;
  }

  public static Property<String> string(String name) {
    return new Property<String>(name);
  }

  public static Property<Date> date(String name) {
    return new Property<Date>(name);
  }

  public static Property<UserId> userId(String name) {
    return new Property<UserId>(name,
        new Function<UserId, Object>() {
          public String apply(UserId input) {
            return input.asString();
          }
        },
        new Function<Object, UserId>() {
          public UserId apply(Object input) {
            return UserId.fromString((String) input);
          }
        });
  }

  public static Property<EncryptedValue> encryptedValue(String name) {
    return new Property<EncryptedValue>(name,
        new Function<EncryptedValue, Object>() {
          public Text apply(EncryptedValue input) {
            return input.toText();
          }
        },
        new Function<Object, EncryptedValue>() {
          public EncryptedValue apply(Object input) {
            return EncryptedValue.fromText((Text) input);
          }
        });
  }

  public static Property<List<EncryptedValue>> encryptedValues(String name) {
    return new Property<List<EncryptedValue>>(name,
        new Function<List<EncryptedValue>, Object>() {
          public List<Text> apply(List<EncryptedValue> input) {
            return EncryptedValue.toTextList(input);
          }
        },
        new Function<Object, List<EncryptedValue>>() {
          public List<EncryptedValue> apply(Object input) {
            return EncryptedValue.fromTextList((List<Text>) input);
          }
        });
  }
}
