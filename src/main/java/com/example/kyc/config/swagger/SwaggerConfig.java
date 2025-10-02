package com.example.kyc.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Response;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("KYC API")
                        .description("KYC Management API with JWT Auth")
                        .version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }


    public ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("KYC API")
                .description("API documentation for KYC system")
                .version("1.0.1")
                .license("").licenseUrl("").termsOfServiceUrl("")
                .contact(new Contact("Zakaria Ahmmed", "www.example.com", "me@example.com")).build();
    }

    @Bean
    Docket docket() {
        return new Docket(DocumentationType.OAS_30).apiInfo(apiInfo()).select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class)).paths(PathSelectors.any())
                .build().useDefaultResponseMessages(false).globalResponses(HttpMethod.POST, getGlobalResponsesDocumentation())
                .globalResponses(HttpMethod.GET, getGlobalResponsesDocumentation());
    }


    public List<Response> getGlobalResponsesDocumentation() {

        List<Response> httpResponseList = new ArrayList<>();
        Response responseOK = new ResponseBuilder().code("200").description("Successfully retrived").build();
        Response response400 = new ResponseBuilder().code("400").description(
                        "Bad request, i.e. The client's request is malformed or contains errors, making it impossible for the server to understand or process the request.")
                .build();
        Response response401 = new ResponseBuilder().code("401").description(
                        "Unauthorized, i.e. The client's request is missing valid authentication credentials, or the provided credentials are invalid or insufficient to access the requested resource.")
                .build();
        Response response412 = new ResponseBuilder().code("412").description(
                        "Precondition failed, i.e. The server received a client request with specific preconditions that were not met.")
                .build();
        Response response415 = new ResponseBuilder().code("415").description(
                        "Method not allowed, i.e. The server cannot process the client's request because the provided media type or content format is not supported or acceptable.")
                .build();
        Response response500 = new ResponseBuilder().code("500").description(
                        "Internal Server Error, i.e. The server encountered an unexpected error or condition that prevented it from processing the client's request.")
                .build();

        httpResponseList.add(responseOK);
        httpResponseList.add(response400);
        httpResponseList.add(response401);
        httpResponseList.add(response412);
        httpResponseList.add(response415);
        httpResponseList.add(response500);

        return httpResponseList;
    }
}

