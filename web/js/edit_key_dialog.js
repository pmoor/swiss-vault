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

goog.require('goog.dom');
goog.require('goog.ui.Dialog');
goog.require('swissvault.EventType');

goog.provide('swissvault.EditKeyDialog');

swissvault.EditKeyDialog = function(key) {
  goog.ui.Dialog.call(this, null, false);

  this.key = key;

  this.setTitle("Edit Key");
  this.setButtonSet(null);
  this.setEscapeToCancel(false);
  this.setHasTitleCloseButton(false);

  this.passphrase_label = goog.dom.createDom('label', { 'for': 'passphrase' }, "Passphrase");
  this.passphrase_input = goog.dom.createDom('input', { type: 'password', size: 24, name: "passphrase" });

  var submit = goog.dom.createDom('input', {
    'type': 'button',
    'value': 'try' });
  goog.events.listen(submit, goog.events.EventType.CLICK, function() {
    var value = this.passphrase_input.value;
    this.passphrase_input.value = '';
    if (this.key.attemptDecryption(value)) {
      this.updateView();
    } else {
      this.updateView();
      this.passphrase_input.focus();
    }
  }, false, this);

  goog.dom.appendChild(this.getContentElement(), this.passphrase_label);
  goog.dom.appendChild(this.getContentElement(), this.passphrase_input);
  goog.dom.appendChild(this.getContentElement(), submit);

  this.encryption_list = goog.dom.createDom('ul');
  goog.dom.appendChild(this.getContentElement(), this.encryption_list);

  this.new_encryption_area = goog.dom.createDom('div');
  goog.dom.appendChild(this.getContentElement(), this.new_encryption_area);

  this.updateView();
};
goog.inherits(swissvault.EditKeyDialog, goog.ui.Dialog);

swissvault.EditKeyDialog.prototype.show = function() {
  this.setVisible(true);
  this.passphrase_input.focus();
};

swissvault.EditKeyDialog.prototype.updateView = function() {
  goog.dom.removeChildren(this.encryption_list);
  goog.dom.removeChildren(this.new_encryption_area);

  var encryptions = this.key.getEncryptions();
  for (var i = 0; i < encryptions.length; i++) {
    var style = '';
    if (this.key.isDecrypted() && this.key.getUsedEncryption() == i) {
      style = 'background: green;'
    } else {
      style = 'background: red;'
    }
    var li = goog.dom.createDom('li', { style: style }, goog.dom.createDom('span', null, 'Encryption #' + i));
    if (this.key.isDecrypted() && this.key.getUsedEncryption() != i) {
      var deleteButton = goog.dom.createDom('a', null, 'delete');
      goog.events.listen(deleteButton, goog.events.EventType.CLICK, function(i) {
        return function() {
          this.key.getEncryptions().splice(i, 1);
          var call = new swissvault.rest.RemoteCall();
          goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
            this.key = swissvault.Key.fromJson(e.json);
            this.key.attemptDecryption('');
            this.updateView();
          }, false, this);
          call.updateKey(this.key.getId(), this.key.toJson());
        }
      }(i), false, this);
      goog.dom.appendChild(li, deleteButton);
    }
    goog.dom.appendChild(this.encryption_list, li);
  }

  if (this.key.isDecrypted()) {
    this.new_passphrase_label = goog.dom.createDom('label', { 'for': 'new-passphrase' }, "Add Passphrase");
    this.new_passphrase_input = goog.dom.createDom('input', { type: 'password', size: 24, name: "new-passphrase" });
    var add = goog.dom.createDom('input', {
      'type': 'button',
      'value': 'add' });
    goog.events.listen(add, goog.events.EventType.CLICK, function() {
      var value = this.new_passphrase_input.value;
      this.new_passphrase_input.value = '';
      this.key.addEncryptionForPassphrase(value);

      var call = new swissvault.rest.RemoteCall();
      goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
        this.key = swissvault.Key.fromJson(e.json);
        this.key.attemptDecryption('');
        this.updateView();
      }, false, this);
      call.updateKey(this.key.getId(), this.key.toJson());
    }, false, this);

    goog.dom.appendChild(this.new_encryption_area, this.new_passphrase_label);
    goog.dom.appendChild(this.new_encryption_area, this.new_passphrase_input);
    goog.dom.appendChild(this.new_encryption_area, add);
  }

};