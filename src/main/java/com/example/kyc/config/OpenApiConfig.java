package com.example.kyc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
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
import java.util.Arrays;
import java.util.List;

@Configuration
//@EnableSwagger2
public class OpenApiConfig implements WebMvcConfigurer {
    
	@Bean
	Docket docket() {
		return new Docket(DocumentationType.OAS_30).apiInfo(apiInfo()).select()
				.apis(RequestHandlerSelectors.withClassAnnotation(RestController.class)).paths(PathSelectors.any())
				.build().useDefaultResponseMessages(false).globalResponses(HttpMethod.POST, getGlobalResponses())
				.globalResponses(HttpMethod.GET, getGlobalResponses());
	}


	public List<Response> getGlobalResponses() {

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

	public ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("KYC API")
				.description("API documentation for KYC system")
				.license("").licenseUrl("").termsOfServiceUrl("").version("1.0.1")
				.contact(new Contact("KYC", "http://localhost:9100", "http://localhost:9100")).build();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
		registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
	}

}

