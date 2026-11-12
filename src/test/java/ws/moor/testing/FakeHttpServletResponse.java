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

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FakeHttpServletResponse {

  public int status = 200;
  public Integer errorCode = null;
  public String errorMessage = null;
  public String redirectLocation = null;
  public String contentType = null;
  public final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  public final List<Cookie> cookies = new ArrayList<>();
  public final StringWriter stringWriter = new StringWriter();
  public final PrintWriter printWriter = new PrintWriter(stringWriter);
  public final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
  public final ServletOutputStream servletOutputStream = new ServletOutputStream() {
    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {}

    @Override
    public void write(int b) {
      outputStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      outputStream.write(b, off, len);
    }
  };

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public Integer getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getRedirectLocation() {
    return redirectLocation;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
    setHeader("Content-Type", contentType);
  }

  public void setHeader(String name, String value) {
    List<String> list = new ArrayList<>();
    list.add(value);
    headers.put(name, list);
  }

  public void addHeader(String name, String value) {
    headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
  }

  public String getHeader(String name) {
    List<String> values = headers.get(name);
    return (values != null && !values.isEmpty()) ? values.get(0) : null;
  }

  public List<String> getHeaders(String name) {
    List<String> values = headers.get(name);
    return values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
  }

  public Map<String, List<String>> getHeaders() {
    return Collections.unmodifiableMap(headers);
  }

  public Collection<String> getHeaderNames() {
    return Collections.unmodifiableCollection(headers.keySet());
  }

  public boolean containsHeader(String name) {
    return headers.containsKey(name);
  }

  public void sendError(int sc) {
    this.errorCode = sc;
    this.status = sc;
  }

  public void sendError(int sc, String msg) {
    this.errorCode = sc;
    this.status = sc;
    this.errorMessage = msg;
  }

  public void sendRedirect(String location) {
    this.redirectLocation = location;
    this.status = HttpServletResponse.SC_MOVED_TEMPORARILY;
  }

  public void addCookie(Cookie cookie) {
    this.cookies.add(cookie);
  }

  public List<Cookie> getCookies() {
    return Collections.unmodifiableList(cookies);
  }

  public PrintWriter getWriter() {
    return printWriter;
  }

  public ServletOutputStream getOutputStream() {
    return servletOutputStream;
  }

  public byte[] getOutputStreamBytes() {
    return outputStream.toByteArray();
  }

  public void flushBuffer() {
    printWriter.flush();
  }

  public String getBody() {
    printWriter.flush();
    if (outputStream.size() > 0) {
      return outputStream.toString(StandardCharsets.UTF_8);
    }
    return stringWriter.toString();
  }

  public String getOutput() {
    return getBody();
  }

  public HttpServletResponse asResponse() {
    return (HttpServletResponse) Proxy.newProxyInstance(
        HttpServletResponse.class.getClassLoader(),
        new Class<?>[]{HttpServletResponse.class},
        new ResponseInvocationHandler());
  }

  public HttpServletResponse asHttpServletResponse() {
    return asResponse();
  }

  private class ResponseInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      String name = method.getName();
      if ("equals".equals(name)) {
        return proxy == args[0];
      } else if ("hashCode".equals(name)) {
        return System.identityHashCode(proxy);
      } else if ("toString".equals(name)) {
        return "FakeHttpServletResponse[status=" + status + ", errorCode=" + errorCode + "]";
      } else if ("setStatus".equals(name)) {
        setStatus((Integer) args[0]);
        return null;
      } else if ("getStatus".equals(name)) {
        return getStatus();
      } else if ("sendError".equals(name)) {
        if (args.length > 1 && args[1] != null) {
          sendError((Integer) args[0], (String) args[1]);
        } else {
          sendError((Integer) args[0]);
        }
        return null;
      } else if ("sendRedirect".equals(name)) {
        sendRedirect((String) args[0]);
        return null;
      } else if ("setHeader".equals(name)) {
        setHeader((String) args[0], (String) args[1]);
        return null;
      } else if ("addHeader".equals(name)) {
        addHeader((String) args[0], (String) args[1]);
        return null;
      } else if ("getHeader".equals(name)) {
        return getHeader((String) args[0]);
      } else if ("getHeaders".equals(name)) {
        return getHeaders((String) args[0]);
      } else if ("getHeaderNames".equals(name)) {
        return getHeaderNames();
      } else if ("containsHeader".equals(name)) {
        return containsHeader((String) args[0]);
      } else if ("setContentType".equals(name)) {
        setContentType((String) args[0]);
        return null;
      } else if ("getContentType".equals(name)) {
        return getContentType();
      } else if ("getWriter".equals(name)) {
        return getWriter();
      } else if ("getOutputStream".equals(name)) {
        return getOutputStream();
      } else if ("flushBuffer".equals(name)) {
        flushBuffer();
        return null;
      } else if ("addCookie".equals(name)) {
        addCookie((Cookie) args[0]);
        return null;
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
