package com.nexacore.systemmodule.accesscontrol.security;

public enum ApiDataScope {
    //ApiDataScope declares how narrowly authorized data must be filtered after access to an API is granted.

    NONE, //No organizational data restriction. The user may access all data permitted by the endpoint and privilege.
    TENANT, //Only records belonging to the authenticated user’s tenant.
    BUSINESS, //Only records belonging to the user’s business or company within that tenant.
    BRANCH // Only records belonging to the user’s assigned branch.
}
