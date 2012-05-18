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

goog.provide('swissvault.PassphraseDialog');

swissvault.PassphraseDialog = function() {
  goog.ui.Dialog.call(this, null, false);

  this.setTitle("Enter Passphrase");
  this.setButtonSet(null);
  this.setEscapeToCancel(false);
  this.setHasTitleCloseButton(false);

  this.input = goog.dom.createDom('input', {
    'type': 'password',
    'size': 24 });
  var submit = goog.dom.createDom('input', {
    'type': 'submit',
    'value': 'OK' });
  var form = goog.dom.createDom('form', {
    action: '',
    onsubmit: function() { return false }});
  goog.dom.appendChild(form, this.input);
  goog.dom.appendChild(form, submit);

  goog.dom.appendChild(this.getContentElement(), form);

  goog.events.listen(form, goog.events.EventType.SUBMIT, function() {
    this.dispatchEvent(swissvault.EventType.PASSPHRASE_ENTERED);
  }, false, this);
};
goog.inherits(swissvault.PassphraseDialog, goog.ui.Dialog);

swissvault.PassphraseDialog.prototype.show = function() {
  this.setVisible(true);
  this.input.focus();
};

swissvault.PassphraseDialog.prototype.getPassphrase = function() {
  return this.input.value;
};