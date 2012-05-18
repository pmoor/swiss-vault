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
import org.joda.time.Duration;
import org.joda.time.Instant;
import ws.moor.common.Clock;
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
  private final Clock clock;

  @Inject
  AuthCookieFactory(@Config("secure_only") boolean secureOnly,
                    @Config("auth.hmac_key") SecretKeySpec hmacKey,
                    @Config("auth.aes_key") SecretKeySpec aesKey,
                    @Config("auth.iv") IvParameterSpec iv,
                    Clock clock) {
    this.secureOnly = secureOnly;
    this.aesKey = aesKey;
    this.hmacKey = hmacKey;
    this.ivSpec = iv;
    this.clock = clock;
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
    Instant validUntil = new Instant(Long.parseLong(parts[1]));
    byte[] actualSignature = DatatypeConverter.parseBase64Binary(parts[2]);

    byte[] expectedSignature = sign(userId, validUntil);
    
    if (Arrays.equals(expectedSignature, actualSignature)) {
      if (validUntil.isAfter(clock.now())) {
        return userId;
      }
    }
    return null;
  }

  public Cookie createCookie(UserId userId) {
    Instant validUntil = clock.now().plus(Duration.standardHours(1));
    byte[] signature = sign(userId, validUntil);
    String plainValue = String.format("%s:%d:%s",
        userId.asString(), validUntil.getMillis(), DatatypeConverter.printBase64Binary(signature));
    String encryptedValue = DatatypeConverter.printBase64Binary(encrypt(plainValue));
    Cookie cookie = new Cookie(COOKIE_NAME, encryptedValue);
    cookie.setPath("/");
    cookie.setMaxAge(60 * 60);
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

  private byte[] sign(UserId userId, Instant validUntil) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(hmacKey);
      return mac.doFinal(String.format("%s:%d", userId.asString(), validUntil.getMillis()).getBytes(Charsets.UTF_8));
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
