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

goog.provide('swissvault.SecretListPane');

goog.require('goog.dom');
goog.require('goog.ui.Component');
goog.require('swissvault.SecretPane');
goog.require('swissvault.ExportDataDialog');

swissvault.SecretListPane = function(key, secrets) {
  goog.ui.Component.call(this);

  this.key = key;
  this.secrets = secrets;
};
goog.inherits(swissvault.SecretListPane, goog.ui.Component);

swissvault.SecretListPane.prototype.createDom = function() {
  var content = goog.dom.createDom('div');
  this.setElementInternal(content);

  this.ul = goog.dom.createDom("ul");
  goog.dom.appendChild(content, this.ul);

  for (var i = 0; i < this.secrets.length; i++) {
    var li = goog.dom.createDom("li");
    goog.dom.appendChild(this.ul, li);

    var secret = this.secrets[i];
    var secretPane = new swissvault.SecretPane(this.key, secret);
    secretPane.decorateInternal(li);

    this.addChild(secretPane);
  }

  this.options = goog.dom.createDom("div");
  this.new_button = goog.dom.createDom("a", null, 'new');
  goog.events.listen(this.new_button, goog.events.EventType.CLICK, function() {
    this.addNewSecret();
  }, false, this);
  goog.dom.appendChild(this.options, this.new_button);

  this.export_button = goog.dom.createDom('a', null, 'export');
  goog.events.listen(this.export_button, goog.events.EventType.CLICK, function() {
    new swissvault.ExportDataDialog(this.key).show();
  }, false, this);
  goog.dom.appendChild(this.options, this.export_button);

  goog.dom.appendChild(content, this.options);
};

swissvault.SecretListPane.prototype.addNewSecret = function() {
  var li = goog.dom.createDom("li");
  goog.dom.appendChild(this.ul, li);

  var secretPane = new swissvault.SecretPane(this.key);
  secretPane.decorateInternal(li);

  this.addChild(secretPane);
};

swissvault.SecretListPane.prototype.removeSecretPane = function(pane) {
  goog.dom.removeNode(pane.getElement());
  this.removeChild(pane);
};
