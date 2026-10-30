/*
  Copyright 2026 Patrick Moor <patrick@moor.ws>

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

import * as api from './api.js';
import {askPassphrase, chooseKey, createKey, editKey} from './dialogs.js';
import {Dialog, button, h} from './dom.js';
import {Key} from './key.js';
import {showSecrets} from './secrets.js';

/** Asks for a passphrase, then for the key to work with. */
async function start() {
  const keys = (await api.listKeys()).map(json => new Key(json));
  const passphrase = await askPassphrase();
  await Promise.all(keys.map(key => key.unlock(passphrase)));

  const choice = await chooseKey(keys);
  switch (choice.action) {
    case 'select':
      return showSecrets(choice.key);
    case 'edit':
      await editKey(new Key(await api.getKey(choice.key.id)));
      return start();
    case 'create': {
      const key = await createKey();
      if (key) {
        await api.createKey(key.toJson());
      }
      return start();
    }
    case 'different-passphrase':
      return start();
  }
}

/** @param {unknown} error */
function showError(error) {
  console.error(error);
  if (!document.querySelector('.error-dialog')) {
    // most likely the login expired, which needs a reload to log in again
    const dialog = new Dialog('Error');
    dialog.element.classList.add('error-dialog');
    dialog.content.append(
        h('p', {}, 'Something went wrong talking to the server. Reload the page to log in again.'),
        button('reload', () => location.reload(), 'dialog-button'));
    dialog.show();
  }
}

window.addEventListener('unhandledrejection', event => showError(event.reason));
window.addEventListener('error', event => showError(event.error));

start();
