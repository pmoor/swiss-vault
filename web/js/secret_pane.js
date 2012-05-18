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

goog.provide('swissvault.SecretPane');

goog.require('goog.dom');
goog.require('goog.ui.Component');
goog.require('goog.ui.Zippy');
goog.require('swissvault.PropertyPane');

swissvault.SecretPane = function(key, opt_secret) {
  goog.ui.Component.call(this);

  this.key = key;
  this.secret = opt_secret || null;
};
goog.inherits(swissvault.SecretPane, goog.ui.Component);

swissvault.SecretPane.prototype.decorateInternal = function(element) {
  this.setElementInternal(element);

  this.header = goog.dom.createDom("div");
  goog.dom.appendChild(element, this.header);

  this.toggle_button = goog.dom.createDom('a', null, 'toggle');

  this.ro_fields = goog.dom.createDom('div');
  this.ro_name = goog.dom.createDom('div');
  goog.dom.appendChild(this.ro_fields, this.ro_name);
  this.ro_description = goog.dom.createDom('div');
  goog.dom.appendChild(this.ro_fields, this.ro_description);
  this.edit_button = goog.dom.createDom('a', null, 'edit');
  goog.events.listen(this.edit_button, goog.events.EventType.CLICK, function() {
    this.showEditableFields();
  }, false, this);
  goog.dom.appendChild(this.ro_fields, this.edit_button);

  this.rw_fields = goog.dom.createDom('div');
  this.rw_name = goog.dom.createDom('input', {type: 'text'});
  goog.dom.appendChild(this.rw_fields, this.rw_name);
  this.rw_description = goog.dom.createDom('input', {type: 'text'});
  goog.dom.appendChild(this.rw_fields, this.rw_description);
  this.save_button = goog.dom.createDom('a', null, 'save');
  goog.events.listen(this.save_button, goog.events.EventType.CLICK, function() {
    this.save();
  }, false, this);
  goog.dom.appendChild(this.rw_fields, this.save_button);

  this.content = goog.dom.createDom("div");
  goog.dom.appendChild(element, this.content);

  this.list = goog.dom.createDom("ul");
  goog.dom.appendChild(this.content, this.list);

  this.options = goog.dom.createDom("div");
  this.add_button = goog.dom.createDom("a", null, 'add');
  goog.events.listen(this.add_button, goog.events.EventType.CLICK, function() {
    this.createNewProperty();
  }, false, this);
  goog.dom.appendChild(this.options, this.add_button);

  this.delete_button = goog.dom.createDom("a", null, 'delete');
  goog.events.listen(this.delete_button, goog.events.EventType.CLICK, function() {
    this.deleteSecret();
  }, false, this);
  goog.dom.appendChild(this.options, this.delete_button);

  goog.dom.appendChild(this.content, this.options);

  this.zippy = new goog.ui.Zippy(this.toggle_button, this.content);

  if (this.secret) {
    this.showReadOnlyFields();
  } else {
    this.showEditableFields();
  }
};

swissvault.SecretPane.prototype.showReadOnlyFields = function() {
  goog.dom.insertChildAt(this.header, this.toggle_button, 0);
  goog.dom.setTextContent(this.ro_name, this.secret.getName(this.key));
  goog.dom.setTextContent(this.ro_description, this.secret.getDescription(this.key));
  goog.dom.removeNode(this.rw_fields);
  goog.dom.appendChild(this.header, this.ro_fields);
};

swissvault.SecretPane.prototype.showEditableFields = function() {
  if (this.secret) {
    this.rw_name.value = this.secret.getName(this.key);
    this.rw_description.value = this.secret.getDescription(this.key);
  } else {
    this.rw_name.value = '';
    this.rw_description.value = '';
  }
  goog.dom.removeNode(this.ro_fields);
  goog.dom.appendChild(this.header, this.rw_fields);
};

swissvault.SecretPane.prototype.save = function() {
  if (!this.secret) {
    var secret = new swissvault.Secret();
    secret.setName(this.key, this.rw_name.value);
    secret.setDescription(this.key, this.rw_description.value);

    var call = new swissvault.rest.RemoteCall();
    call.newSecret(this.key.getId(), secret.toJson());
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      this.secret = swissvault.Secret.fromJson(e.json);
      this.showReadOnlyFields();
    }, false, this);
  } else {
    this.secret.setName(this.key, this.rw_name.value);
    this.secret.setDescription(this.key, this.rw_description.value);

    var call = new swissvault.rest.RemoteCall();
    call.updateSecret(this.key.getId(), this.secret.getId(), this.secret.toJson());
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      this.secret = swissvault.Secret.fromJson(e.json);
      this.showReadOnlyFields();
    }, false, this);
  }
};

swissvault.SecretPane.prototype.createNewProperty = function() {
  var li = goog.dom.createDom("li");
  goog.dom.appendChild(this.list, li);

  var child = new swissvault.PropertyPane(this.key, this.secret);
  child.decorateInternal(li);
  this.addChild(child);
};

swissvault.SecretPane.prototype.enterDocument = function() {
  swissvault.SecretPane.superClass_.enterDocument.call(this);

  goog.events.listen(this.zippy, goog.ui.Zippy.Events.TOGGLE, function(e) {
    if (this.zippy.isExpanded()) {
      var call = new swissvault.rest.RemoteCall();
      goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
        var properties = e.json;
        for (var i = 0; i < properties.length; i++) {
          var property = properties[i];
          var domainProperty = swissvault.Property.fromJson(property);

          var li = goog.dom.createDom("li");
          goog.dom.appendChild(this.list, li);

          var child = new swissvault.PropertyPane(this.key, this.secret, domainProperty);
          child.decorateInternal(li);
          this.addChild(child);
        }

        if (properties.length == 0) {
          this.createNewProperty();
        }
      }, false, this);
      call.listSecretProperties(this.key.getId(), this.secret.getId());
    } else {
      this.removeChildren();
      goog.dom.removeChildren(this.list);
    }
  }, false, this);
};

swissvault.SecretPane.prototype.removePropertyPane = function(pane) {
  goog.dom.removeNode(pane.getElement());
  this.removeChild(pane);
};

swissvault.SecretPane.prototype.deleteSecret = function() {
  var del = confirm('Are you sure you want to delete this secret and all its properties?');
  if (del) {
    var call = new swissvault.rest.RemoteCall();
    call.deleteSecret(this.key.getId(), this.secret.getId());
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      this.getParent().removeSecretPane(this);
    }, false, this);
  }
};