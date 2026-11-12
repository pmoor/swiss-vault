/*
 * Copyright 2026 Patrick Moor <patrick@moor.ws>
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
package ws.moor.testing;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FakeHttpServletRequest {

  private String method = "GET";
  private String pathInfo = null;
  private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  private final Map<String, List<String>> parameters = new LinkedHashMap<>();
  private final Map<String, Object> attributes = new HashMap<>();
  private Cookie[] cookies = null;

  public FakeHttpServletRequest() {}

  public FakeHttpServletRequest(String method, String pathInfo) {
    this.method = method;
    this.pathInfo = pathInfo;
  }

  public FakeHttpServletRequest withMethod(String method) {
    this.method = method;
    return this;
  }

  public FakeHttpServletRequest withPathInfo(String pathInfo) {
    this.pathInfo = pathInfo;
    return this;
  }

  public FakeHttpServletRequest withHeader(String name, String value) {
    headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    return this;
  }

  public FakeHttpServletRequest withHeaders(Map<String, String> headers) {
    if (headers != null) {
      headers.forEach(this::withHeader);
    }
    return this;
  }

  public FakeHttpServletRequest withParameter(String name, String value) {
    parameters.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    return this;
  }

  public FakeHttpServletRequest withParameters(Map<String, String> parameters) {
    if (parameters != null) {
      parameters.forEach(this::withParameter);
    }
    return this;
  }

  public FakeHttpServletRequest withCookies(Cookie... cookies) {
    this.cookies = cookies;
    return this;
  }

  public FakeHttpServletRequest withCookie(Cookie cookie) {
    if (this.cookies == null) {
      this.cookies = new Cookie[]{cookie};
    } else {
      Cookie[] copy = Arrays.copyOf(this.cookies, this.cookies.length + 1);
      copy[copy.length - 1] = cookie;
      this.cookies = copy;
    }
    return this;
  }

  public FakeHttpServletRequest withCookie(String name, String value) {
    return withCookie(new Cookie(name, value));
  }

  public FakeHttpServletRequest withAttribute(String name, Object value) {
    this.attributes.put(name, value);
    return this;
  }

  public String getMethod() {
    return method;
  }

  public String getPathInfo() {
    return pathInfo;
  }

  public String getHeader(String name) {
    List<String> values = headers.get(name);
    return (values != null && !values.isEmpty()) ? values.get(0) : null;
  }

  public List<String> getHeaders(String name) {
    List<String> values = headers.get(name);
    return values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
  }

  public String getParameter(String name) {
    List<String> values = parameters.get(name);
    return (values != null && !values.isEmpty()) ? values.get(0) : null;
  }

  public Object getAttribute(String name) {
    return attributes.get(name);
  }

  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }

  public void removeAttribute(String name) {
    attributes.remove(name);
  }

  public Cookie[] getCookies() {
    return cookies;
  }

  public HttpServletRequest asRequest() {
    return (HttpServletRequest) Proxy.newProxyInstance(
        HttpServletRequest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class},
        new RequestInvocationHandler());
  }

  public HttpServletRequest asHttpServletRequest() {
    return asRequest();
  }

  private class RequestInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      String name = method.getName();
      if ("equals".equals(name)) {
        return proxy == args[0];
      } else if ("hashCode".equals(name)) {
        return System.identityHashCode(proxy);
      } else if ("toString".equals(name)) {
        return "FakeHttpServletRequest[" + FakeHttpServletRequest.this.method + " " + FakeHttpServletRequest.this.pathInfo + "]";
      } else if ("getMethod".equals(name)) {
        return FakeHttpServletRequest.this.method;
      } else if ("getPathInfo".equals(name)) {
        return FakeHttpServletRequest.this.pathInfo;
      } else if ("getHeader".equals(name)) {
        return getHeader((String) args[0]);
      } else if ("getHeaders".equals(name)) {
        return Collections.enumeration(getHeaders((String) args[0]));
      } else if ("getHeaderNames".equals(name)) {
        return Collections.enumeration(headers.keySet());
      } else if ("getParameter".equals(name)) {
        return getParameter((String) args[0]);
      } else if ("getParameterValues".equals(name)) {
        List<String> values = parameters.get((String) args[0]);
        return values != null ? values.toArray(new String[0]) : null;
      } else if ("getParameterNames".equals(name)) {
        return Collections.enumeration(parameters.keySet());
      } else if ("getParameterMap".equals(name)) {
        Map<String, String[]> map = new LinkedHashMap<>();
        parameters.forEach((k, v) -> map.put(k, v.toArray(new String[0])));
        return Collections.unmodifiableMap(map);
      } else if ("getCookies".equals(name)) {
        return cookies;
      } else if ("getAttribute".equals(name)) {
        return getAttribute((String) args[0]);
      } else if ("setAttribute".equals(name)) {
        setAttribute((String) args[0], args[1]);
        return null;
      } else if ("removeAttribute".equals(name)) {
        removeAttribute((String) args[0]);
        return null;
      } else if ("getAttributeNames".equals(name)) {
        return Collections.enumeration(attributes.keySet());
      } else if ("getContentType".equals(name)) {
        return getHeader("Content-Type");
      }

      Class<?> returnType = method.getReturnType();
      if (returnType.equals(boolean.class)) {
        return false;
      } else if (returnType.equals(int.class)) {
        return 0;
      } else if (returnType.equals(long.class)) {
        return 0L;
      }
      return null;
    }
  }
}
