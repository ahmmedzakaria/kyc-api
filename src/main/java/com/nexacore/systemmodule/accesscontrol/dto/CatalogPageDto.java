package com.nexacore.systemmodule.accesscontrol.dto;
import java.util.List;
public record CatalogPageDto<T>(List<T> items,long total,int page,int pageSize) {}
