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

goog.require('goog.json');
goog.require('goog.net.XhrIo');
goog.require('goog.events');
goog.require('goog.events.Event');
goog.require('goog.events.EventTarget');

goog.provide('swissvault.rest.RemoteCall');
goog.provide('swissvault.rest.EventType');
goog.provide('swissvault.rest.SuccessEvent');
goog.provide('swissvault.rest.FailureEvent');

swissvault.rest.EventType = {
  SUCCESS: 'success',
  FAILURE: 'failure'
};

swissvault.rest.SuccessEvent = function(target, opt_json) {
  goog.events.Event.call(this, swissvault.rest.EventType.SUCCESS, target);

  this.json = opt_json || null;
};
goog.inherits(swissvault.rest.SuccessEvent, goog.events.Event);

swissvault.rest.FailureEvent = function(target) {
  goog.events.Event.call(this, swissvault.rest.EventType.FAILURE, target);
};
goog.inherits(swissvault.rest.FailureEvent, goog.events.Event);

swissvault.rest.RemoteCall = function() {
  goog.events.EventTarget.call(this);

  this.xhr = new goog.net.XhrIo();
};
goog.inherits(swissvault.rest.RemoteCall, goog.events.EventTarget);

swissvault.rest.RemoteCall.prototype.listKeys = function() {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys', 'GET');
};

swissvault.rest.RemoteCall.prototype.listSecrets = function(keyId) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets', 'GET');
};

swissvault.rest.RemoteCall.prototype.listSecretProperties = function(keyId, secretId) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets/' + secretId + '/properties', 'GET');
};

swissvault.rest.RemoteCall.prototype.getKey = function(keyId) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId, 'GET');
};

swissvault.rest.RemoteCall.prototype.newKey = function(key) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys', 'POST', goog.json.serialize(key), {'Content-Type': 'application/json'});
};

swissvault.rest.RemoteCall.prototype.newSecret = function(keyId, secret) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets',
      'POST', goog.json.serialize(secret), {'Content-Type': 'application/json'});
};

swissvault.rest.RemoteCall.prototype.newSecretProperty = function(keyId, secretId, property) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets/' + secretId + '/properties',
      'POST', goog.json.serialize(property), {'Content-Type': 'application/json'});
};

swissvault.rest.RemoteCall.prototype.updateKey = function(keyId, key) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId,
      'PUT', goog.json.serialize(key), {'Content-Type': 'application/json'});
};

swissvault.rest.RemoteCall.prototype.updateSecret = function(keyId, secretId, secret) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets/' + secretId,
      'PUT', goog.json.serialize(secret), {'Content-Type': 'application/json'});
};

swissvault.rest.RemoteCall.prototype.updateSecretProperty = function(keyId, secretId, propertyId, property) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.jsonResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets/' + secretId + '/properties/' + propertyId,
      'PUT', goog.json.serialize(property), {'Content-Type': 'application/json'});
};

swissvault.rest.RemoteCall.prototype.deleteSecret = function(keyId, secretId) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.emptyResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets/' + secretId, 'DELETE');
};

swissvault.rest.RemoteCall.prototype.deleteSecretProperty = function(keyId, secretId, propertyId) {
  goog.events.listen(this.xhr, goog.net.EventType.COMPLETE, this.emptyResponseCallback_, false, this);

  this.xhr.setTimeoutInterval(10 * 1000);
  this.xhr.send('/api/keys/' + keyId + '/secrets/' + secretId + '/properties/' + propertyId, 'DELETE');
};

swissvault.rest.RemoteCall.prototype.jsonResponseCallback_ = function(e) {
  var xhr = e.target;
  if (xhr.isSuccess()) {
    this.dispatchEvent(new swissvault.rest.SuccessEvent(this, xhr.getResponseJson()));
  } else {
    this.dispatchEvent(new swissvault.rest.FailureEvent(this));
  }
};

swissvault.rest.RemoteCall.prototype.emptyResponseCallback_ = function(e) {
  var xhr = e.target;
  if (xhr.isSuccess()) {
    this.dispatchEvent(new swissvault.rest.SuccessEvent(this));
  } else {
    this.dispatchEvent(new swissvault.rest.FailureEvent(this));
  }
};