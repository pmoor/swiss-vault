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
package ws.moor.rest;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ws.moor.rest.RestServletModule.ActionMatcher;
import ws.moor.rest.RestServletModule.FilterMatcher;
import ws.moor.rest.RestServletModule.RestServlet;
import ws.moor.rest.annotations.Delete;
import ws.moor.rest.annotations.FilterAnnotation;
import ws.moor.rest.annotations.Get;
import ws.moor.rest.annotations.Post;
import ws.moor.rest.annotations.Put;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ws.moor.testing.FakeHttpServletRequest;
import ws.moor.testing.FakeHttpServletResponse;

import static com.google.common.truth.Truth.assertThat;

public class RestServletModuleTest {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @FilterAnnotation
  private @interface AnnotationA {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @FilterAnnotation
  private @interface AnnotationB {}

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  private @interface NonFilterAnnotation {}

  private static class DummyFilter implements RestFilter {}

  @AnnotationA
  private static class FilterWithAnnotationA implements RestFilter {}

  @AnnotationA
  @AnnotationB
  @NonFilterAnnotation
  private static class FilterWithABAndNonFilterAnnotation implements RestFilter {}

  @Get(path = "/unannotated")
  private static class UnannotatedAction implements RestAction {}

  @AnnotationA
  @Get(path = "/annotated-a")
  private static class AnnotatedWithAAction implements RestAction {}

  @AnnotationB
  @Get(path = "/annotated-b")
  private static class AnnotatedWithBAction implements RestAction {}

  @AnnotationA
  @AnnotationB
  @Get(path = "/annotated-ab")
  private static class AnnotatedWithABAction implements RestAction {}

  @Post(path = "/post")
  private static class PostAction implements RestAction {}

  @Put(path = "/put")
  private static class PutAction implements RestAction {}

  @Delete(path = "/delete")
  private static class DeleteAction implements RestAction {}

  @Test
  public void testFilterMatcherWithNoAnnotationsAppliesToAllActions() {
    FilterMatcher matcher = new FilterMatcher(DummyFilter.class, ImmutableList.of());
    assertThat(matcher.applies(UnannotatedAction.class)).isTrue();
    assertThat(matcher.applies(AnnotatedWithAAction.class)).isTrue();
    assertThat(matcher.applies(AnnotatedWithBAction.class)).isTrue();
    assertThat(matcher.applies(AnnotatedWithABAction.class)).isTrue();
  }

  @Test
  public void testFilterMatcherWithSingleAnnotation() {
    FilterMatcher matcher = new FilterMatcher(DummyFilter.class, ImmutableList.of(AnnotationA.class));
    assertThat(matcher.applies(UnannotatedAction.class)).isFalse();
    assertThat(matcher.applies(AnnotatedWithAAction.class)).isTrue();
    assertThat(matcher.applies(AnnotatedWithBAction.class)).isFalse();
    assertThat(matcher.applies(AnnotatedWithABAction.class)).isTrue();
  }

  @Test
  public void testFilterMatcherWithMultipleAnnotationsRequiresAll() {
    FilterMatcher matcher = new FilterMatcher(
        DummyFilter.class, ImmutableList.of(AnnotationA.class, AnnotationB.class));
    assertThat(matcher.applies(UnannotatedAction.class)).isFalse();
    assertThat(matcher.applies(AnnotatedWithAAction.class)).isFalse();
    assertThat(matcher.applies(AnnotatedWithBAction.class)).isFalse();
    assertThat(matcher.applies(AnnotatedWithABAction.class)).isTrue();
  }

  @Test
  public void testRestServletModuleFilterMatcherExtraction() {
    TestModule module = new TestModule("/api/*");

    FilterMatcher globalMatcher = module.getFilterMatcher(DummyFilter.class);
    assertThat(globalMatcher.applies(UnannotatedAction.class)).isTrue();
    assertThat(globalMatcher.applies(AnnotatedWithAAction.class)).isTrue();

    FilterMatcher aMatcher = module.getFilterMatcher(FilterWithAnnotationA.class);
    assertThat(aMatcher.applies(UnannotatedAction.class)).isFalse();
    assertThat(aMatcher.applies(AnnotatedWithAAction.class)).isTrue();
    assertThat(aMatcher.applies(AnnotatedWithBAction.class)).isFalse();
    assertThat(aMatcher.applies(AnnotatedWithABAction.class)).isTrue();

    FilterMatcher abMatcher = module.getFilterMatcher(FilterWithABAndNonFilterAnnotation.class);
    assertThat(abMatcher.applies(UnannotatedAction.class)).isFalse();
    assertThat(abMatcher.applies(AnnotatedWithAAction.class)).isFalse();
    assertThat(abMatcher.applies(AnnotatedWithBAction.class)).isFalse();
    assertThat(abMatcher.applies(AnnotatedWithABAction.class)).isTrue();
  }

  @Test
  public void testActionMatcherWithNullPathReturnsNull() {
    ActionMatcher matcher = new ActionMatcher(Verb.GET, "/unannotated", UnannotatedAction.class);
    assertThat(matcher.match(Verb.GET, null)).isNull();
    assertThat(matcher.match(Verb.POST, null)).isNull();
  }

  @Test
  public void testRestServletDoGetWithNullPathInfoReturns404() throws Exception {
    ActionMatcher matcher = new ActionMatcher(Verb.GET, "/unannotated", UnannotatedAction.class);
    RestServlet servlet = new RestServlet(ImmutableList.of(matcher), () -> {
      throw new AssertionError("invocator should not be called when pathInfo is null");
    });

    FakeHttpServletRequest request = new FakeHttpServletRequest("GET", null);
    FakeHttpServletResponse response = new FakeHttpServletResponse();

    servlet.doGet(request.asHttpServletRequest(), response.asHttpServletResponse());

    assertThat(response.errorCode).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testRestServletAllVerbsWithNullPathInfoReturns404() throws Exception {
    ActionMatcher getMatcher = new ActionMatcher(Verb.GET, "/unannotated", UnannotatedAction.class);
    ActionMatcher postMatcher = new ActionMatcher(Verb.POST, "/post", PostAction.class);
    ActionMatcher putMatcher = new ActionMatcher(Verb.PUT, "/put", PutAction.class);
    ActionMatcher deleteMatcher = new ActionMatcher(Verb.DELETE, "/delete", DeleteAction.class);

    RestServlet servlet = new RestServlet(
        ImmutableList.of(getMatcher, postMatcher, putMatcher, deleteMatcher),
        () -> {
          throw new AssertionError("invocator should not be called when pathInfo is null");
        });

    FakeHttpServletRequest request = new FakeHttpServletRequest("GET", null);

    FakeHttpServletResponse getResponse = new FakeHttpServletResponse();
    servlet.doGet(request.asHttpServletRequest(), getResponse.asHttpServletResponse());
    assertThat(getResponse.errorCode).isEqualTo(HttpServletResponse.SC_NOT_FOUND);

    FakeHttpServletResponse postResponse = new FakeHttpServletResponse();
    servlet.doPost(request.asHttpServletRequest(), postResponse.asHttpServletResponse());
    assertThat(postResponse.errorCode).isEqualTo(HttpServletResponse.SC_NOT_FOUND);

    FakeHttpServletResponse putResponse = new FakeHttpServletResponse();
    servlet.doPut(request.asHttpServletRequest(), putResponse.asHttpServletResponse());
    assertThat(putResponse.errorCode).isEqualTo(HttpServletResponse.SC_NOT_FOUND);

    FakeHttpServletResponse deleteResponse = new FakeHttpServletResponse();
    servlet.doDelete(request.asHttpServletRequest(), deleteResponse.asHttpServletResponse());
    assertThat(deleteResponse.errorCode).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
  }

  @Test
  public void testRestServletModuleGetActionMatcher() {
    TestModule module = new TestModule("/api/*");
    ActionMatcher matcher = module.getActionMatcher(UnannotatedAction.class);
    assertThat(matcher).isNotNull();
    assertThat(matcher.match(Verb.GET, null)).isNull();
    assertThat(matcher.match(Verb.GET, "/unannotated")).isNotNull();
  }

  private static class TestModule extends RestServletModule {
    TestModule(String pattern) {
      super(pattern);
    }

    @Override
    protected void configureActions() {}
  }
}
