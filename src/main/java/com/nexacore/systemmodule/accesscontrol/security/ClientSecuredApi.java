package com.nexacore.systemmodule.accesscontrol.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClientSecuredApi {
    String moduleCode() default "";
    String moduleName() default "";
    String submoduleCode() default "";
    String submoduleName() default "";
    String featureTypeCode() default "";
    String featureTypeName() default "";
    String featureCode() default "";
    String featureName() default "";
    String actionCode() default "";
    String actionName() default "";
    String requiredPrivilegeCode() default "";
    ClientAuthenticationRequirement clientAuthentication() default ClientAuthenticationRequirement.REQUIRED;
    UserAuthorizationRequirement userAuthorization() default UserAuthorizationRequirement.PRIVILEGE;
    ApiDataScope dataScope() default ApiDataScope.NONE;
    int priority() default 0;
    boolean publicApi() default false;
}
