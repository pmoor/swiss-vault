#!/usr/bin/python

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

import getpass
import random

import simplejson
import httplib
import sys
import crypto
import os
import urllib
import urllib2
import cookielib

def GetNewCookie(user, password):
  cookiejar = cookielib.LWPCookieJar()
  opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cookiejar))
  urllib2.install_opener(opener)

  auth_uri = 'https://www.google.com/accounts/ClientLogin'
  authreq_data = urllib.urlencode({
      "Email": user,
      "Passwd": password,
      "service": "ah",
      "source":  "javavault",
      "accountType": "HOSTED_OR_GOOGLE" })
  auth_req = urllib2.Request(auth_uri, data=authreq_data)
  auth_resp = urllib2.urlopen(auth_req)
  auth_resp_body = auth_resp.read()
  print auth_resp_body
  auth_resp_dict = dict(
    x.split("=") for x in auth_resp_body.split("\n") if x)
  authtoken = auth_resp_dict["Auth"]

  serv_args = {
    "continue": "https://javavault.appspot.com/",
    "auth": authtoken,
  }
  full_serv_uri = "https://javavault.appspot.com/_ah/login?%s" % (urllib.urlencode(serv_args))

  serv_req = urllib2.Request(full_serv_uri)
  serv_resp = urllib2.urlopen(serv_req)
  serv_resp_body = serv_resp.read()

  print serv_resp_body

  cookies = [c for c in cookiejar]
  assert len(cookies) == 1

  return cookies[0]


class AuthenticatedConnection(object):

  def __init__(self):
    #self._connection = httplib.HTTPConnection("localhost", 8080)
    #self._headers = { "Cookie": "dev_appserver_login=test@example.com:false:18580476422013912411" }
    self._connection = httplib.HTTPSConnection("javavault.appspot.com")
    self._headers = { "Cookie": "SACSID=AJKiYcFR-CP-qhPW6_QgeQQgIVNkyw9-ulsdPIn7r5ITTaRZoMTIJT-rCsoVopu1kEbb4lGSI6gcBhr"
                      "PZoPXQl4whlPG70W-Ovs5HTc91XxAFdm4VwCX665OTyTj38nkXj4m2GN6zwcWxTx0J0SLUzq9PUTY5Na9_o4-6P6p00_Rke1"
                      "CI9Hcvyu0K44vyyKjdTrlg7sD55ALed1jqMwFJs0vNbfYi02TeyZL5rOywCZUmJNlznZh2WlHfAkf8WYswK78SVJ4qtluBIN"
                      "6mMUolX0TuYFgEcbrp96bpLAs13yXYIEXZfSsexQxHjSoB9M_-PjtGjjBmX-nUmFUM6-VNZq3yyY6d7m7ZI96N1ZU2LTV-eb"
                      "83rmRDEQoD0vAfX3HIh3-4dwQ-dRiflBSEC51PwE0YNe77J5gs1MrfPYFlVED5_La1VmQWMZbCuSk9elyFiBaG-EWGMR8JKt"
                      "NNrWPgFVbsMxUfkvUXaWAROLnbIEPd-VjTzKQCb7kGqEZCD6uvNiE8yd7hKveT0WL38y7uyKjI519jmowc1Y205Pt55bj0YQ"
                      "RUmD03NnPj6CwuDZ3MzQhDmVHoXDLCYPQgxZ-CcvdRdMwTinvtTa2ioXaq1UlJzJxt140JL-9ux2aQfXqWwOMpOtoqyEa9AO"
                      "UILtviJsAp50TqyhrmA" }

  def get(self, path):
    self._connection.request("GET", path, headers=self._headers)
    response = self._connection.getresponse()
    assert response.status == 200, "%d %s" % (response.status, response.reason)
    return simplejson.loads(response.read())

  def delete(self, path):
    self._connection.request("DELETE", path, headers=self._headers)
    response = self._connection.getresponse()
    assert response.status == 200, "%d %s" % (response.status, response.reason)

  def post(self, path, json):
    self._connection.request("POST", path, body=simplejson.dumps(json), headers=self._headers)
    response = self._connection.getresponse()
    assert response.status == 200, "%d %s" % (response.status, response.reason)
    return simplejson.loads(response.read())

  def close(self):
    self._connection.close()


def getKeys(connection):
  return connection.get("/api/keys")

def deleteKey(connection, key_id):
  connection.delete("/api/keys/%s" % key_id)

def getKey(connection, key_id):
  return connection.get("/api/keys/%s" % key_id)

def getSecrets(connection, key_id):
  return connection.get("/api/keys/%s/secrets" % key_id)

def getSecretProperties(connection, key_id, secret_id):
  return connection.get("/api/keys/%s/secrets/%s/properties" % (key_id, secret_id))

def createNewKey(connection, passphrase, name, description):
  secret_key = crypto.newRandomSecretKey()
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "description": crypto.createEncryption(secret_key, description),
    "encryptions": [
      crypto.createEncryptionWithPassphrase(secret_key, passphrase),
    ],
  }
  del secret_key
  return connection.post("/api/keys", data)

def createNewSecret(connection, key_id, secret_key, name, description):
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "description": crypto.createEncryption(secret_key, description),
  }
  return connection.post("/api/keys/%s/secrets" % key_id, data)

def createNewSecretProperty(connection, key_id, secret_id, secret_key, name, value):
  data = {
    "name": crypto.createEncryption(secret_key, name),
    "value": crypto.createEncryption(secret_key, value),
  }
  return connection.post("/api/keys/%s/secrets/%s/properties" % (key_id, secret_id), data)

def updateKey(connection, key):
  return connection.post("/api/keys/%s" % key["id"], key)

def dumpAllData(connection, passphrase):
  for key in getKeys(connection):
    key_id = key["id"]
    secret_key = crypto.extractSecretKey(key, passphrase)
    if not secret_key:
      print "key %s could not be decrypted" % key_id
      continue

    print "---"
    print "name: %s" % crypto.decryptSecret(key["name"], secret_key)
    print "description: %s" % crypto.decryptSecret(key["description"], secret_key)
    print "---"
    for secret in getSecrets(connection, key_id):
      secret_id = secret["id"]
      name = crypto.decryptSecret(secret["name"], secret_key)
      description = crypto.decryptSecret(secret["description"], secret_key)
      print "  name: \"%s\"\n  description: \"%s\"" % (name, description)

      for property in connection.get("/api/keys/%s/secrets/%s/properties" % (key_id, secret_id)):
        name = crypto.decryptSecret(property["name"], secret_key)
        value = crypto.decryptSecret(property["value"], secret_key)
        print "    name: \"%s\"\n    value: \"%s\"" % (name, value)


def prettyprint(json):
  print simplejson.dumps(json, sort_keys=True, indent=" ")


#cookie = GetNewCookie("patrick@moor.ws", "")
#print "name: " + cookie.name
#print "value: " + cookie.value
#sys.exit(0)

connection = AuthenticatedConnection()
passphrase = getpass.getpass()
dumpAllData(connection, passphrase)

if False:
  new_key = createNewKey(connection, "the quick brown fox jumps over the lazy dog", "The Name", "The Description")
  #prettyprint(new_key)

  secret_key = crypto.extractSecretKey(new_key, "the quick brown fox jumps over the lazy dog")

  secrets = set()
  for i in range(10):
    if not secrets or random.randint(0, len(secrets)) == 0:
      new_secret = createNewSecret(connection, new_key["id"], secret_key, "Secret From Iteration %d" % i, "%d %d" % (i, i*i))
      secrets.add(new_secret["id"])
    else:
      id = random.choice(list(secrets))
      createNewSecretProperty(connection, new_key["id"], id, secret_key, "Property From Iteration %d" % i, "%d %d %d" % (i, i*i, i*i*i))

  dumpAllData(connection, "the quick brown fox jumps over the lazy dog")

  #deleteKey(connection, new_key["id"])
  print "deleted"

  connection.close()