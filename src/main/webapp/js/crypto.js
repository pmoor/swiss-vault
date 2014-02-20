/*
  Copyright 2012 Patrick Moor <patrick@moor.ws>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

// AES block size: 128bit (16 bytes, 4 words)
// AES IV size: 128bit
// AES key size: 256bit (32 bytes, 8 words)

goog.provide('swissvault.crypto');

swissvault.crypto.encrypt = function(key, cleartextUtf8) {
  var cleartext = Crypto.charenc.UTF8.stringToBytes(cleartextUtf8);

  var iv = Crypto.util.randomBytes(16);
  var signature = Crypto.HMAC(Crypto.SHA256, iv.concat(cleartext), key, {asBytes: true});

  // this messes with cleartext
  var ciphertext = Crypto.AES.encrypt(
      cleartext, key, { mode: new Crypto.mode.CBC(Crypto.pad.pkcs7), iv: iv, asBytes: true });

  return {
    'version': 0,
    'algorithm': 'AES_256_CBC_HMAC_SHA256',
    'iv': Crypto.util.bytesToBase64(iv),
    'ciphertext': Crypto.util.bytesToBase64(ciphertext),
    'signature': Crypto.util.bytesToBase64(signature)
  }
};

swissvault.crypto.decrypt = function(key, encryption, asBytes) {
  var iv = Crypto.util.base64ToBytes(encryption.iv);
  var ciphertext = Crypto.util.base64ToBytes(encryption.ciphertext);
  var signature = encryption.signature;

  var cleartext = Crypto.AES.decrypt(
      ciphertext, key, { mode: new Crypto.mode.CBC(Crypto.pad.pkcs7), iv: iv, asBytes: true });

  var expectedSignature = Crypto.util.bytesToBase64(
      Crypto.HMAC(Crypto.SHA256, iv.concat(cleartext), key, {asBytes: true}));
  if (expectedSignature != signature) {
    throw new Error("signature does not match");
  }

  return asBytes ? cleartext : Crypto.charenc.UTF8.bytesToString(cleartext);
};

swissvault.crypto.newEncryption = function(passphraseUtf8) {
  var passphrase = Crypto.charenc.UTF8.stringToBytes(passphraseUtf8);

  var secret_key_part_1 = Crypto.util.randomBytes(32);
  var secret_key_part_2 = Crypto.SHA256(passphrase, {asBytes: true});
  var secret_key = new Array(32);
  for (var i = 0; i < 32; i++) {
    secret_key[i] = secret_key_part_1[i] ^ secret_key_part_2[i];
  }

  return swissvault.crypto.encryptWithPassphrase(passphraseUtf8, secret_key);
};

swissvault.crypto.encryptWithPassphrase = function(passphraseUtf8, secret_key) {
  var passphrase = Crypto.charenc.UTF8.stringToBytes(passphraseUtf8);

  var iv = Crypto.util.randomBytes(16);
  var key = Crypto.PBKDF2(passphrase, iv, 32, {iterations: 1000, asBytes: true});
  
  var signature = Crypto.HMAC(Crypto.SHA256, iv.concat(secret_key), key, {asBytes: true});
  var ciphertext = Crypto.AES.encrypt(
      secret_key.slice(0), key, { mode: new Crypto.mode.CBC(Crypto.pad.pkcs7), iv: iv, asBytes: true });

  return {
    'version': 0,
    'algorithm': 'PBKDF2_SHA1_AES_256_CBC_HMAC_SHA256',
    'iv': Crypto.util.bytesToBase64(iv),
    'ciphertext': Crypto.util.bytesToBase64(ciphertext),
    'signature': Crypto.util.bytesToBase64(signature)
  }
};

swissvault.crypto.tryEncryptionDecryption = function(encryption, passphraseUtf8) {
  var passphrase = Crypto.charenc.UTF8.stringToBytes(passphraseUtf8);
  var salt = Crypto.util.base64ToBytes(encryption.iv);
  var key = Crypto.PBKDF2(passphrase, salt, 32, {iterations: 1000, asBytes: true});
  try {
    return swissvault.crypto.decrypt(key, encryption, true);
  } catch(e) {
    return null;
  }
};