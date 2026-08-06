package com.nexacore.systemmodule.accesscontrol.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ClientSecuredApi(userAuthorization = UserAuthorizationRequirement.NONE)
public @interface AuthenticatedApi {
  /*
  @AuthenticatedApi marks an endpoint that requires a valid authenticated user but does not require a specific privilege code.
            Conceptually, it represents:
    Valid client, normally required
        +
    Valid user access token
        +
    No specific privilege check
    */
}
