package com.nexacore.systemmodule.accesscontrol.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ClientSecuredApi(
        publicApi = true,
        clientAuthentication = ClientAuthenticationRequirement.OPTIONAL,
        userAuthorization = UserAuthorizationRequirement.NONE
)
public @interface PublicApi {
}
