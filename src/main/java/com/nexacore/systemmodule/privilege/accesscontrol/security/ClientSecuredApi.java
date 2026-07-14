package com.nexacore.systemmodule.privilege.accesscontrol.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClientSecuredApi {
    String moduleCode();
    String moduleName();
    String submoduleCode();
    String submoduleName();
    String featureTypeCode();
    String featureTypeName();
    String featureCode();
    String featureName();
    String actionCode();
    String actionName();
    boolean publicApi() default false;
}
