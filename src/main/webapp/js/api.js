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

// The REST API. The server only ever sees encrypted values.

/** @typedef {import('./crypto.js').EncryptedValue} EncryptedValue */

/**
 * @typedef {object} KeyJson
 * @property {string} [id]
 * @property {EncryptedValue} name
 * @property {EncryptedValue} description
 * @property {EncryptedValue[]} encryptions the key's raw secret, wrapped with each of its passphrases
 */

/**
 * @typedef {object} SecretJson
 * @property {string} id
 * @property {EncryptedValue} name
 * @property {EncryptedValue} description
 */

/**
 * @typedef {object} PropertyJson
 * @property {string} id
 * @property {EncryptedValue} name
 * @property {EncryptedValue} value
 */

/** Thrown when the server responds with an error. */
export class ApiError extends Error {}

/**
 * @param {string} method
 * @param {string} path
 * @param {object} [body]
 * @returns {Promise<any>} the parsed JSON response, or undefined for an empty response
 */
async function call(method, path, body) {
  const response = await fetch('/api' + path, {
    method,
    headers: body ? {'Content-Type': 'application/json'} : {},
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(10_000),
  });
  if (!response.ok) {
    throw new ApiError(`${method} ${path} failed with status ${response.status}`);
  }
  return response.status === 204 ? undefined : response.json();
}

/** @returns {Promise<KeyJson[]>} */
export const listKeys = () => call('GET', '/keys');

/** @param {string} keyId @returns {Promise<KeyJson>} */
export const getKey = keyId => call('GET', `/keys/${keyId}`);

/** @param {KeyJson} key @returns {Promise<KeyJson>} */
export const createKey = key => call('POST', '/keys', key);

/** @param {string} keyId @param {KeyJson} key @returns {Promise<KeyJson>} */
export const updateKey = (keyId, key) => call('PUT', `/keys/${keyId}`, key);

/** @param {string} keyId @returns {Promise<SecretJson[]>} */
export const listSecrets = keyId => call('GET', `/keys/${keyId}/secrets`);

/** @param {string} keyId @param {Omit<SecretJson, 'id'>} secret @returns {Promise<SecretJson>} */
export const createSecret = (keyId, secret) => call('POST', `/keys/${keyId}/secrets`, secret);

/**
 * @param {string} keyId @param {string} secretId @param {Omit<SecretJson, 'id'>} secret
 * @returns {Promise<SecretJson>}
 */
export const updateSecret = (keyId, secretId, secret) =>
    call('PUT', `/keys/${keyId}/secrets/${secretId}`, secret);

/** @param {string} keyId @param {string} secretId @returns {Promise<void>} */
export const deleteSecret = (keyId, secretId) => call('DELETE', `/keys/${keyId}/secrets/${secretId}`);

/** @param {string} keyId @param {string} secretId @returns {Promise<PropertyJson[]>} */
export const listProperties = (keyId, secretId) => call('GET', `/keys/${keyId}/secrets/${secretId}/properties`);

/**
 * @param {string} keyId @param {string} secretId @param {Omit<PropertyJson, 'id'>} property
 * @returns {Promise<PropertyJson>}
 */
export const createProperty = (keyId, secretId, property) =>
    call('POST', `/keys/${keyId}/secrets/${secretId}/properties`, property);

/**
 * @param {string} keyId @param {string} secretId @param {string} propertyId
 * @param {Omit<PropertyJson, 'id'>} property
 * @returns {Promise<PropertyJson>}
 */
export const updateProperty = (keyId, secretId, propertyId, property) =>
    call('PUT', `/keys/${keyId}/secrets/${secretId}/properties/${propertyId}`, property);

/** @param {string} keyId @param {string} secretId @param {string} propertyId @returns {Promise<void>} */
export const deleteProperty = (keyId, secretId, propertyId) =>
    call('DELETE', `/keys/${keyId}/secrets/${secretId}/properties/${propertyId}`);
