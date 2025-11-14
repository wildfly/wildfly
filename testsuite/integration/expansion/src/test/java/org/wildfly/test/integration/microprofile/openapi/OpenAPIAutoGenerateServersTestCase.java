/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.openapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.ManagementServerSetupTask;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.openapi.service.TestApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Validates usage of "mp.openapi.extensions.server.*.host.*.auto-generate-servers" configuration property.
 * @author Paul Ferraro
 */
@ServerSetup(OpenAPIAutoGenerateServersTestCase.ConfigServerSetupTask.class)
@RunWith(Arquillian.class)
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
    public void test() throws IOException, URISyntaxException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(HttpRequest.newBuilder(this.baseURL.toURI().resolve("/openapi")).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode(), equalTo(HttpURLConnection.HTTP_OK));
        List<String> urls = validateContent(response);
        // Ensure absolute urls are valid
        for (String url : urls) {
            try {
                response = client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
                assertThat(response.statusCode(), equalTo(HttpURLConnection.HTTP_OK));
                assertThat(response.body(), equalTo("foo"));
            } catch (SSLHandshakeException ignored) {
                // Ignore exception due to auto-generated self-signed certificate
                // javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target
            }
        }
    }

    private static List<String> validateContent(HttpResponse<String> response) throws IOException {
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(null), equalTo("application/yaml"));

        JsonNode node = new ObjectMapper(new YAMLFactory()).reader().readTree(response.body());
        System.out.println(node.toPrettyString());
        JsonNode info = node.required("info");
        assertThat(info.required("title").asText(), equalTo(DEPLOYMENT_NAME));
        assertThat(info.get("description"), nullValue());

        JsonNode servers = node.required("servers");
        JsonNode paths = node.required("paths");
        List<String> result = new LinkedList<>();
        for (JsonNode server : servers) {
            Iterator<String> pathNames = paths.fieldNames();
            while (pathNames.hasNext()) {
                result.add(server.required("url").asText() + pathNames.next().replace("{value}", "foo"));
            }
        }
        assertThat(result, not(empty()));
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
