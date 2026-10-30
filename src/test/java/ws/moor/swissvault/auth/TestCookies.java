package ws.moor.swissvault.auth;

import com.google.common.io.BaseEncoding;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;

/** Login cookies for the dev server, signed with auth.hmac_key from development.properties. */
public final class TestCookies {

  private static final SecretKeySpec DEVELOPMENT_HMAC_KEY = new SecretKeySpec(BaseEncoding.base16().decode(
      "000102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F"), "HmacSHA256");

  private TestCookies() {}

  /** @return the value of the "auth" cookie that logs in the given user */
  public static String authCookieValue(String userId) {
    String header = new AuthCookieFactory(false, DEVELOPMENT_HMAC_KEY, Clock.systemUTC())
        .createCookieString(UserId.fromString(userId));
    return header.substring("auth=".length(), header.indexOf(';'));
  }
}
