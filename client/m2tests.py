#!/usr/bin/python

# Copyright 2012 Patrick Moor <patrick@moor.ws>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import unittest
from M2Crypto import EVP
import M2Crypto
from M2Crypto.Rand import rand_bytes


class HmacTest(unittest.TestCase):

  def _runTest(self, key, data, hash):
    self.assertEquals(
      hash,
      EVP.hmac(key.decode("hex"), data.decode("hex"), algo="sha256").encode("hex"))

  def test_one(self):
    self._runTest(
      "0b" * 20,
      "4869205468657265",
      "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7")

  def test_two(self):
    self._runTest(
      "4a656665",
      "7768617420646f2079612077616e7420666f72206e6f7468696e673f",
      "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843")

  def test_three(self):
    self._runTest(
      "aa" * 20,
      "dd" * 50,
      "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe")

  def test_four(self):
    self._runTest(
      "0102030405060708090a0b0c0d0e0f10111213141516171819",
      "cd" * 50,
      "82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b")

  def test_five(self):
    self._runTest(
      "aa" * 131,
      "54657374205573696e67204c6172676572205468616e20426c6f636b2d53697a65204b6579202d2048617368204b6579204669727374",
      "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54")

class AesTest(unittest.TestCase):

  def _runTest(self, key, iv, plaintext, ciphertext):
    cipher = EVP.Cipher(alg="aes_256_cbc", key=key.decode("hex"), iv=iv.decode("hex"), op=M2Crypto.encrypt, padding=0)
    c = cipher.update(plaintext.decode("hex"))
    c += cipher.final()
    self.assertEquals(ciphertext, c.encode("hex"))

    cipher = EVP.Cipher(alg="aes_256_cbc", key=key.decode("hex"), iv=iv.decode("hex"), op=M2Crypto.decrypt, padding=0)
    new_plaintext = cipher.update(c)
    new_plaintext += cipher.final()
    self.assertEquals(plaintext, new_plaintext.encode("hex"))

  def test_one(self):
    self._runTest(
      "8000000000000000000000000000000000000000000000000000000000000000",
      "00000000000000000000000000000000",
      "00000000000000000000000000000000",
      "e35a6dcb19b201a01ebcfa8aa22b5759")

  def test_two(self):
    self._runTest(
      "fffffffffffffffffffffffffffffffffffffffffffffff80000000000000000",
      "00000000000000000000000000000000",
      "00000000000000000000000000000000",
      "6168b00ba7859e0970ecfd757efecf7c")

  def test_three(self):
    self._runTest(
      "0000000000000000000000000000000000000000000000000000000000000000",
      "00000000000000000000000000000000",
      "ffffff00000000000000000000000000",
      "ac86bc606b6640c309e782f232bf367f")

  def test_four(self):
    self._runTest(
      "fca02f3d5011cfc5c1e23165d413a049d4526a991827424d896fe3435e0bf68e",
      "00000000000000000000000000000000",
      "00000000000000000000000000000000",
      "179a49c712154bbffbe6e7a84a18e220")

  def test_five(self):
    self._runTest(
      "0000000000000000000000000000000000000000000000000000000000000000",
      "00000000000000000000000000000000",
      "8a560769d605868ad80d819bdba03771",
      "38f2c7ae10612415d27ca190d27da8b4")


class PBKDF2Test(unittest.TestCase):
  """based on http://www.ietf.org/rfc/rfc6070.txt"""

  def test_one(self):
    self.assertEqual(
      "0c60c80f961f0e71f3a9b524af6012062fe037a6",
      EVP.pbkdf2("password", "salt", 1, 20).encode("hex"))

  def test_two(self):
    self.assertEqual(
      "ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957",
      EVP.pbkdf2("password", "salt", 2, 20).encode("hex"))

  def test_three(self):
    self.assertEqual(
      "4b007901b765489abead49d926f721d065a429c1",
      EVP.pbkdf2("password", "salt", 4096, 20).encode("hex"))

  def test_four(self):
    # this test case takes a long time (order of 1 minute)
    self.assertEqual(
      "eefe3d61cd4da4e4e9945b3d6ba2158c2634e984",
      EVP.pbkdf2("password", "salt", 16777216, 20).encode("hex"))

  def test_five(self):
    self.assertEqual(
      "3d2eec4fe41c849b80c8d83662c0e44a8b291a964cf2f07038",
      EVP.pbkdf2("passwordPASSWORDpassword", "saltSALTsaltSALTsaltSALTsaltSALTsalt", 4096, 25).encode("hex"))

  def test_six(self):
    self.assertEqual(
      "56fa6aa75548099dcc37d7f03425e0c3",
      EVP.pbkdf2("pass\0word", "sa\0lt", 4096, 16).encode("hex"))

if __name__ == '__main__':
  unittest.main()