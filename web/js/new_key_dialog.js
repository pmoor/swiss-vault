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

goog.provide('swissvault.NewKeyDialog');

swissvault.NewKeyDialog = function() {
  goog.ui.Dialog.call(this, null, false);

  this.setTitle("New Key");
  this.setButtonSet(null);
  this.setEscapeToCancel(false);
  this.setHasTitleCloseButton(false);

  this.error_box = goog.dom.createDom('div', { style: 'color: red' });
  goog.dom.appendChild(this.getContentElement(), this.error_box);

  var form = goog.dom.createDom('form', {
    action: '',
    onsubmit: function() { return false }});

  this.name_label = goog.dom.createDom('label', { 'for': 'name' }, "Name");
  this.name_input = goog.dom.createDom('input', { type: 'text', size: 24, name: "name" });

  this.description_label = goog.dom.createDom('label', { 'for': 'description' }, "Description");
  this.description_input = goog.dom.createDom('input', { type: 'text', size: 24, name: "description" });

  this.passphrase1_label = goog.dom.createDom('label', { 'for': 'passphrase1' }, "Passphrase");
  this.passphrase1_input = goog.dom.createDom('input', { type: 'password', size: 24, name: "passphrase1" });

  this.passphrase2_label = goog.dom.createDom('label', { 'for': 'passphrase2' }, "Passphrase (repeat)");
  this.passphrase2_input = goog.dom.createDom('input', { type: 'password', size: 24, name: "passphrase2" });

  var submit = goog.dom.createDom('input', {
    'type': 'submit',
    'value': 'create' });
  var cancel = goog.dom.createDom('input', {
    'type': 'button',
    'value': 'cancel' });
  goog.events.listen(cancel, goog.events.EventType.CLICK, function() {
    this.dispatchEvent(swissvault.EventType.CANCEL);
  }, false, this);

  goog.dom.appendChild(form, this.name_label);
  goog.dom.appendChild(form, this.name_input);
  goog.dom.appendChild(form, goog.dom.createDom('br'));
  goog.dom.appendChild(form, this.description_label);
  goog.dom.appendChild(form, this.description_input);
  goog.dom.appendChild(form, goog.dom.createDom('br'));
  goog.dom.appendChild(form, this.passphrase1_label);
  goog.dom.appendChild(form, this.passphrase1_input);
  goog.dom.appendChild(form, goog.dom.createDom('br'));
  goog.dom.appendChild(form, this.passphrase2_label);
  goog.dom.appendChild(form, this.passphrase2_input);
  goog.dom.appendChild(form, goog.dom.createDom('br'));
  goog.dom.appendChild(form, submit);
  goog.dom.appendChild(form, cancel);

  goog.dom.appendChild(this.getContentElement(), form);

  goog.events.listen(form, goog.events.EventType.SUBMIT, function() {
    if (this.passphrase1_input.value != this.passphrase2_input.value || this.passphrase1_input.value.length < 1) {
      goog.dom.setTextContent(this.error_box, "passphrases do not match");
    } else if (this.name_input.value.length < 1) {
      goog.dom.setTextContent(this.error_box, "name cannot be empty");
    } else {
      goog.dom.setTextContent(this.error_box, '');
      var key = swissvault.Key.createNew(
          this.name_input.value, this.description_input.value, this.passphrase1_input.value);
      this.dispatchEvent({type: swissvault.EventType.NEW_KEY_CREATED, key: key});
    }
  }, false, this);
};
goog.inherits(swissvault.NewKeyDialog, goog.ui.Dialog);

swissvault.NewKeyDialog.prototype.show = function() {
  this.setVisible(true);
  this.name_input.focus();
};