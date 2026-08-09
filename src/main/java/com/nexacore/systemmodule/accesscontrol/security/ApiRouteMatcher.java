package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ApiRouteMatcher {
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public Optional<SysAccApiRegistry> resolve(List<SysAccApiRegistry> candidates, String requestPath) {
        List<SysAccApiRegistry> matches = candidates.stream()
                .filter(api -> pathMatcher.match(api.getPathPattern(), requestPath))
                .sorted(precedence(requestPath))
                .toList();
        if (matches.isEmpty()) return Optional.empty();
        if (matches.size() > 1 && samePrecedence(matches.get(0), matches.get(1), requestPath)) {
            throw new ApiRouteAmbiguityException("Ambiguous API registry routes: "
                    + matches.get(0).getApiCode() + " and " + matches.get(1).getApiCode());
        }
        return Optional.of(matches.getFirst());
    }

    public boolean hasUnresolvedOverlap(SysAccApiRegistry left, SysAccApiRegistry right) {
        if (!left.getHttpMethod().equalsIgnoreCase(right.getHttpMethod())
                || left.getPriority() != right.getPriority()) return false;
        for (String witness : overlapWitnesses(left.getPathPattern(), right.getPathPattern())) {
            if (pathMatcher.match(left.getPathPattern(), witness)
                    && pathMatcher.match(right.getPathPattern(), witness)
                    && samePrecedence(left, right, witness)) return true;
        }
        return false;
    }

    private Comparator<SysAccApiRegistry> precedence(String path) {
        Comparator<String> patternComparator = pathMatcher.getPatternComparator(path);
        return (left, right) -> {
            int exact = Boolean.compare(isExact(right.getPathPattern(), path), isExact(left.getPathPattern(), path));
            if (exact != 0) return exact;
            int specific = patternComparator.compare(
                    canonicalPattern(left.getPathPattern()), canonicalPattern(right.getPathPattern()));
            if (specific != 0) return specific;
            return Integer.compare(right.getPriority(), left.getPriority());
        };
    }

    private boolean samePrecedence(SysAccApiRegistry left, SysAccApiRegistry right, String path) {
        return precedence(path).compare(left, right) == 0;
    }

    private boolean isExact(String pattern, String path) {
        return pattern != null && pattern.equals(path) && !pathMatcher.isPattern(pattern);
    }

    private String canonicalPattern(String pattern) {
        return pattern.replaceAll("\\{[^/:}]+(?=[:}])", "{variable");
    }

    private List<String> overlapWitnesses(String left, String right) {
        List<String> witnesses = new ArrayList<>();
        witnesses.add(concretePath(left));
        witnesses.add(concretePath(right));
        return witnesses;
    }

    private String concretePath(String pattern) {
        return pattern.replaceAll("\\{[^/]+}", "value")
                .replace("**", "value/more")
                .replace("*", "value")
                .replace("?", "x");
    }
}
