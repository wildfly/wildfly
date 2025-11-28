/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi.filter;

import java.util.List;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * @author Paul Ferraro
 */
public class TestModelReader implements OASModelReader {
    public static final String CALLBACK_PATH_DESCRIPTION = "callback.path.description";
    public static final String EXAMPLE_SUMMARY = "example.summary";
    public static final String HEADER_DESCRIPTION = "header.description";
    public static final String LINK_DESCRIPTION = "link.description";
    public static final String PARAMETER_DESCRIPTION = "parameter.description";
    public static final String PATH_ITEM_DESCRIPTION = "path-item.description";
    public static final String REQUEST_BODY_DESCRIPTION = "request-body.description";
    public static final String RESPONSE_DESCRIPTION = "response.description";
    public static final String SCHEMA_DESCRIPTION = "schema.description";
    public static final String SECURITY_SCHEME_DESCRIPTION = "security-scheme.description";

    public static final String EXTERNAL_DOCUMENTATION_DESCRIPTION = "external-documentation.description";
    public static final String EXTERNAL_DOCUMENTATION_URL = "external-documentation.url";
    public static final String INFO_CONTACT_EMAIL = "info.contact.email";
    public static final String INFO_CONTACT_NAME = "info.contact.name";
    public static final String INFO_CONTACT_URL = "info.contact.url";
    public static final String INFO_DESCRIPTION = "info.description";
    public static final String INFO_LICENSE_IDENTIFIER = "info.license.identifier";
    public static final String INFO_LICENSE_NAME = "info.license.name";
    public static final String INFO_LICENSE_URL = "info.license.url";
    public static final String INFO_SUMMARY = "info.summary";
    public static final String INFO_TERMS_OF_SERVICE = "info.terms-of-service";
    public static final String INFO_TITLE = "info.title";
    public static final String INFO_VERSION = "info.version";
    public static final String JSON_SCHEMA_DIALECT = "json-schema-dialect";
    public static final String VERSION = "version";

    private final Config config = ConfigProvider.getConfig(TestModelReader.class.getClassLoader());

    @Override
    public OpenAPI buildModel() {
        return OASFactory.createOpenAPI()
                .components(OASFactory.createComponents()
                        .addCallback("callback", OASFactory.createCallback().addPathItem("callback-path", OASFactory.createPathItem().description(this.config.getValue(CALLBACK_PATH_DESCRIPTION, String.class))))
                        .addCallback("callbackRef", OASFactory.createCallback().ref("#/components/callbacks/callback"))
                        .addExample("example", OASFactory.createExample().summary(this.config.getValue(EXAMPLE_SUMMARY, String.class)))
                        .addExample("exampleRef", OASFactory.createExample().ref("#/components/examples/example"))
                        .addHeader("header", OASFactory.createHeader().description(this.config.getValue(HEADER_DESCRIPTION, String.class)))
                        .addHeader("headerRef", OASFactory.createHeader().ref("#/components/headers/header"))
                        .addLink("link", OASFactory.createLink().description(this.config.getValue(LINK_DESCRIPTION, String.class)))
                        .addLink("linkRef", OASFactory.createLink().ref("#/components/links/link"))
                        .addParameter("parameter", OASFactory.createParameter().description(this.config.getValue(PARAMETER_DESCRIPTION, String.class)))
                        .addParameter("parameterRef", OASFactory.createParameter().ref("#/components/parameters/parameter"))
                        .addPathItem("path", OASFactory.createPathItem().description(this.config.getValue(PATH_ITEM_DESCRIPTION, String.class)))
                        .addPathItem("pathRef", OASFactory.createPathItem().ref("#/components/pathItems/path"))
                        .addRequestBody("requestBody", OASFactory.createRequestBody().description(this.config.getValue(REQUEST_BODY_DESCRIPTION, String.class)))
                        .addRequestBody("requestBodyRef", OASFactory.createRequestBody().ref("#/components/requestBodies/requestBody"))
                        .addResponse("response", OASFactory.createAPIResponse().description(this.config.getValue(RESPONSE_DESCRIPTION, String.class)))
                        .addResponse("responseRef", OASFactory.createAPIResponse().ref("#/components/responses/response"))
                        .addSchema("schema", OASFactory.createSchema().description(this.config.getValue(SCHEMA_DESCRIPTION, String.class)))
                        .addSchema("schemaRef", OASFactory.createSchema().ref("#/components/schemas/schema"))
                        .addSecurityScheme("securityScheme", OASFactory.createSecurityScheme().description(this.config.getValue(SECURITY_SCHEME_DESCRIPTION, String.class)))
                        .addSecurityScheme("securitySchemeRef", OASFactory.createSecurityScheme().ref("#/components/securitySchemes/securityScheme")))
                .externalDocs(OASFactory.createExternalDocumentation()
                        .description(this.config.getValue(EXTERNAL_DOCUMENTATION_DESCRIPTION, String.class))
                        .url(this.config.getValue(EXTERNAL_DOCUMENTATION_URL, String.class)))
                .info(OASFactory.createInfo()
                        .contact(OASFactory.createContact()
                                .email(this.config.getValue(INFO_CONTACT_EMAIL, String.class))
                                .name(this.config.getValue(INFO_CONTACT_NAME, String.class))
                                .url(this.config.getValue(INFO_CONTACT_URL, String.class)))
                        .description(this.config.getValue(INFO_DESCRIPTION, String.class))
                        .license(OASFactory.createLicense()
                                .identifier(this.config.getValue(INFO_LICENSE_IDENTIFIER, String.class))
                                .name(this.config.getValue(INFO_LICENSE_NAME, String.class))
                                .url(this.config.getValue(INFO_LICENSE_URL, String.class)))
                        .summary(this.config.getValue(INFO_SUMMARY, String.class))
                        .termsOfService(this.config.getValue(INFO_TERMS_OF_SERVICE, String.class))
                        .title(this.config.getValue(INFO_TITLE, String.class))
                        .version(this.config.getValue(INFO_VERSION, String.class)))
                .paths(OASFactory.createPaths()
                        .addPathItem("/path", OASFactory.createPathItem().GET(OASFactory.createOperation()
                                .addCallback("operation-callback", OASFactory.createCallback().ref("#/components/callbacks/callback"))
                                .addParameter(OASFactory.createParameter().ref("#/components/parameters/parameter"))
                                .addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("securityScheme"))
                                .requestBody(OASFactory.createRequestBody().ref("#/components/requestBodies/requestBody"))
                                .responses(OASFactory.createAPIResponses()
                                        .addAPIResponse("operation-response-ref", OASFactory.createAPIResponse().ref("#/components/responses/response"))
                                        .addAPIResponse("operation-response", OASFactory.createAPIResponse()
                                                .content(OASFactory.createContent().addMediaType("media-type", OASFactory.createMediaType()
                                                        .addExample("media-type-example", OASFactory.createExample().ref("#/components/examples/example"))
                                                        .schema(OASFactory.createSchema().ref("#/components/schemas/schema"))))
                                                .addHeader("response-header", OASFactory.createHeader().ref("#/components/headers/header"))
                                                .addLink("response-link", OASFactory.createLink().ref("#/components/links/link"))))
                                .addTag("tag")
                                ))
                        .addPathItem("/path-ref", OASFactory.createPathItem().ref("#/components/pathItems/path")))
                .jsonSchemaDialect(this.config.getValue(JSON_SCHEMA_DIALECT, String.class))
                .openapi(this.config.getValue(VERSION, String.class))
                .addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("securityScheme", List.of("scope")))
                .addTag(OASFactory.createTag().name("tag").description("Tag description"))
                ;
    }
}
