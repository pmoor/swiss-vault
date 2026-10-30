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

// Small DOM helpers. Text is only ever inserted as text nodes, never as HTML.

/**
 * @template {keyof HTMLElementTagNameMap} K
 * @param {K} tag
 * @param {Record<string, string>} [attributes]
 * @param {...(Node | string)} children
 * @returns {HTMLElementTagNameMap[K]}
 */
export function h(tag, attributes = {}, ...children) {
  const element = document.createElement(tag);
  for (const [name, value] of Object.entries(attributes)) {
    element.setAttribute(name, value);
  }
  element.append(...children);
  return element;
}

/**
 * A button that is disabled while the promise returned by its action is pending, so it cannot be
 * triggered twice by accident.
 *
 * @param {string} text
 * @param {() => unknown} action
 * @param {string} [className] 'link' looks like a link, 'button' like a small boxed button
 */
export function button(text, action, className = 'link') {
  const element = h('button', {type: 'button', class: className}, text);
  element.addEventListener('click', async () => {
    element.disabled = true;
    try {
      await action();
    } finally {
      element.disabled = false;
    }
  });
  return element;
}

/** A modal dialog which, like the original ones, can only be closed by its own buttons. */
export class Dialog {
  /** @param {string} title */
  constructor(title) {
    this.content = h('div', {class: 'dialog-content'});
    this.element = h('dialog', {}, h('h2', {class: 'dialog-title'}, title), this.content);
    this.closed = false;
    this.element.addEventListener('cancel', event => event.preventDefault());
    // browsers close modal dialogs on repeated escape key presses regardless, so reopen it
    this.element.addEventListener('close', () => {
      if (!this.closed) {
        this.element.showModal();
      }
    });
  }

  show() {
    document.body.append(this.element);
    this.element.showModal();
  }

  close() {
    this.closed = true;
    this.element.close();
    this.element.remove();
  }
}
