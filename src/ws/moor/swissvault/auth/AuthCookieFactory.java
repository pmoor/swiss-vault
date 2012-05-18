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

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import ws.moor.swissvault.config.Config;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.security.GeneralSecurityException;
import java.util.Arrays;

class AuthCookieFactory {
  
  private static final String COOKIE_NAME = "auth";

  private final boolean secureOnly;
  private final SecretKeySpec aesKey;
  private final SecretKeySpec hmacKey;
  private final IvParameterSpec ivSpec;

  @Inject
  AuthCookieFactory(@Config("secure_only") boolean secureOnly,
                    @Config("auth.hmac_key") SecretKeySpec hmacKey,
                    @Config("auth.aes_key") SecretKeySpec aesKey,
                    @Config("auth.iv") IvParameterSpec iv) {
    this.secureOnly = secureOnly;
    this.aesKey = aesKey;
    this.hmacKey = hmacKey;
    this.ivSpec = iv;
  }

  public UserId extractUserId(HttpServletRequest request) {
    Cookie cookie = findCookie(request);
    if (cookie == null) {
      return null;
    }

    byte[] encrypted = DatatypeConverter.parseBase64Binary(cookie.getValue());

    String decrypted = decrypt(encrypted);

    String[] parts = decrypted.split(":");
    UserId userId = UserId.fromString(parts[0]);
    long validUntilMs = Long.parseLong(parts[1]);
    byte[] actualSignature = DatatypeConverter.parseBase64Binary(parts[2]);

    byte[] expectedSignature = sign(userId, validUntilMs);
    
    if (Arrays.equals(expectedSignature, actualSignature)) {
      return userId;
    } else {
      return null;
    }
  }

  public Cookie createCookie(UserId userId) {
    long validUntilMs = System.currentTimeMillis() + 3600 * 1000; // 1h
    byte[] signature = sign(userId, validUntilMs);
    String plainValue = String.format("%s:%d:%s", userId.asString(), validUntilMs, DatatypeConverter.printBase64Binary(signature));
    String encryptedValue = DatatypeConverter.printBase64Binary(encrypt(plainValue));
    Cookie cookie = new Cookie(COOKIE_NAME, encryptedValue);
    cookie.setMaxAge(3600);
    cookie.setSecure(secureOnly);
    return cookie;
  }

  private byte[] encrypt(String decrypted) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);
      return cipher.doFinal(decrypted.getBytes(Charsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] sign(UserId userId, long validUntilMs) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(hmacKey);
      return mac.doFinal(String.format("%s:%d", userId.asString(), validUntilMs).getBytes(Charsets.UTF_8));
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private String decrypt(byte[] encrypted) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
      return new String(cipher.doFinal(encrypted), Charsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private Cookie findCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(COOKIE_NAME)) {
          return cookie;
        }
      }
    }
    return null;
  }
}
