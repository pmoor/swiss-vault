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
package ws.moor.swissvault.util;

import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import ws.moor.swissvault.config.Config;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

@Singleton
public class Obfuscator {

  private final SecretKeySpec secret;

  @Inject
  Obfuscator(@Config("obfuscator_secret") SecretKeySpec secret) {
    this.secret = secret;
  }

  public String obfuscateLong(long unobfuscated) {
    try {
      Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, secret);
      byte[] encrypted = cipher.doFinal(Longs.toByteArray(unobfuscated));
      return new BigInteger(1, encrypted).toString(16);
    } catch (Exception e) {
      throw new RuntimeException(String.valueOf(unobfuscated), e);
    }
  }

  public long unobfuscateLong(String obfuscated) {
    try {
      Cipher cipher = Cipher.getInstance("Blowfish/ECB/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, secret);
      long value = new BigInteger(obfuscated, 16).longValue();
      return Longs.fromByteArray(cipher.doFinal(Longs.toByteArray(value)));
    } catch (Exception e) {
      throw new RuntimeException(obfuscated, e);
    }
  }
}
