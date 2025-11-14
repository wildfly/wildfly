/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import org.eclipse.microprofile.openapi.OASConfig;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.filter.TestModelReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates that multiple REST applications from different deployments can publish to the same OpenAPI endpoint.
 * @author Paul Ferraro
 */
@ServerSetup(OpenAPIMultipleDeploymentTestCase.ConfigServerSetupTask.class)
@RunWith(Arquillian.class)
@RunAsClient
public class OpenAPIMultipleDeploymentTestCase {
    private static final String DEPLOYMENT1_NAME = "deployment-1";
    private static final String DEPLOYMENT2_NAME = "deployment-2";

    @Deployment(name = DEPLOYMENT1_NAME, testable = false)
    public static Archive<?> deployment1() {
        return deployment(DEPLOYMENT1_NAME);
    }

    @Deployment(name = DEPLOYMENT2_NAME, testable = false, managed = false)
    public static Archive<?> deployment2() {
        return deployment(DEPLOYMENT2_NAME);
    }

    private static Archive<?> deployment(String deploymentName) {
        Properties properties = new Properties();
        properties.setProperty(OASConfig.MODEL_READER, TestModelReader.class.getName());
        properties.setProperty(TestModelReader.CALLBACK_PATH_DESCRIPTION, "Callback description for " + deploymentName);
        properties.setProperty(TestModelReader.EXAMPLE_SUMMARY, "Example summary for " + deploymentName);
        properties.setProperty(TestModelReader.HEADER_DESCRIPTION, "Header description for " + deploymentName);
        properties.setProperty(TestModelReader.LINK_DESCRIPTION, "Link description: " + deploymentName);
        properties.setProperty(TestModelReader.PARAMETER_DESCRIPTION, "Parameter description for " + deploymentName);
        properties.setProperty(TestModelReader.PATH_ITEM_DESCRIPTION, "Path item description for " + deploymentName);
        properties.setProperty(TestModelReader.REQUEST_BODY_DESCRIPTION, "Request body description for " + deploymentName);
        properties.setProperty(TestModelReader.RESPONSE_DESCRIPTION, "Response description for " + deploymentName);
        properties.setProperty(TestModelReader.SCHEMA_DESCRIPTION, "Schema description for " + deploymentName);
        properties.setProperty(TestModelReader.SECURITY_SCHEME_DESCRIPTION, "Security scheme description for " + deploymentName);
        properties.setProperty(TestModelReader.EXTERNAL_DOCUMENTATION_DESCRIPTION, "External documentation description for " + deploymentName);
        properties.setProperty(TestModelReader.EXTERNAL_DOCUMENTATION_URL, "External documentation description for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_CONTACT_EMAIL, "Contact email for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_CONTACT_NAME, "Contact name for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_CONTACT_URL, "Contact url for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_DESCRIPTION, "Description for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_LICENSE_IDENTIFIER, "License identifier for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_LICENSE_NAME, "License name for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_LICENSE_URL, "License url for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_SUMMARY, "Summary for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_TERMS_OF_SERVICE, "Terms of service for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_TITLE, "Title for " + deploymentName);
        properties.setProperty(TestModelReader.INFO_VERSION, "Version for " + deploymentName);
        properties.setProperty(TestModelReader.JSON_SCHEMA_DIALECT, "JSON schema dialect for " + deploymentName);
        properties.setProperty(TestModelReader.VERSION, "OpenAPI version for " + deploymentName);
        try (StringWriter writer = new StringWriter()) {
            properties.store(writer, null);
            return ShrinkWrap.create(WebArchive.class, deploymentName + ".war")
                    .addPackage(TestModelReader.class.getPackage())
                    .addAsManifestResource(new StringAsset(writer.toString()), "microprofile-config.properties")
                    ;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @OperateOnDeployment(DEPLOYMENT1_NAME)
    @ArquillianResource
    private URL baseURL;

    @ArquillianResource
    private Deployer deployer;

    @Test
    public void test() throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        JsonNode model = this.read(client);
        // OpenAPI model should use singleton properties from host, not deployment
        verifySingletonProperties(model, DEPLOYMENT1_NAME);
        verifyMapProperties(model, DEPLOYMENT1_NAME, "");
        this.deployer.deploy(DEPLOYMENT2_NAME);
        try {
            model = this.read(client);
            System.out.println(model.toPrettyString());
            // Verify use of host-specific properties
            verifySingletonProperties(model, null);
            for (String deploymentName : List.of(DEPLOYMENT1_NAME, DEPLOYMENT2_NAME)) {
                // Verify deployment-specific components and references
                verifyMapProperties(model, deploymentName, deploymentName + "-");
            }
        } finally {
            this.deployer.undeploy(DEPLOYMENT2_NAME);
        }
        model = this.read(client);
        // Verify model is back to normal
        verifySingletonProperties(model, DEPLOYMENT1_NAME);
        verifyMapProperties(model, DEPLOYMENT1_NAME, "");
    }

    private JsonNode read(HttpClient client) throws IOException, InterruptedException, URISyntaxException {
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(this.baseURL.toURI().resolve("/openapi")).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode(), equalTo(HttpURLConnection.HTTP_OK));
        assertThat(response.headers().firstValue("Content-Type").orElse(null), equalTo("application/yaml"));
        return new ObjectMapper(new YAMLFactory()).reader().readTree(response.body());
    }

    private static void verifySingletonProperties(JsonNode model, String titleSuffix) {
        String suffix = " for host";
        assertThat(model.required("externalDocs").required("description").asText(), endsWith(suffix));
        assertThat(model.required("externalDocs").required("url").asText(), endsWith(suffix));
        assertThat(model.required("info").required("contact").required("email").asText(), endsWith(suffix));
        assertThat(model.required("info").required("contact").required("name").asText(), endsWith(suffix));
        assertThat(model.required("info").required("contact").required("url").asText(), endsWith(suffix));
        assertThat(model.required("info").required("description").asText(), endsWith(suffix));
        assertThat(model.required("info").required("license").required("identifier").asText(), endsWith(suffix));
        assertThat(model.required("info").required("license").required("name").asText(), endsWith(suffix));
        assertThat(model.required("info").required("license").required("url").asText(), endsWith(suffix));
        assertThat(model.required("info").required("summary").asText(), endsWith(suffix));
        assertThat(model.required("info").required("termsOfService").asText(), endsWith(suffix));
        // Title not defined by host
        if (titleSuffix != null) {
            assertThat(model.required("info").required("title").asText(), endsWith(titleSuffix));
        } else {
            assertThat(model.required("info").get("title"), nullValue());
        }
        assertThat(model.required("info").required("version").asText(), endsWith(suffix));
        assertThat(model.required("jsonSchemaDialect").asText(), endsWith(suffix));
        assertThat(model.required("openapi").asText(), endsWith(suffix));
    }

    private static void verifyMapProperties(JsonNode model, String deploymentName, String deploymentPrefix) {
        String callback = deploymentPrefix + "callback";
        String callbackRef = deploymentPrefix + "callbackRef";
        String example = deploymentPrefix + "example";
        String exampleRef = deploymentPrefix + "exampleRef";
        String header = deploymentPrefix + "header";
        String headerRef = deploymentPrefix + "headerRef";
        String link = deploymentPrefix + "link";
        String linkRef = deploymentPrefix + "linkRef";
        String parameter = deploymentPrefix + "parameter";
        String parameterRef = deploymentPrefix + "parameterRef";
        String pathItem = deploymentPrefix + "path";
        String pathItemRef = deploymentPrefix + "pathRef";
        String requestBody = deploymentPrefix + "requestBody";
        String requestBodyRef = deploymentPrefix + "requestBodyRef";
        String response = deploymentPrefix + "response";
        String responseRef = deploymentPrefix + "responseRef";
        String schema = deploymentPrefix + "schema";
        String schemaRef = deploymentPrefix + "schemaRef";
        String securityScheme = deploymentPrefix + "securityScheme";
        String securitySchemeRef = deploymentPrefix + "securitySchemeRef";
        assertThat(model.required("components").required("callbacks").required(callback).required("callback-path").required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("callbacks").required(callbackRef).required("$ref").asText(), equalTo("#/components/callbacks/" + callback));
        assertThat(model.required("components").required("examples").required(example).required("summary").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("examples").required(exampleRef).required("$ref").asText(), equalTo("#/components/examples/" + example));
        assertThat(model.required("components").required("headers").required(header).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("headers").required(headerRef).required("$ref").asText(), equalTo("#/components/headers/" + header));
        assertThat(model.required("components").required("links").required(link).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("links").required(linkRef).required("$ref").asText(), equalTo("#/components/links/" + link));
        assertThat(model.required("components").required("parameters").required(parameter).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("parameters").required(parameterRef).required("$ref").asText(), equalTo("#/components/parameters/" + parameter));
        assertThat(model.required("components").required("pathItems").required(pathItem).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("pathItems").required(pathItemRef).required("$ref").asText(), equalTo("#/components/pathItems/" + pathItem));
        assertThat(model.required("components").required("requestBodies").required(requestBody).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("requestBodies").required(requestBodyRef).required("$ref").asText(), equalTo("#/components/requestBodies/" + requestBody));
        assertThat(model.required("components").required("responses").required(response).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("responses").required(responseRef).required("$ref").asText(), equalTo("#/components/responses/" + response));
        assertThat(model.required("components").required("schemas").required(schema).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("schemas").required(schemaRef).required("$ref").asText(), equalTo("#/components/schemas/" + schema));
        assertThat(model.required("components").required("securitySchemes").required(securityScheme).required("description").asText(), endsWith(deploymentName));
        assertThat(model.required("components").required("securitySchemes").required(securitySchemeRef).required("$ref").asText(), equalTo("#/components/securitySchemes/" + securityScheme));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("callbacks").required("operation-callback").required("$ref").asText(), equalTo("#/components/callbacks/" + callback));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("parameters").required(0).required("$ref").asText(), equalTo("#/components/parameters/" + parameter));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("requestBody").required("$ref").asText(), equalTo("#/components/requestBodies/" + requestBody));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("responses").required("operation-response-ref").required("$ref").asText(), equalTo("#/components/responses/" + response));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("responses").required("operation-response").required("content").required("media-type").required("examples").required("media-type-example").required("$ref").asText(), equalTo("#/components/examples/" + example));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("responses").required("operation-response").required("content").required("media-type").required("schema").required("$ref").asText(), equalTo("#/components/schemas/" + schema));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("responses").required("operation-response").required("headers").required("response-header").required("$ref").asText(), equalTo("#/components/headers/" + header));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("responses").required("operation-response").required("links").required("response-link").required("$ref").asText(), equalTo("#/components/links/" + link));
        assertThat(model.required("paths").required(String.format("/%s/path", deploymentName)).required("get").required("tags").required(0).asText(), equalTo(deploymentPrefix + "tag"));
        assertThat(model.required("paths").required(String.format("/%s/path-ref", deploymentName)).required("$ref").asText(), equalTo("#/components/pathItems/" + pathItem));
    }

    private static void verifyListProperties(JsonNode model, List<String> deploymentPrefix) {
        assertThat(model.required("security").size(), equalTo(deploymentPrefix.size()));
        for (int i = 0; i < deploymentPrefix.size(); ++i) {
            assertThat(model.required("security").required(i).required(deploymentPrefix + "securityScheme"), notNullValue());
        }
        assertThat(model.required("tags").size(), equalTo(deploymentPrefix.size()));
        for (int i = 0; i < deploymentPrefix.size(); ++i) {
            assertThat(model.required("tags").required(i).required("name"), equalTo(deploymentPrefix + "tag"));
        }
    }

    public static class ConfigServerSetupTask extends ManagementServerSetupTask {

        public ConfigServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.externalDocs.description:add(value=External documentation description for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.externalDocs.url:add(value=External documentation url for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.contact.email:add(value=Contact email for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.contact.name:add(value=Contact name for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.contact.url:add(value=Contact url for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.description:add(value=Description for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.license.identifier:add(value=License identifier for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.license.name:add(value=License name for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.license.url:add(value=License url for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.summary:add(value=Summary for host)")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.termsOfService:add(value=Terms of service for host)")
                            // Title to be specified by deployment
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.version:add(value=Version for host)")
                            .add("/system-property=mp.openapi.extensions.jsonSchemaDialect:add(value=JSON schema dialect for host)") // validate that value from generic property used in absence of host-specific property
                            .add("/system-property=mp.openapi.extensions.version:add(value=OpenAPI version for host)") // validate that value from generic property used in absence of host-specific property
                            .add("/system-property=mp.openapi.extensions.info.version:add(value=Version for foo)") // validate that host specific property takes precedence over generic property
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.component-key-format:add(value=%1\\$s-%2\\$s)")
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.externalDocs.description:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.externalDocs.url:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.contact.email:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.contact.name:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.contact.url:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.description:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.license.identifier:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.license.name:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.license.url:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.summary:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.termsOfService:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.info.version:remove")
                            .add("/system-property=mp.openapi.extensions.jsonSchemaDialect:remove")
                            .add("/system-property=mp.openapi.extensions.version:remove")
                            .add("/system-property=mp.openapi.extensions.info.version:remove")
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.component-key-format:remove")
                            .build())
                    .build());
        }

        @Override
        public void setup(ManagementClient client, String containerId) throws Exception {
            super.setup(client, containerId);
            // We need to force a reload.  System properties added at runtime are not visible to existing Config instances.
            ServerReload.executeReloadAndWaitForCompletion(client);
        }
    }
}
