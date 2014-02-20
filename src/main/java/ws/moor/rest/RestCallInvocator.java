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
package ws.moor.rest;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.servlet.RequestScoped;
import ws.moor.rest.annotations.Execute;
import ws.moor.rest.annotations.PostFilter;
import ws.moor.rest.annotations.PreFilter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@RequestScoped
class RestCallInvocator {

  private final Injector injector;
  private final HttpServletResponse response;
  private final List<Class<? extends RestFilter>> filters;
  private final Class<? extends RestAction> actionClass;
  private final Provider<RestAction> actionProvider;

  @Inject
  RestCallInvocator(
      Class<? extends RestAction> actionClass,
      Provider<RestAction> actionProvider,
      Injector injector,
      HttpServletResponse response,
      List<Class<? extends RestFilter>> filters) {
    this.actionClass = actionClass;
    this.actionProvider = actionProvider;
    this.injector = injector;
    this.response = response;
    this.filters = filters;
  }

  public void invoke() {
    Method method = findAnnotatedMethod(actionClass, Execute.class);
    Preconditions.checkNotNull(method);

    try {
      for (Class<? extends RestFilter> filterClass : filters) {
        RestFilter filter = injector.getInstance(filterClass);
        Method preFilterMethod = findAnnotatedMethod(filterClass, PreFilter.class);
        if (preFilterMethod != null) {
          new InjectedMethodInvocator(preFilterMethod).invoke(injector, filter);
        }
      }
    
      Object result = new InjectedMethodInvocator(method).invoke(injector, actionProvider.get());

      for (Class<? extends RestFilter> filterClass : filters) {
        RestFilter filter = injector.getInstance(filterClass);
        Method postFilterMethod = findAnnotatedMethod(filterClass, PostFilter.class);
        if (postFilterMethod != null) {
          new InjectedMethodInvocator(postFilterMethod).invoke(injector, filter);
        }
      }
      
      if (result == null) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else if (result instanceof JsonArray) {
        JsonArray array = (JsonArray) result;
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(array.toString().getBytes());
      } else if (result instanceof JsonObject) {
        JsonObject object = (JsonObject) result;
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().write(object.toString().getBytes());
      } else {
        throw new RuntimeException("unknown response type: " + result.getClass());
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Method findAnnotatedMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {
    for (Method method : clazz.getDeclaredMethods()) {
      Annotation annotation = method.getAnnotation(annotationClass);
      if (annotation != null) {
        method.setAccessible(true);
        return method;
      }
    }
    return null;
  }
}
