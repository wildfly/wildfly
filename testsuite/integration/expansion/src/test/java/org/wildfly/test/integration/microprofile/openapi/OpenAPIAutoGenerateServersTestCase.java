/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates usage of "mp.openapi.extensions.server.*.host.*.auto-generate-servers" configuration property.
 * @author Paul Ferraro
 */
@ServerSetup(OpenAPIAutoGenerateServersTestCase.ConfigServerSetupTask.class)
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class OpenAPIAutoGenerateServersTestCase {
    private static final String DEPLOYMENT_NAME = OpenAPIAutoGenerateServersTestCase.class.getSimpleName() + ".war";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME)
                .add(EmptyAsset.INSTANCE, "WEB-INF/beans.xml")
                .addPackage(TestApplication.class.getPackage())
                ;
    }

    @ArquillianResource
    private URL baseURL;

    @Test
    void test() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(this.baseURL.toURI().resolve("/openapi")).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
        List<String> urls = validateContent(response);
        // Ensure absolute urls are valid
        for (String url : urls) {
            try {
                response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertEquals(HttpURLConnection.HTTP_OK, response.statusCode());
                assertEquals("foo", response.body());
            } catch (SSLHandshakeException ignored) {
                // Ignore exception due to auto-generated self-signed certificate
                // javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
            }
        }
    }

    private static List<String> validateContent(HttpResponse<String> response) throws IOException {
        assertEquals("application/yaml", response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(null));

        JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.body());
        System.out.println(node.toPrettyString());
        JsonNode info = node.required("info");
        assertEquals(DEPLOYMENT_NAME, info.required("title").asText());
        assertNull(info.get("description"));

        JsonNode servers = node.required("servers");
        JsonNode paths = node.required("paths");
        List<String> result = new LinkedList<>();
        for (JsonNode server : servers) {
            Iterator<String> pathNames = paths.fieldNames();
            while (pathNames.hasNext()) {
                result.add(server.required("url").asText() + pathNames.next().replace("{value}", "foo"));
            }
        }
        assertFalse(result.isEmpty());
        return result;
    }

    public static class ConfigServerSetupTask extends ManagementServerSetupTask {

        public ConfigServerSetupTask() {
            super(createContainerConfigurationBuilder()
                    .setupScript(createScriptBuilder()
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.auto-generate-servers:add(value=true)")
                            .build())
                    .tearDownScript(createScriptBuilder()
                            .add("/system-property=mp.openapi.extensions.server.default-server.host.default-host.auto-generate-servers:remove")
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
