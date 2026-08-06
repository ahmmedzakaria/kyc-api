package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiRouteMatcherTest {
    private final ApiRouteMatcher matcher = new ApiRouteMatcher();

    @Test
    void exactRouteWinsBeforeAnyTemplate() {
        SysPrivApiRegistry exact = route("exact", "/api/person/me", -100);
        SysPrivApiRegistry template = route("template", "/api/person/{id}", 100);

        assertThat(matcher.resolve(List.of(template, exact), "/api/person/me"))
                .contains(exact);
    }

    @Test
    void mostSpecificTemplateWinsBeforeExplicitPriority() {
        SysPrivApiRegistry specific = route("specific", "/api/person/{id}/documents/{documentId}", -100);
        SysPrivApiRegistry broad = route("broad", "/api/person/**", 100);

        assertThat(matcher.resolve(List.of(broad, specific), "/api/person/7/documents/9"))
                .contains(specific);
    }

    @Test
    void priorityBreaksEquallySpecificTemplateTie() {
        SysPrivApiRegistry lower = route("lower", "/api/person/{id}", 5);
        SysPrivApiRegistry higher = route("higher", "/api/person/{personId}", 10);

        assertThat(matcher.resolve(List.of(lower, higher), "/api/person/7"))
                .contains(higher);
    }

    @Test
    void unresolvedTieFailsClosedRegardlessOfRepositoryOrder() {
        SysPrivApiRegistry left = route("left", "/api/person/{id}", 10);
        SysPrivApiRegistry right = route("right", "/api/person/{personId}", 10);

        assertThatThrownBy(() -> matcher.resolve(List.of(left, right), "/api/person/7"))
                .isInstanceOf(ApiRouteAmbiguityException.class)
                .hasMessageContaining("left").hasMessageContaining("right");
        assertThat(matcher.hasUnresolvedOverlap(left, right)).isTrue();
    }

    @Test
    void nonMatchingRouteProducesNoResolution() {
        assertThat(matcher.resolve(List.of(route("person", "/api/person/{id}", 0)), "/api/business/7"))
                .isEmpty();
    }

    private SysPrivApiRegistry route(String code, String path, int priority) {
        return SysPrivApiRegistry.builder().id((long) code.hashCode()).apiCode(code)
                .httpMethod("GET").pathPattern(path).priority(priority).active(true).build();
    }
}
