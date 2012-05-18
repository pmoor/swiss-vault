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

goog.provide('swissvault.KeyListDialog');

swissvault.KeyListDialog = function(keys, passphrase) {
  goog.ui.Dialog.call(this, null, false);

  this.setTitle("Secret Keys");
  this.setButtonSet(null);
  this.setEscapeToCancel(false);
  this.setHasTitleCloseButton(false);

  this.ul = goog.dom.createDom("ul");
  for (var i = 0; i < keys.length; i++) {
    var key = keys[i];
    key.attemptDecryption(passphrase);
    var li = goog.dom.createDom("li");
    if (key.isDecrypted()) {
      var keyName = goog.dom.createDom("span", null, key.getName());
      goog.dom.appendChild(li, keyName);
      goog.dom.append(li, ' ');

      var keyDescription = goog.dom.createDom("span", null, key.getDescription());
      goog.dom.appendChild(li, keyDescription);
      goog.dom.append(li, ' ');

      var selectLink = goog.dom.createDom("a", null, "select");
      goog.events.listen(selectLink, goog.events.EventType.CLICK, function(key) {
        return function() {
          this.dispatchEvent({type: swissvault.EventType.KEY_SELECTED, key: key});
        }}(key), false, this);
      goog.dom.appendChild(li, selectLink);
    } else {
      var unknownKey = goog.dom.createDom("span", null, "Key #" + key.getId());
      goog.dom.appendChild(li, unknownKey);
    }

    goog.dom.append(li, ' ');
    var editLink = goog.dom.createDom("a", null, "edit");
    goog.events.listen(editLink, goog.events.EventType.CLICK, function(key) {
      return function() {
        this.dispatchEvent({type: swissvault.EventType.KEY_EDITED, key: key});
      }}(key), false, this);
    goog.dom.appendChild(li, editLink);

    goog.dom.appendChild(this.ul, li);
  }
  goog.dom.appendChild(this.getContentElement(), this.ul);

  var newKeyText = goog.dom.createDom("a", null, "Create new key...");
  goog.events.listen(newKeyText, goog.events.EventType.CLICK, function() {
      this.dispatchEvent(swissvault.EventType.CREATE_NEW_KEY);
    }, false, this);
  goog.dom.appendChild(this.getContentElement(), newKeyText);

  var differentPassphrase = goog.dom.createDom("a", null, "Different passphrase...");
  goog.events.listen(differentPassphrase, goog.events.EventType.CLICK, function() {
    this.dispatchEvent(swissvault.EventType.CANCEL);
  }, false, this);
  goog.dom.appendChild(this.getContentElement(), differentPassphrase);
};
goog.inherits(swissvault.KeyListDialog, goog.ui.Dialog);

swissvault.KeyListDialog.prototype.show = function() {
  this.setVisible(true);
};