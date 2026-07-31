package com.nexacore.systemmodule.layout.service.interfaces;

import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileRequestDto;

import java.util.List;

public interface LayoutProfileService {
    LayoutProfileDto save(LayoutProfileRequestDto request, String actor);
    List<LayoutProfileDto> list();
    LayoutProfileDto detail(String profileCode);
}
