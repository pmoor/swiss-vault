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

import base64
import M2Crypto
from M2Crypto import EVP
from M2Crypto.Rand import rand_bytes

__author__ = 'pmoor'

def createEncryption(secret_key, value):
  iv = rand_bytes(16)
  cipher = EVP.Cipher(alg="aes_256_cbc", key=secret_key, iv=iv, op=M2Crypto.encrypt)
  ciphertext = cipher.update(value)
  ciphertext += cipher.final()
  hmac = EVP.hmac(secret_key, iv + value, algo="sha256")

  return {
    "version": 0,
    "algorithm": "AES_256_CBC_HMAC_SHA256",
    "iv": base64.standard_b64encode(iv),
    "ciphertext": base64.standard_b64encode(ciphertext),
    "signature": base64.standard_b64encode(hmac)
  }

def newRandomSecretKey():
  return rand_bytes(256 / 8)


def createEncryptionWithPassphrase(secret_key, passphrase):
  iv_and_salt = rand_bytes(128 / 8)
  password_derived_key = EVP.pbkdf2(passphrase, iv_and_salt, 1000, 256 / 8)

  cipher = EVP.Cipher(alg="aes_256_cbc", key=password_derived_key, iv=iv_and_salt, op=M2Crypto.encrypt)
  encrypted_key = cipher.update(secret_key)
  encrypted_key += cipher.final()
  hmac = EVP.hmac(password_derived_key, iv_and_salt + secret_key, algo="sha256")

  return {
    "version": 0,
    "algorithm": "PBKDF2_SHA1_AES_256_CBC_HMAC_SHA256",
    "iv": base64.standard_b64encode(iv_and_salt),
    "ciphertext": base64.standard_b64encode(encrypted_key),
    "signature": base64.standard_b64encode(hmac)
  }

def extractSecretKey(key_resource, passphrase):
  for encryption in key_resource["encryptions"]:
    assert encryption["algorithm"] == "PBKDF2_SHA1_AES_256_CBC_HMAC_SHA256"
    iv = base64.standard_b64decode(encryption["iv"])
    ciphertext = base64.standard_b64decode(encryption["ciphertext"])
    signature = base64.standard_b64decode(encryption["signature"])

    password_derived_key = EVP.pbkdf2(passphrase, iv, 1000, 256 / 8)

    try:
      cipher = EVP.Cipher(alg="aes_256_cbc", key=password_derived_key, iv=iv, op=M2Crypto.decrypt)
      secret_key = cipher.update(ciphertext)
      secret_key += cipher.final()
    except EVP.EVPError:
      # try other encryptions
      continue

    hmac = EVP.hmac(password_derived_key, iv + secret_key, algo="sha256")
    if hmac == signature:
      return secret_key


def decryptSecret(encryption, secret_key):
  assert encryption["algorithm"] == "AES_256_CBC_HMAC_SHA256"
  iv = base64.standard_b64decode(encryption["iv"])
  ciphertext = base64.standard_b64decode(encryption["ciphertext"])
  signature = base64.standard_b64decode(encryption["signature"])

  cipher = EVP.Cipher(alg="aes_256_cbc", key=secret_key, iv=iv, op=M2Crypto.decrypt)
  value = cipher.update(ciphertext)
  value += cipher.final()

  hmac = EVP.hmac(secret_key, iv + value, algo="sha256")
  if hmac != signature:
    raise Exception("invalid signature")
  return value