/*
 * Copyright 2012 Patrick Moor <patrick@moor.ws>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ws.moor.swissvault.rest;


import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import ws.moor.swissvault.util.Obfuscator;

public class ParameterExtractor {

  private final Obfuscator obfuscator;
  private final ImmutableMap<String, String> parameters;

  @Inject
  ParameterExtractor(Obfuscator obfuscator, ImmutableMap<String, String> parameters) {
    this.obfuscator = obfuscator;
    this.parameters = parameters;
  }

  public long getKeyId() {
    return obfuscator.unobfuscateLong(parameters.get("keyId"));
  }

  public long getSecretId() {
    return obfuscator.unobfuscateLong(parameters.get("secretId"));
  }

  public long getPropertyId() {
    return obfuscator.unobfuscateLong(parameters.get("propertyId"));
  }
}
