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

import com.google.common.collect.ImmutableMap;

import java.util.List;

class RestCallContext {

  final Verb verb;
  final Class<? extends RestAction> actionClass;
  final ImmutableMap<String, String> parameters;
  final List<Class<? extends RestFilter>> filterClasses;

  RestCallContext(Verb verb, Class<? extends RestAction> actionClass, List<Class<? extends RestFilter>> filterClasses, ImmutableMap<String, String> parameters) {
    this.verb = verb;
    this.actionClass = actionClass;
    this.filterClasses = filterClasses;
    this.parameters = parameters;
  }
}
