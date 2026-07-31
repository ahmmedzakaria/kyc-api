package com.nexacore.systemmodule.layout.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LayoutNavigationCategoryOrderRequestDto {
    private List<CategoryOrder> categories = new ArrayList<>();

    @Data
    public static class CategoryOrder {
        private Long id;
        private Integer displayOrder;
    }
}
