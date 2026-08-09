package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiRouteMatcherTest {
    private final ApiRouteMatcher matcher = new ApiRouteMatcher();

    @Test
    void exactRouteWinsBeforeAnyTemplate() {
        SysAccApiRegistry exact = route("exact", "/api/person/me", -100);
        SysAccApiRegistry template = route("template", "/api/person/{id}", 100);

        assertThat(matcher.resolve(List.of(template, exact), "/api/person/me"))
                .contains(exact);
    }

    @Test
    void mostSpecificTemplateWinsBeforeExplicitPriority() {
        SysAccApiRegistry specific = route("specific", "/api/person/{id}/documents/{documentId}", -100);
        SysAccApiRegistry broad = route("broad", "/api/person/**", 100);

        assertThat(matcher.resolve(List.of(broad, specific), "/api/person/7/documents/9"))
                .contains(specific);
    }

    @Test
    void priorityBreaksEquallySpecificTemplateTie() {
        SysAccApiRegistry lower = route("lower", "/api/person/{id}", 5);
        SysAccApiRegistry higher = route("higher", "/api/person/{personId}", 10);

        assertThat(matcher.resolve(List.of(lower, higher), "/api/person/7"))
                .contains(higher);
    }

    @Test
    void unresolvedTieFailsClosedRegardlessOfRepositoryOrder() {
        SysAccApiRegistry left = route("left", "/api/person/{id}", 10);
        SysAccApiRegistry right = route("right", "/api/person/{personId}", 10);

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

    private SysAccApiRegistry route(String code, String path, int priority) {
        return SysAccApiRegistry.builder().id((long) code.hashCode()).apiCode(code)
                .httpMethod("GET").pathPattern(path).priority(priority).active(true).build();
    }
}
