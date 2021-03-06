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
package ws.moor.swissvault.auth;

import com.google.appengine.api.urlfetch.*;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import ws.moor.swissvault.config.Config;
import ws.moor.swissvault.util.UriBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class AuthHelper {

  private static final Logger logger = Logger.getLogger(AuthHelper.class.getName());

  private static final URI PROFILE_SCOPE = URI.create("https://www.googleapis.com/auth/userinfo.profile");
  private static final Set<URI> SCOPES = ImmutableSet.of(PROFILE_SCOPE);

  private final URLFetchService urlFetchService;
  private final JsonParser jsonParser;
  private final UriBuilder uriBuilder;
  private final String clientId;
  private final String clientSecret;

  @Inject
  AuthHelper(URLFetchService urlFetchService, JsonParser jsonParser, UriBuilder uriBuilder,
             @Config("oauth.client_secret") String clientSecret, @Config("oauth.client_id") String clientId) {
    this.urlFetchService = urlFetchService;
    this.jsonParser = jsonParser;
    this.uriBuilder = uriBuilder;
    this.clientSecret = clientSecret;
    this.clientId = clientId;
  }

  public URI createRedirectUri() {
    Map<String, String> parameters = Maps.newHashMap();
    parameters.put("response_type", "code");
    parameters.put("client_id", clientId);
    parameters.put("redirect_uri", uriBuilder.forPath(OAuthCallbackServlet.PATH).toString());
    parameters.put("scope", Joiner.on(" ").join(SCOPES));
    parameters.put("access_type", "online");
    parameters.put("state", uriBuilder.forPath("/").toString());

    try {
      return new URI("https", "accounts.google.com", "/o/oauth2/auth", buildKeyValueString(parameters, false), null);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public UserId determineUserId(String code) throws IOException {
    Map<String, String> parameters = Maps.newHashMap();
    parameters.put("code", code);
    parameters.put("client_id", clientId);
    parameters.put("client_secret", clientSecret);
    parameters.put("redirect_uri", uriBuilder.forPath(OAuthCallbackServlet.PATH).toString());
    parameters.put("grant_type", "authorization_code");

    HTTPRequest fetchRequest = new HTTPRequest(new URL("https://accounts.google.com/o/oauth2/token"), HTTPMethod.POST);
    fetchRequest.setPayload(buildKeyValueString(parameters, true).getBytes());
    HTTPResponse response = urlFetchService.fetch(fetchRequest);
    JsonObject object = jsonParser.parse(new String(response.getContent())).getAsJsonObject();
    String access_token = object.get("access_token").getAsString();

    HTTPRequest secondRequest = new HTTPRequest(new URL("https://www.googleapis.com/oauth2/v1/userinfo"));
    secondRequest.addHeader(new HTTPHeader("Authorization", String.format("Bearer %s", access_token)));
    response = urlFetchService.fetch(secondRequest);
    object = jsonParser.parse(new String(response.getContent())).getAsJsonObject();

    return UserId.fromString(object.get("id").getAsString());
  }

  private String buildKeyValueString(Map<String, String> parameters, final boolean encode) {
    return Joiner.on('&').join(Iterables.transform(parameters.entrySet(),
        new Function<Map.Entry<String, String>, Object>() {
          @Override public String apply(Map.Entry<String, String> parameter) {
            try {
              String key = encode ? URLEncoder.encode(parameter.getKey(), "UTF-8") : parameter.getKey();
              String value = encode ? URLEncoder.encode(parameter.getValue(), "UTF-8") : parameter.getValue();
              return String.format("%s=%s", key, value);
            } catch (UnsupportedEncodingException e) {
              throw Throwables.propagate(e);
            }
          }
        }));
  }
}
