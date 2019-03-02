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

goog.provide('swissvault.PropertyPane');

goog.require('goog.dom');
goog.require('goog.ui.Textarea');
goog.require('goog.ui.Component');
goog.require('swissvault.Property');

swissvault.PropertyPane = function(key, secret, opt_property) {
  goog.ui.Component.call(this);

  this.key = key;
  this.secret = secret;
  this.property = opt_property || null;
};
goog.inherits(swissvault.PropertyPane, goog.ui.Component);

swissvault.PropertyPane.prototype.decorateInternal = function(element) {
  this.setElementInternal(element);

  this.ro_fields = goog.dom.createDom('div');

  this.ro_name = goog.dom.createDom('div', 'propertyName');
  goog.dom.appendChild(this.ro_fields, this.ro_name);
  goog.dom.appendChild(this.ro_fields, goog.dom.createTextNode(" "));

  this.ro_value = goog.dom.createDom('div', 'propertyValue');
  goog.dom.appendChild(this.ro_fields, this.ro_value);
  goog.dom.appendChild(this.ro_fields, goog.dom.createTextNode(" "));

  this.edit_button = goog.dom.createDom('a', 'button', 'edit');
  goog.events.listen(this.edit_button, goog.events.EventType.CLICK, function() {
    this.edit();
  }, false, this);
  goog.dom.appendChild(this.ro_fields, this.edit_button);

  this.rw_fields = goog.dom.createDom('div');

  this.name = goog.dom.createDom('input', {type: 'text', 'class': 'propertyName propertyNameEdit'});
  goog.dom.appendChild(this.rw_fields, this.name);
  this.value = goog.dom.createDom('input', {type: 'text', 'class': 'propertyValue propertyValueEdit'});
  goog.dom.appendChild(this.rw_fields, this.value);
  this.save_button = goog.dom.createDom('a', 'button', 'save property');
  goog.events.listen(this.save_button, goog.events.EventType.CLICK, function() {
    this.save();
  }, false, this);
  goog.dom.appendChild(this.rw_fields, this.save_button);
  this.delete_button = goog.dom.createDom('a', 'button', 'delete property');
  goog.events.listen(this.delete_button, goog.events.EventType.CLICK, function() {
    this.deletePrompt();
  }, false, this);

  if (this.property) {
    this.showReadOnlyFields();
  } else {
    this.showEditableFields();
  }
};

swissvault.PropertyPane.prototype.enterDocument = function() {
  swissvault.PropertyPane.superClass_.enterDocument.call(this);
};

swissvault.PropertyPane.prototype.showEditableFields = function() {
  if (this.property) {
    this.name.value = this.property.getName(this.key);
    this.value.value = this.property.getValue(this.key);
    goog.dom.appendChild(this.rw_fields, this.delete_button);
  } else {
    this.name.value = '';
    this.value.value = '';
    goog.dom.removeNode(this.delete_button);
  }
  goog.dom.removeNode(this.ro_fields);
  goog.dom.appendChild(this.getElement(), this.rw_fields);
};

swissvault.PropertyPane.prototype.showReadOnlyFields = function() {
  if (this.property) {
    goog.dom.setTextContent(this.ro_name, this.property.getName(this.key));
    goog.dom.setTextContent(this.ro_value, this.property.getValue(this.key));
  } else {
    goog.dom.setTextContent(this.ro_name, '');
    goog.dom.setTextContent(this.ro_value, '');
  }
  goog.dom.removeNode(this.rw_fields);
  goog.dom.appendChild(this.getElement(), this.ro_fields);
};

swissvault.PropertyPane.prototype.edit = function() {
  this.showEditableFields();
};

swissvault.PropertyPane.prototype.save = function() {
  if (!this.property) {
    var property = new swissvault.Property();
    property.setName(this.key, this.name.value);
    property.setValue(this.key, this.value.value);

    var call = new swissvault.rest.RemoteCall();
    call.newSecretProperty(this.key.getId(), this.secret.getId(), property.toJson());
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      this.property = swissvault.Property.fromJson(e.json);
      this.showReadOnlyFields();
    }, false, this);
  } else {
    this.property.setName(this.key, this.name.value);
    this.property.setValue(this.key, this.value.value);

    var call = new swissvault.rest.RemoteCall();
    call.updateSecretProperty(this.key.getId(), this.secret.getId(), this.property.getId(), this.property.toJson());
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      this.property = swissvault.Property.fromJson(e.json);
      this.showReadOnlyFields();
    }, false, this);
  }
};

swissvault.PropertyPane.prototype.deletePrompt = function() {
  var del = confirm('Are you sure you want to delete this property?');
  if (del) {
    var call = new swissvault.rest.RemoteCall();
    call.deleteSecretProperty(this.key.getId(), this.secret.getId(), this.property.getId());
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      this.getParent().removePropertyPane(this);
    }, false, this);
  }
};
