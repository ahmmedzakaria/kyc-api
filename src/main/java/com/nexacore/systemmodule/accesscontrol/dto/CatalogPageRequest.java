package com.nexacore.systemmodule.accesscontrol.dto;
public record CatalogPageRequest(String query,int page,int pageSize) {
    public CatalogPageRequest { if(page<0)page=0; if(pageSize<1||pageSize>200)pageSize=25; }
}
