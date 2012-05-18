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
goog.require('goog.format.JsonPrettyPrinter');
goog.require('swissvault.rest.RemoteCall');

goog.provide('swissvault.ExportDataDialog');

swissvault.ExportDataDialog = function(key) {
  goog.ui.Dialog.call(this, null, false);

  key.assertDecrypted();
  this.key = key;

  this.setTitle("Export Data");
  this.setButtonSet(null);

  this.textarea = goog.dom.createDom("textarea", { rows: 30, cols: 80 });
  this.data = new Array();
  this.done = false;

  goog.dom.appendChild(this.getContentElement(), this.textarea);
};
goog.inherits(swissvault.ExportDataDialog, goog.ui.Dialog);

swissvault.ExportDataDialog.prototype.show = function() {
  this.setVisible(true);

  var call = new swissvault.rest.RemoteCall();
  goog.events.listen(call, swissvault.rest.EventType.SUCCESS, function(e) {
    for (var i = 0; i < e.json.length; i++) {
      var secret = swissvault.Secret.fromJson(e.json[i]);

      var call2 = new swissvault.rest.RemoteCall();
      goog.events.listen(call2, swissvault.rest.EventType.SUCCESS, function(secret) {
        return function(e) {
          var exportData = {};
          exportData.name = secret.getName(this.key);
          exportData.description = secret.getDescription(this.key);
          exportData.properties = new Array();

          for (var j = 0; j < e.json.length; j++) {
            var property = swissvault.Property.fromJson(e.json[j]);
            exportData.properties.push({
              name: property.getName(this.key),
              value: property.getValue(this.key)
            });
          }
          this.data.push(exportData);
          if (this.done) {
            var printer = new goog.format.JsonPrettyPrinter();
            goog.dom.setTextContent(this.textarea, printer.format(this.data));
          }
        }
      }(secret), false, this);
      call2.listSecretProperties(this.key.getId(), secret.getId());
    }

    this.done = true;
  }, false, this);

  call.listSecrets(this.key.getId());
};