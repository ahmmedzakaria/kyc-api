package com.nexacore.systemmodule.accesscontrol.security;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ClientSecuredApi
public @interface PrivilegeApi {
    @AliasFor(annotation = ClientSecuredApi.class, attribute = "requiredPrivilegeCode")
    String value();

    @AliasFor(annotation = ClientSecuredApi.class, attribute = "dataScope")
    ApiDataScope dataScope() default ApiDataScope.NONE;

    @AliasFor(annotation = ClientSecuredApi.class, attribute = "priority")
    int priority() default 0;
}
