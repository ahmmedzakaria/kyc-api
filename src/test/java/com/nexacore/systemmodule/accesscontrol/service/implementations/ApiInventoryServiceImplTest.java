package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.dto.ApiInventoryItemDto;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiInventoryServiceImplTest {

    @Test
    void inventoriesOnlyNexaCoreApiMappingsAndMarksUndeclaredMetadataForReview() throws Exception {
        RequestMappingHandlerMapping handlerMapping = new RequestMappingHandlerMapping();
        register(
                handlerMapping,
                RequestMappingInfo.paths("/api/v1/example").methods(RequestMethod.POST).build(),
                new ExampleController(),
                "save"
        );
        register(
                handlerMapping,
                RequestMappingInfo.paths("/internal/example").methods(RequestMethod.GET).build(),
                new ExampleController(),
                "internal"
        );
        register(
                handlerMapping,
                RequestMappingInfo.paths("/api/framework").methods(RequestMethod.GET).build(),
                new Object(),
                "toString"
        );

        List<ApiInventoryItemDto> inventory = new ApiInventoryServiceImpl(handlerMapping).inventory();

        assertThat(inventory).hasSize(1);
        ApiInventoryItemDto item = inventory.getFirst();
        assertThat(item.getApiCode()).isEqualTo("POST:/api/v1/example");
        assertThat(item.getSourceModule()).isEqualTo("systemmodule");
        assertThat(item.isAccessMetadataDeclared()).isFalse();
        assertThat(item.getReviewStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(item.getDataScope()).isEqualTo("REVIEW_REQUIRED");
    }

    private HandlerMethod handlerMethod(Object bean, String methodName) throws NoSuchMethodException {
        Method method = bean.getClass().getDeclaredMethod(methodName);
        return new HandlerMethod(bean, method);
    }

    private void register(
            RequestMappingHandlerMapping handlerMapping,
            RequestMappingInfo mapping,
            Object bean,
            String methodName
    ) throws NoSuchMethodException {
        handlerMapping.registerMapping(mapping, bean, handlerMethod(bean, methodName).getMethod());
    }

    private static class ExampleController {
        void save() {
        }

        void internal() {
        }
    }
}
