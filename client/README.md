# Swiss Vault Python Client

An independent Python 3 client for Swiss Vault to inspect, dump, and manage secrets without running the browser JavaScript client.

## Requirements

- Python 3.8+
- `cryptography` package (`pip install cryptography`)

## Usage

### 1. Authentication / Login

To log in:

```bash
python3 client/client.py login
```

This opens your browser to `/cli-login` (or prints the URL if in a headless/remote terminal). Once you log in, copy the authentication token displayed in the text box and paste it into your terminal.

The session cookie is saved to `~/.config/swiss-vault/cookie_<host>` (mode 0600) and automatically reused.

For local development against the dev server without Google OAuth:
```bash
python3 client/client.py --dev-user test-user dump
```

Or provide an explicit auth cookie:
```bash
python3 client/client.py --cookie "<auth-cookie-value>" dump
# or
export SWISSVAULT_COOKIE="<auth-cookie-value>"
```

To log out and delete the cached cookie:
```bash
python3 client/client.py logout
```

### 2. Viewing and Exporting Secrets

Dump all decrypted keys, secrets, and properties:
```bash
python3 client/client.py dump
```

Export decrypted contents in JSON format (identical to the web client export):
```bash
python3 client/client.py export > secrets.json
```

List keys without decrypting:
```bash
python3 client/client.py list
```

### 3. Server Selection

By default, the client connects to `http://localhost:8080`. To connect to a different instance:
```bash
python3 client/client.py --server https://swiss-vault.appspot.com dump
# or
export SWISSVAULT_SERVER="https://swiss-vault.appspot.com"
```

## Running Tests

Unit tests for cryptography:
```bash
python3 client/m2tests.py
```

Integration tests against a running dev server:
```bash
python3 client/test_client.py http://localhost:8181
```
