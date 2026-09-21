package com.isp.assistant.config;

import com.isp.assistant.security.ApiKeyFilter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class OpenApiConfig {

    static final String API_KEY_SCHEME = "ApiKeyAuth";

    @Bean
    OpenAPI applicationOpenApi(AppProperties properties) {
        OpenAPI openApi = new OpenAPI()
                .info(new Info()
                        .title("ISP Support Assistant API")
                        .version("v1")
                        .description("API para apoio técnico a analistas N1/N2 de provedores de internet."));

        if (StringUtils.hasText(properties.apiKey())) {
            openApi.components(new Components().addSecuritySchemes(
                    API_KEY_SCHEME,
                    new SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .in(SecurityScheme.In.HEADER)
                            .name(ApiKeyFilter.HEADER_NAME)))
                    .addSecurityItem(new SecurityRequirement().addList(API_KEY_SCHEME));
        }

        return openApi;
    }
}
