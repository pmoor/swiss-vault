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

goog.provide('swissvault.Property');

goog.require('swissvault.crypto');

swissvault.Property = function() {
  this.id = null;
  this.encrypted_name = null;
  this.encrypted_value = null;
};

swissvault.Property.fromJson = function(json) {
  var property = new swissvault.Property();
  property.id = json.id;
  property.encrypted_name = json.name;
  property.encrypted_value = json.value;
  return property;
};

swissvault.Property.prototype.getName = function(key) {
  key.assertDecrypted();
  return swissvault.crypto.decrypt(key.raw_secret, this.encrypted_name, false);
};

swissvault.Property.prototype.getValue = function(key) {
  key.assertDecrypted();
  return swissvault.crypto.decrypt(key.raw_secret, this.encrypted_value, false);
};

swissvault.Property.prototype.setName = function(key, name) {
  key.assertDecrypted();
  this.encrypted_name = swissvault.crypto.encrypt(key.raw_secret, name);
};

swissvault.Property.prototype.setValue = function(key, value) {
  key.assertDecrypted();
  this.encrypted_value = swissvault.crypto.encrypt(key.raw_secret, value);
};

swissvault.Property.prototype.getId = function() {
  return this.id;
};

swissvault.Property.prototype.toJson = function() {
  var json = {};
  if (this.id) {
    json.id = this.id;
  }
  json.name = this.encrypted_name;
  json.value = this.encrypted_value;
  return json;
};