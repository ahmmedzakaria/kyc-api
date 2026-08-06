package com.nexacore.systemmodule.accesscontrol.security;

public enum ClientAuthenticationRequirement {
    //pecifies whether the calling application itself must authenticate. This is separate from authenticating the human user.

    NONE, //No client credential is expected. Suitable for public endpoints such as health checks or SSO callbacks.
    OPTIONAL, //A recognized client may identify itself, but requests without client credentials are still accepted. Useful for login or registration endpoints shared by browser and trusted clients.
    REQUIRED //The calling frontend, mobile application, or integration must be recognized and authorized for the API.
}
