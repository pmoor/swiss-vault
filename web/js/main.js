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
goog.require('swissvault.crypto');
goog.require('swissvault.Key');
goog.require('swissvault.rest.RemoteCall');
goog.require('swissvault.PassphraseDialog');
goog.require('swissvault.KeyListDialog');
goog.require('swissvault.Secret');
goog.require('swissvault.SecretListPane');
goog.require('swissvault.NewKeyDialog');
goog.require('swissvault.EditKeyDialog');

goog.provide('swissvault.onLoad');

swissvault.showKeyDialog_ = function(keys, passphraseUtf8) {
  var dialog = new swissvault.KeyListDialog(keys, passphraseUtf8);
  goog.events.listen(dialog, swissvault.EventType.KEY_SELECTED, function(e) {
    dialog.dispose();

    var selectedKey = e.key;

    var call = new swissvault.rest.RemoteCall();
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      var secrets = e.json;
      var domainSecrets = new Array(secrets.length);
      for (var i = 0; i < secrets.length; i++) {
        domainSecrets[i] = swissvault.Secret.fromJson(secrets[i]);
      }
      new swissvault.SecretListPane(selectedKey, domainSecrets).render();
    });

    call.listSecrets(selectedKey.getId());
  });

  goog.events.listen(dialog, swissvault.EventType.KEY_EDITED, function(e) {
    dialog.dispose();

    var call = new swissvault.rest.RemoteCall();
    goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
      var key = swissvault.Key.fromJson(e.json);
      var editDialog = new swissvault.EditKeyDialog(key);
      editDialog.show();
    });
    call.getKey(e.key.getId())
  });

  goog.events.listen(dialog, swissvault.EventType.CREATE_NEW_KEY, function(e) {
    dialog.dispose();

    var newDialog = new swissvault.NewKeyDialog();
    goog.events.listen(newDialog, swissvault.EventType.NEW_KEY_CREATED, function(e) {
      newDialog.dispose();

      var key = e.key;
      var call = new swissvault.rest.RemoteCall();
      goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
        swissvault.onLoad();
      });
      call.newKey(key.toJson());
    });
    goog.events.listen(newDialog, swissvault.EventType.CANCEL, function(e) {
      newDialog.dispose();
      swissvault.onLoad();
    });
    newDialog.show();
  });

  goog.events.listen(dialog, swissvault.EventType.CANCEL, function(e) {
    dialog.dispose();
    swissvault.onLoad();
  });

  dialog.show();
};

swissvault.onLoad = function() {
  var call = new swissvault.rest.RemoteCall();
  goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
    var keys = e.json;
    var globalKeys = new Array();
    if (keys.length > 0) {
      globalKeys = new Array(keys.length);
      for (var i = 0; i < keys.length; i++) {
        globalKeys[i] = swissvault.Key.fromJson(keys[i]);
      }
    }

    var dialog = new swissvault.PassphraseDialog();
    goog.events.listen(dialog, swissvault.EventType.PASSPHRASE_ENTERED, function(e) {
      var passphraseUtf8 = dialog.getPassphrase();
      dialog.dispose();
      swissvault.showKeyDialog_(globalKeys, passphraseUtf8);
    });
    dialog.show();
  });
  call.listKeys();
};

goog.exportSymbol('swissvault.onLoad', swissvault.onLoad);