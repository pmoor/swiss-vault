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
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import org.joda.time.Duration;
import org.joda.time.Instant;
import ws.moor.common.Clock;
import ws.moor.swissvault.config.Config;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.security.GeneralSecurityException;
import java.util.Arrays;

class AuthCookieFactory {
  
  private static final String COOKIE_NAME = "auth";
  private static final Duration COOKIE_EXPIRATION = Duration.standardHours(1);

  private final boolean secureOnly;
  private final SecretKeySpec hmacKey;
  private final Clock clock;

  @Inject
  AuthCookieFactory(@Config("secure_only") boolean secureOnly,
                    @Config("auth.hmac_key") SecretKeySpec hmacKey,
                    Clock clock) {
    this.secureOnly = secureOnly;
    this.hmacKey = hmacKey;
    this.clock = clock;
  }

  public UserId extractUserId(HttpServletRequest request) {
    Cookie cookie = findCookie(request);
    if (cookie == null) {
      return null;
    }

    String[] parts = cookie.getValue().split(":");
    UserId userId = UserId.fromString(parts[0]);
    Instant creationTime = new Instant(Long.parseLong(parts[1]));
    byte[] actualSignature = BaseEncoding.base64().decode(parts[2]);

    byte[] expectedSignature = sign(userId, creationTime);
    if (Arrays.equals(expectedSignature, actualSignature)) {
      if (creationTime.plus(COOKIE_EXPIRATION).isAfter(clock.now())) {
        return userId;
      }
    }
    return null;
  }

  public Cookie createCookie(UserId userId) {
    Instant creationTime = clock.now();
    byte[] signature = sign(userId, creationTime);
    String value = String.format("%s:%d:%s",
        userId.asString(), creationTime.getMillis(), BaseEncoding.base64().encode(signature));

    Cookie cookie = new Cookie(COOKIE_NAME, value);
    cookie.setPath("/");
    cookie.setMaxAge((int) COOKIE_EXPIRATION.getStandardSeconds());
    cookie.setSecure(secureOnly);
    return cookie;
  }

  private byte[] sign(UserId userId, Instant creationTime) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(hmacKey);
      return mac.doFinal(String.format("%s:%d", userId.asString(), creationTime.getMillis()).getBytes(Charsets.UTF_8));
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
