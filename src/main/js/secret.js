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

goog.provide('swissvault.Secret');

goog.require('swissvault.crypto');

swissvault.Secret = function() {
  this.id = null;
  this.encrypted_name = null;
  this.encrypted_description = null;
};

swissvault.Secret.fromJson = function(json) {
  var secret = new swissvault.Secret();
  secret.id = json.id;
  secret.encrypted_name = json.name;
  secret.encrypted_description = json.description;
  return secret;
};

swissvault.Secret.prototype.getName = function(key) {
  key.assertDecrypted();
  return swissvault.crypto.decrypt(key.raw_secret, this.encrypted_name, false);
};

swissvault.Secret.prototype.getDescription = function(key) {
  key.assertDecrypted();
  return swissvault.crypto.decrypt(key.raw_secret, this.encrypted_description, false);
};

swissvault.Secret.prototype.setName = function(key, name) {
  key.assertDecrypted();
  this.encrypted_name = swissvault.crypto.encrypt(key.raw_secret, name);
};

swissvault.Secret.prototype.setDescription = function(key, description) {
  key.assertDecrypted();
  this.encrypted_description = swissvault.crypto.encrypt(key.raw_secret, description);
};

swissvault.Secret.prototype.getId = function() {
  return this.id;
};

swissvault.Secret.prototype.toJson = function() {
  var json = {};
  if (this.id) {
    json.id = this.id;
  }
  json.name = this.encrypted_name;
  json.description = this.encrypted_description;
  return json;
};