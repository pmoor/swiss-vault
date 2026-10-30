#!/usr/bin/env python3

# Copyright 2012 Patrick Moor <patrick@moor.ws>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import argparse
import base64
import getpass
import hashlib
import hmac
import http.client
import json
import os
import sys
import time
import urllib.parse
import webbrowser
from typing import Any, Dict, List, Optional

try:
  from . import crypto
except ImportError:
  import crypto

DEFAULT_SERVER = os.environ.get("SWISSVAULT_SERVER", "http://localhost:8080")
DEV_HMAC_KEY = bytes.fromhex(
    "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")


class AuthenticationError(Exception):
  """Raised when authentication fails or credentials have expired."""


class ApiException(Exception):
  """Raised when an API call fails."""


def _get_cookie_file_path(server_url: str) -> str:
  parsed = urllib.parse.urlparse(server_url)
  host_slug = (parsed.netloc or "default").replace(":", "_")
  config_dir = os.path.expanduser(os.path.join("~", ".config", "swiss-vault"))
  os.makedirs(config_dir, mode=0o700, exist_ok=True)
  return os.path.join(config_dir, f"cookie_{host_slug}")


def save_cached_cookie(server_url: str, cookie_value: str) -> None:
  path = _get_cookie_file_path(server_url)
  cookie_clean = cookie_value.strip()
  if cookie_clean.startswith("auth="):
    cookie_clean = cookie_clean[len("auth="):]
  flags = os.O_WRONLY | os.O_CREAT | os.O_TRUNC
  with open(os.open(path, flags, 0o600), "w", encoding="utf-8") as f:
    f.write(cookie_clean)


def load_cached_cookie(server_url: str) -> Optional[str]:
  path = _get_cookie_file_path(server_url)
  if os.path.exists(path):
    try:
      with open(path, "r", encoding="utf-8") as f:
        cookie = f.read().strip()
        if cookie:
          return cookie
    except OSError:
      pass
  return None


def clear_cached_cookie(server_url: str) -> None:
  path = _get_cookie_file_path(server_url)
  if os.path.exists(path):
    try:
      os.remove(path)
    except OSError:
      pass


def make_dev_cookie(user_id: str, hmac_key: bytes = DEV_HMAC_KEY) -> str:
  """Generates a signed auth cookie for development/testing with development.properties."""
  now_ms = int(time.time() * 1000)
  payload = f"{user_id}:{now_ms}".encode("utf-8")
  signature = base64.b64encode(hmac.new(hmac_key, payload, hashlib.sha256).digest()).decode("ascii")
  return f"{user_id}:{now_ms}:{signature}"


def login(server_url: str = DEFAULT_SERVER,
          dev_user: Optional[str] = None,
          open_browser: bool = True) -> str:
  """Prompts the user to authenticate via the browser and paste the auth token."""
  if dev_user:
    cookie_value = make_dev_cookie(dev_user)
    save_cached_cookie(server_url, cookie_value)
    print(f"Generated dev auth cookie for user '{dev_user}'.")
    return cookie_value

  login_url = f"{server_url.rstrip('/')}/cli-login"
  print("=" * 60)
  print("Swiss Vault Login")
  print("=" * 60)
  print(f"Please log in and copy your token from:\n  {login_url}\n")

  if open_browser:
    try:
      webbrowser.open(login_url)
    except Exception:
      pass

  try:
    cookie_value = input("Paste auth token: ").strip()
  except (KeyboardInterrupt, EOFError):
    print("\nLogin cancelled.")
    raise AuthenticationError("Login cancelled.")

  if not cookie_value:
    raise AuthenticationError("No token entered. Login cancelled.")

  if cookie_value.startswith("auth="):
    cookie_value = cookie_value[len("auth="):]

  save_cached_cookie(server_url, cookie_value)
  print("Login successful!")
  return cookie_value


def GetNewCookie(user: Optional[str] = None,
                 password: Optional[str] = None,
                 server_url: str = DEFAULT_SERVER) -> str:
  """Legacy compatibility wrapper for login()."""
  return login(server_url=server_url)


class AuthenticatedConnection:
  """Manages HTTP/HTTPS communication with the Swiss Vault REST API."""

  def __init__(self,
               server_url: Optional[str] = None,
               auth_cookie: Optional[str] = None,
               dev_user: Optional[str] = None):
    self._server_url = (server_url or DEFAULT_SERVER).rstrip('/')
    parsed = urllib.parse.urlparse(self._server_url)
    self._scheme = parsed.scheme or "http"
    self._host = parsed.netloc or "localhost:8080"

    if auth_cookie:
      self._cookie = auth_cookie
    elif dev_user:
      self._cookie = make_dev_cookie(dev_user)
    elif os.environ.get("SWISSVAULT_COOKIE"):
      self._cookie = os.environ.get("SWISSVAULT_COOKIE")
    else:
      self._cookie = load_cached_cookie(self._server_url)

    if not self._cookie:
      self._cookie = login(server_url=self._server_url)

    if self._cookie.startswith("auth="):
      self._cookie = self._cookie[len("auth="):]

    self._headers = {
      "Cookie": f"auth={self._cookie}",
      "Content-Type": "application/json",
    }
    self._connection = self._create_connection()

  def _create_connection(self):
    if self._scheme == "https":
      return http.client.HTTPSConnection(self._host)
    return http.client.HTTPConnection(self._host)

  def _request(self, method: str, path: str, body: Optional[str] = None) -> Any:
    for attempt in range(2):
      try:
        self._connection.request(method, path, body=body, headers=self._headers)
        response = self._connection.getresponse()
      except (http.client.CannotSendRequest, http.client.BadStatusLine):
        self._connection = self._create_connection()
        self._connection.request(method, path, body=body, headers=self._headers)
        response = self._connection.getresponse()

      content = response.read().decode("utf-8")
      if response.status in (401, 403):
        if attempt == 0 and not os.environ.get("SWISSVAULT_NONINTERACTIVE"):
          print("\nAuthentication expired or rejected. Initiating login...")
          self._cookie = login(self._server_url)
          self._headers["Cookie"] = f"auth={self._cookie}"
          self._connection = self._create_connection()
          continue
        raise AuthenticationError(f"Authentication failed ({response.status}): {content}")

      if response.status not in (200, 204):
        raise ApiException(f"API request {method} {path} returned {response.status} {response.reason}: {content}")

      if not content or response.status == 204:
        return None
      return json.loads(content)

  def get(self, path: str) -> Any:
    return self._request("GET", path)

  def delete(self, path: str) -> None:
    self._request("DELETE", path)

  def post(self, path: str, data: Any) -> Any:
    return self._request("POST", path, body=json.dumps(data))

  def put(self, path: str, data: Any) -> Any:
    return self._request("PUT", path, body=json.dumps(data))

  def close(self) -> None:
    self._connection.close()


def getKeys(connection: AuthenticatedConnection) -> List[Dict[str, Any]]:
  return connection.get("/api/keys")


def getKey(connection: AuthenticatedConnection, key_id: str) -> Dict[str, Any]:
  return connection.get(f"/api/keys/{key_id}")


def createNewKey(connection: AuthenticatedConnection,
                 passphrase: str,
                 name: str,
                 description: str) -> Dict[str, Any]:
  secret_key = crypto.newRandomSecretKey()
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "description": crypto.createEncryption(secret_key, description),
    "encryptions": [
      crypto.createEncryptionWithPassphrase(secret_key, passphrase),
    ],
  }
  return connection.post("/api/keys", data)


def updateKey(connection: AuthenticatedConnection, key: Dict[str, Any]) -> Dict[str, Any]:
  return connection.put(f"/api/keys/{key['id']}", key)


def deleteKey(connection: AuthenticatedConnection, key_id: str) -> None:
  connection.delete(f"/api/keys/{key_id}")


def getSecrets(connection: AuthenticatedConnection, key_id: str) -> List[Dict[str, Any]]:
  return connection.get(f"/api/keys/{key_id}/secrets")


def createNewSecret(connection: AuthenticatedConnection,
                    key_id: str,
                    secret_key: bytes,
                    name: str,
                    description: str) -> Dict[str, Any]:
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "description": crypto.createEncryption(secret_key, description),
  }
  return connection.post(f"/api/keys/{key_id}/secrets", data)


def updateSecret(connection: AuthenticatedConnection,
                 key_id: str,
                 secret_id: str,
                 secret_key: bytes,
                 name: str,
                 description: str) -> Dict[str, Any]:
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "description": crypto.createEncryption(secret_key, description),
  }
  return connection.put(f"/api/keys/{key_id}/secrets/{secret_id}", data)


def deleteSecret(connection: AuthenticatedConnection, key_id: str, secret_id: str) -> None:
  connection.delete(f"/api/keys/{key_id}/secrets/{secret_id}")


def getSecretProperties(connection: AuthenticatedConnection,
                        key_id: str,
                        secret_id: str) -> List[Dict[str, Any]]:
  return connection.get(f"/api/keys/{key_id}/secrets/{secret_id}/properties")


def createNewSecretProperty(connection: AuthenticatedConnection,
                            key_id: str,
                            secret_id: str,
                            secret_key: bytes,
                            name: str,
                            value: str) -> Dict[str, Any]:
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "value": crypto.createEncryption(secret_key, value),
  }
  return connection.post(f"/api/keys/{key_id}/secrets/{secret_id}/properties", data)


def updateSecretProperty(connection: AuthenticatedConnection,
                         key_id: str,
                         secret_id: str,
                         property_id: str,
                         secret_key: bytes,
                         name: str,
                         value: str) -> Dict[str, Any]:
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "value": crypto.createEncryption(secret_key, value),
  }
  return connection.put(f"/api/keys/{key_id}/secrets/{secret_id}/properties/{property_id}", data)


def deleteSecretProperty(connection: AuthenticatedConnection,
                         key_id: str,
                         secret_id: str,
                         property_id: str) -> None:
  connection.delete(f"/api/keys/{key_id}/secrets/{secret_id}/properties/{property_id}")


def exportData(connection: AuthenticatedConnection, passphrase: str) -> List[Dict[str, Any]]:
  """Decrypts and returns all secrets and properties in the web client's export format."""
  exported = []
  for key in getKeys(connection):
    key_id = key["id"]
    secret_key = crypto.extractSecretKey(key, passphrase)
    if not secret_key:
      continue

    for secret in getSecrets(connection, key_id):
      secret_id = secret["id"]
      sec_name = crypto.decryptSecret(secret["name"], secret_key)
      sec_desc = crypto.decryptSecret(secret["description"], secret_key)
      props = []
      for prop in getSecretProperties(connection, key_id, secret_id):
        props.append({
          "name": crypto.decryptSecret(prop["name"], secret_key),
          "value": crypto.decryptSecret(prop["value"], secret_key),
        })
      exported.append({
        "name": sec_name,
        "description": sec_desc,
        "properties": props,
      })
  return exported


def dumpAllData(connection: AuthenticatedConnection, passphrase: str) -> None:
  """Prints all decrypted keys, secrets, and properties to stdout."""
  keys = getKeys(connection)
  if not keys:
    print("No keys found.")
    return

  unlocked_any = False
  for key in keys:
    key_id = key["id"]
    secret_key = crypto.extractSecretKey(key, passphrase)
    if not secret_key:
      print(f"[Key {key_id} could not be unlocked with provided passphrase]")
      continue

    unlocked_any = True
    name = crypto.decryptSecret(key["name"], secret_key)
    description = crypto.decryptSecret(key["description"], secret_key)
    print("=" * 60)
    print(f"Key: {name}")
    if description:
      print(f"Description: {description}")
    print("=" * 60)

    secrets_list = getSecrets(connection, key_id)
    if not secrets_list:
      print("  (no secrets in this key)")

    for secret in secrets_list:
      secret_id = secret["id"]
      s_name = crypto.decryptSecret(secret["name"], secret_key)
      s_desc = crypto.decryptSecret(secret["description"], secret_key)
      print(f"\n  Secret: {s_name}")
      if s_desc:
        print(f"  Description: {s_desc}")

      properties = getSecretProperties(connection, key_id, secret_id)
      for prop in properties:
        p_name = crypto.decryptSecret(prop["name"], secret_key)
        p_value = crypto.decryptSecret(prop["value"], secret_key)
        print(f"    {p_name}: {p_value}")

  if not unlocked_any:
    print("No keys could be unlocked with the provided passphrase.")


def main() -> None:
  parser = argparse.ArgumentParser(description="Swiss Vault CLI Python Client")
  parser.add_argument("--server", default=DEFAULT_SERVER, help="Server URL (default: http://localhost:8080)")
  parser.add_argument("--cookie", default=None, help="Explicit auth cookie value")
  parser.add_argument("--dev-user", default=None, help="Dev user ID to mint a local test cookie")
  parser.add_argument("--passphrase", default=None, help="Passphrase to unlock vault")
  parser.add_argument("--no-browser", action="store_true", help="Do not automatically open browser on login")

  subparsers = parser.add_subparsers(dest="command")

  subparsers.add_parser("login", help="Log in and cache authentication cookie")
  subparsers.add_parser("logout", help="Clear cached authentication cookie")
  subparsers.add_parser("dump", help="Dump all decrypted vault contents (default)")
  subparsers.add_parser("export", help="Export decrypted contents as JSON")
  subparsers.add_parser("list", help="List secret keys")

  create_key_p = subparsers.add_parser("create-key", help="Create a new key")
  create_key_p.add_argument("name", help="Name of the key")
  create_key_p.add_argument("--desc", default="", help="Description")

  create_sec_p = subparsers.add_parser("create-secret", help="Create a new secret in a key")
  create_sec_p.add_argument("key_id", help="ID of key")
  create_sec_p.add_argument("name", help="Secret name")
  create_sec_p.add_argument("--desc", default="", help="Description")

  args = parser.parse_args()

  if args.command == "logout":
    clear_cached_cookie(args.server)
    print("Logged out. Cached cookie cleared.")
    return

  if args.command == "login":
    login(server_url=args.server, dev_user=args.dev_user, open_browser=not args.no_browser)
    return

  conn = AuthenticatedConnection(server_url=args.server,
                                 auth_cookie=args.cookie,
                                 dev_user=args.dev_user)

  cmd = args.command or "dump"

  if cmd in ("dump", "export"):
    passphrase = args.passphrase
    if not passphrase:
      passphrase = getpass.getpass("Enter vault passphrase: ")
    if cmd == "dump":
      dumpAllData(conn, passphrase)
    else:
      data = exportData(conn, passphrase)
      print(json.dumps(data, indent=2, ensure_ascii=False))
    return

  if cmd == "list":
    keys = getKeys(conn)
    print(f"Total keys: {len(keys)}")
    for k in keys:
      print(f"Key ID: {k['id']} (encryptions: {len(k.get('encryptions', []))})")
    return

  if cmd == "create-key":
    passphrase = args.passphrase
    if not passphrase:
      passphrase = getpass.getpass("Enter passphrase for new key: ")
    new_k = createNewKey(conn, passphrase, args.name, args.desc)
    print(f"Created key {new_k.get('id')}")
    return

  if cmd == "create-secret":
    passphrase = args.passphrase
    if not passphrase:
      passphrase = getpass.getpass("Enter passphrase for key: ")
    k = getKey(conn, args.key_id)
    secret_key = crypto.extractSecretKey(k, passphrase)
    if not secret_key:
      print(f"Could not unlock key {args.key_id} with provided passphrase.")
      sys.exit(1)
    s = createNewSecret(conn, args.key_id, secret_key, args.name, args.desc)
    print(f"Created secret {s.get('id')}")
    return


if __name__ == "__main__":
  main()
