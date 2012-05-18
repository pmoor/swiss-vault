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
package ws.moor.swissvault.domain;


import com.google.appengine.api.datastore.Text;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.xml.bind.DatatypeConverter;
import java.util.List;

public class EncryptedValue {

  private final EncryptionAlgorithm algorithm;
  private final byte[] iv;
  private final byte[] ciphertext;
  private final byte[] signature;

  public EncryptedValue(EncryptionAlgorithm algorithm, byte[] iv, byte[] ciphertext, byte[] signature) {
    this.algorithm = algorithm;
    this.iv = iv;
    this.ciphertext = ciphertext;
    this.signature = signature;
  }

  public static EncryptedValue fromText(Text text) {
    String[] parts = text.getValue().split(":");
    if (parts.length != 5) {
      throw new IllegalArgumentException(text.getValue());
    }
    if (!parts[0].equals("0")) {
      throw new IllegalArgumentException(text.getValue());
    }
    EncryptionAlgorithm algorithm = EncryptionAlgorithm.valueOf(parts[1]);
    byte[] iv = DatatypeConverter.parseBase64Binary(parts[2]);
    byte[] ciphertext = DatatypeConverter.parseBase64Binary(parts[3]);
    byte[] signature = DatatypeConverter.parseBase64Binary(parts[4]);
    return new EncryptedValue(algorithm, iv, ciphertext, signature);
  }

  public Text toText() {
    StringBuilder encoded = new StringBuilder();
    encoded.append("0:");
    encoded.append(algorithm.name()).append(":");
    encoded.append(DatatypeConverter.printBase64Binary(iv)).append(":");
    encoded.append(DatatypeConverter.printBase64Binary(ciphertext)).append(":");
    encoded.append(DatatypeConverter.printBase64Binary(signature));
    return new Text(encoded.toString());
  }

  public JsonObject toJsonObject() {
    JsonObject object = new JsonObject();
    object.addProperty("version", 0);
    object.addProperty("algorithm", algorithm.name());
    object.addProperty("iv", DatatypeConverter.printBase64Binary(iv));
    object.addProperty("ciphertext", DatatypeConverter.printBase64Binary(ciphertext));
    object.addProperty("signature", DatatypeConverter.printBase64Binary(signature));
    return object;
  }

  public static EncryptedValue fromJsonObject(JsonObject json) {
    int version = json.getAsJsonPrimitive("version").getAsInt();
    if (version != 0) {
      throw new IllegalArgumentException("unknown version: " + version);
    }
    EncryptionAlgorithm algorithm = EncryptionAlgorithm.valueOf(json.getAsJsonPrimitive("algorithm").getAsString());
    byte[] iv = DatatypeConverter.parseBase64Binary(json.getAsJsonPrimitive("iv").getAsString());
    byte[] ciphertext = DatatypeConverter.parseBase64Binary(json.getAsJsonPrimitive("ciphertext").getAsString());
    byte[] signature = DatatypeConverter.parseBase64Binary(json.getAsJsonPrimitive("signature").getAsString());
    return new EncryptedValue(algorithm, iv, ciphertext, signature);
  }

  public static List<Text> toTextList(List<EncryptedValue> values) {
    List<Text> result = Lists.newArrayList();
    for (EncryptedValue value : values) {
      result.add(value.toText());
    }
    return result;
  }

  public static List<EncryptedValue> fromTextList(List<Text> texts) {
    List<EncryptedValue> result = Lists.newArrayList();
    for (Text text : texts) {
      result.add(fromText(text));
    }
    return result;
  }

  public static List<EncryptedValue> fromJsonArray(JsonArray encryptions) {
    List<EncryptedValue> list = Lists.newArrayList();
    for (int i = 0; i < encryptions.size(); i++) {
      list.add(fromJsonObject(encryptions.get(i).getAsJsonObject()));
    }
    return list;
  }
}
