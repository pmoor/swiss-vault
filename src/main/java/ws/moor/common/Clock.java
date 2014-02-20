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
package ws.moor.common;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.Instant;

public interface Clock {
  Instant now();

  static class SystemClock implements Clock {
    @Inject private SystemClock() {}
    @Override public Instant now() {
      return new Instant(System.currentTimeMillis());
    }
  }

  static class Module extends AbstractModule {
    @Override protected void configure() {
      bind(Clock.class).to(SystemClock.class).in(Singleton.class);
    }
  }
}
