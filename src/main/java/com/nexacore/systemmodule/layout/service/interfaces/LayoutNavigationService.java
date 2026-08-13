package com.nexacore.systemmodule.layout.service.interfaces;

import com.nexacore.systemmodule.layout.dto.NavNodeDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationCategoryOrderRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationNodeRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationIntegrityDto;

import java.util.List;
import java.util.Set;

public interface LayoutNavigationService {
    List<NavNodeDto> getNavigationTree(String clientCode, String username, Set<String> privilegeCodes);
    List<NavNodeDto> getFullNavigationTree();
    NavNodeDto saveGroup(LayoutNavigationNodeRequestDto request, String actor);
    NavNodeDto saveModule(LayoutNavigationNodeRequestDto request, String actor);
    NavNodeDto saveCategory(LayoutNavigationNodeRequestDto request, String actor);
    List<NavNodeDto> listCategories();
    void reorderCategories(LayoutNavigationCategoryOrderRequestDto request, String actor);
    NavNodeDto saveFeatureGroup(LayoutNavigationNodeRequestDto request, String actor);
    NavNodeDto saveFeature(LayoutNavigationNodeRequestDto request, String actor);
    LayoutNavigationIntegrityDto diagnoseIntegrity();
}
