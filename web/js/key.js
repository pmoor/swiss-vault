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

goog.provide('swissvault.Key');

goog.require('swissvault.crypto');

swissvault.Key = function() {
  this.id = null;
  this.encrypted_name = null;
  this.encrypted_description = null;
  this.encryptions = [];

  this.raw_secret = null;
  this.used_encryption = -1;
};

swissvault.Key.fromJson = function(json) {
  var key = new swissvault.Key();
  key.id = json.id;
  key.encrypted_name = json.name;
  key.encrypted_description = json.description;
  key.encryptions = json.encryptions;
  return key;
};

swissvault.Key.createNew = function(name, description, passphrase) {
  var key = new swissvault.Key();
  key.encryptions = new Array(1);
  key.encryptions[0] = swissvault.crypto.newEncryption(passphrase);

  var raw_secret = swissvault.crypto.tryEncryptionDecryption(key.encryptions[0], passphrase);
  key.encrypted_name = swissvault.crypto.encrypt(raw_secret, name);
  key.encrypted_description = swissvault.crypto.encrypt(raw_secret, description);
  return key;
};

swissvault.Key.prototype.attemptDecryption = function(passphraseUtf8) {
  this.raw_secret = null;
  this.used_encryption = -1;
  for (var i = 0; i < this.encryptions.length; i++) {
    var encryption = this.encryptions[i];

    var raw_secret = swissvault.crypto.tryEncryptionDecryption(encryption, passphraseUtf8);
    if (raw_secret != null) {
      this.raw_secret = raw_secret;
      this.used_encryption = i;
      return true;
    }
  }
  return false;
};

swissvault.Key.prototype.addEncryptionForPassphrase = function(passphrase) {
  this.assertDecrypted();

  for (var i = 0; i < this.encryptions.length; i++) {
    var encryption = this.encryptions[i];
    var raw_secret = swissvault.crypto.tryEncryptionDecryption(encryption, passphrase);
    if (raw_secret != null) {
      return;
    }
  }
  // no existing encryption for this passphrase
  this.encryptions.push(swissvault.crypto.encryptWithPassphrase(passphrase, this.raw_secret));
};

swissvault.Key.prototype.assertDecrypted = function() {
  if (!this.isDecrypted()) {
    throw new Error("key is not decrypted");
  }
};

swissvault.Key.prototype.getName = function() {
  this.assertDecrypted();
  return swissvault.crypto.decrypt(this.raw_secret, this.encrypted_name, false);
};

swissvault.Key.prototype.getDescription = function() {
  this.assertDecrypted();
  return swissvault.crypto.decrypt(this.raw_secret, this.encrypted_description, false);
};

swissvault.Key.prototype.getEncryptions = function() {
  return this.encryptions;
};

swissvault.Key.prototype.getUsedEncryption = function() {
  this.assertDecrypted();
  return this.used_encryption;
};

swissvault.Key.prototype.getId = function() {
  return this.id;
};

swissvault.Key.prototype.isDecrypted = function() {
  return this.raw_secret != null;
};

swissvault.Key.prototype.toJson = function() {
  var json = {};
  if (this.id) {
    json.id = this.id;
  }
  json.name = this.encrypted_name;
  json.description = this.encrypted_description;
  json.encryptions = this.encryptions;
  return json;
};