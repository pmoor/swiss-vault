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

goog.provide('swissvault.EventType');

goog.require('goog.events');

swissvault.EventType = {
  PASSPHRASE_ENTERED: goog.events.getUniqueId("passphrase-entered"),
  KEY_SELECTED: goog.events.getUniqueId("key-selected"),
  CREATE_NEW_KEY: goog.events.getUniqueId("create-new-key"),
  NEW_KEY_CREATED: goog.events.getUniqueId("new-key-created"),
  CANCEL: goog.events.getUniqueId("cancel"),
  KEY_EDITED: goog.events.getUniqueId("key-edited")
};