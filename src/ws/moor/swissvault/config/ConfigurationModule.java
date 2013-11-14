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
package ws.moor.swissvault.config;

import com.google.appengine.api.utils.SystemProperty;
import com.google.common.io.BaseEncoding;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationModule extends AbstractModule {
  
  @Override
  protected void configure() { }
  
  @Provides
  @Singleton
  private Properties loadProperties() throws IOException {
    String resourceName = "production.properties";
    if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Development) {
      resourceName = "development.properties";
    }
    InputStream resource = ConfigurationModule.class.getResourceAsStream(resourceName);
    Properties properties = new Properties();
    properties.load(resource);
    return properties;
  }

  @Provides
  @Config("secure_only")
  private boolean provideSecureOnly(Properties properties) {
    return Boolean.parseBoolean(properties.getProperty("secure_only"));
  }

  @Provides
  @Config("obfuscator_secret")
  private SecretKeySpec provideObfuscatorSecret(Properties properties) {
    return new SecretKeySpec(parseHexKey(properties.getProperty("obfuscator.secret"), 8), "Blowfish");
  }

  @Provides
  @Config("auth.hmac_key")
  private SecretKeySpec provideAuthHmacKey(Properties properties) {
    return new SecretKeySpec(parseHexKey(properties.getProperty("auth.hmac_key"), 32), "HmacSHA256");
  }

  @Provides
  @Config("scheme")
  private String provideScheme(Properties properties) {
    return (String) properties.get("scheme");
  }

  @Provides
  @Config("hostname")
  private String provideHostname(Properties properties) {
    return (String) properties.get("hostname");
  }

  @Provides
  @Config("oauth.client_secret")
  private String provideClientSecret(Properties properties) {
    return (String) properties.get("oauth.client_secret");
  }

  @Provides
  @Config("oauth.client_id")
  private String provideClientId(Properties properties) {
    return (String) properties.get("oauth.client_id");
  }

  private byte[] parseHexKey(String hexValue, int expectedLength) {
    byte[] decoded = BaseEncoding.base16().decode(hexValue.toUpperCase());
    if (decoded.length != expectedLength) {
      throw new IllegalArgumentException("expected key of length "+ expectedLength + ", but got " + decoded.length);
    }
    return decoded;
  }
}
