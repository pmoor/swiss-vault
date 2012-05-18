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

import junit.framework.TestCase;

import javax.crypto.spec.SecretKeySpec;
import java.util.Random;

public class ObfuscatorTest extends TestCase {

  private final Random rnd = new Random();
  private Obfuscator obfuscator;

  protected void setUp() throws Exception {
    super.setUp();
    obfuscator = new Obfuscator(new SecretKeySpec(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, "Blowfish"));
  }

  public void testRandomLongs() {
    for (int i = 0; i < 100; i++) {
      long secret = rnd.nextLong();
      String obfuscated = obfuscator.obfuscateLong(secret);
      long unobfuscated = obfuscator.unobfuscateLong(obfuscated);
      if (secret != unobfuscated) {
        fail(String.format("mismatch: %d -> %s -> %d", secret, obfuscated, unobfuscated));
      }
    }
  }

  public void testSpecificLongs() {
    assertEquals("1bfed93fc7d99b9e", obfuscator.obfuscateLong(0));
    assertEquals("6949d135d37d9f63", obfuscator.obfuscateLong(1));
    assertEquals("a30969c2022555c8", obfuscator.obfuscateLong(-1));

    assertEquals("e06d5a0d3b827a69", obfuscator.obfuscateLong(Long.MAX_VALUE - 1));
    assertEquals("39727ae9766d1cc7", obfuscator.obfuscateLong(Long.MAX_VALUE));
    assertEquals("8e3a7a307f0ffa3c", obfuscator.obfuscateLong(Long.MIN_VALUE));
    assertEquals("630e8401eb2613e2", obfuscator.obfuscateLong(Long.MIN_VALUE + 1));

    // short encryptions
    assertEquals("12cc47e690bc3c9", obfuscator.obfuscateLong(2090197856513702069L));
    assertEquals("b2d1e7dbd2b7c7", obfuscator.obfuscateLong(-7610100052787030355L));
    assertEquals("bafb16f27334a", obfuscator.obfuscateLong(-6306291253572087852L));
    assertEquals("9cec374c1381", obfuscator.obfuscateLong(-1133199824324347252L));
  }
}
