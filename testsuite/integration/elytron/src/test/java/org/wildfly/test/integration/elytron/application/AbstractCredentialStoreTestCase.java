/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.elytron.application;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.shared.integration.ejb.security.PermissionUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.wildfly.test.security.servlets.ReadCredentialServlet;

/**
 * Abstract parent for Elytron Credential store test cases. It provides a deployment with {@link ReadCredentialServlet} and
 * helper {@code assert*} methods to verify credential store content.
 *
 * @author Josef Cacek
 */
public abstract class AbstractCredentialStoreTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(ReadCredentialServlet.class)
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.server,org.jboss.as.controller,org.wildfly.security.elytron\n"),
                        "MANIFEST.MF")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new AllPermission()), "permissions.xml");
    }

    /**
     * Asserts that given credential store contains alias with given value.
     *
     * @param credentialStore
     * @param alias secret name
     * @param expectedValue expected secret value
     * @throws Exception
     */
    protected void assertCredentialValue(String credentialStore, String alias, String expectedValue) throws Exception {
        assertEquals("Unexpected password (secret-value) in credential store", expectedValue,
                doReadCredentialPostReq(credentialStore, alias, SC_OK));
    }

    /**
     * Asserts that the provided credentialStore + alias combination doesn't exist.
     *
     * @param credentialStore store name
     * @param alias alias to check
     */
    protected void assertCredentialNotFound(String credentialStore, String alias) throws Exception {
        doReadCredentialPostReq(credentialStore, alias, SC_NOT_FOUND);
    }

    /**
     * Asserts that a credential store with given name contains given aliases.
     *
     * @param cli connected {@link CLIWrapper} instance (not <code>null</code>)
     * @param storeName credential store name (not <code>null</code>)
     * @param aliases aliases to check
     * @throws IOException
     */
    protected void assertContainsAliases(CLIWrapper cli, String storeName, String... aliases) throws IOException {
        if (aliases == null || aliases.length > 0) {
            return;
        }
        cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:read-children-names(child-type=alias)", storeName));
        final CLIOpResult opResult = cli.readAllAsOpResult();
        Set<String> set = opResult.getResponseNode().get(ModelDescriptionConstants.RESULT).asList().stream()
                .map(n -> n.asString()).collect(Collectors.toSet());
        for (String alias : aliases) {
            if (!set.contains(alias)) {
                fail(String.format("Credential store '%s' doesn't contain expected alias '%s'", storeName, alias));
            }
        }
    }

    /**
     * Creates alias in given credential store (must exist) with provided secret value. Then uses
     * {@link #assertCredentialValue(String, String, String)} method to check if it's correctly stored in the credential
     * store. It removes the alias as the final step.
     */
    protected void assertAliasAndSecretSupported(String storeName, String alias, String secret) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                if (secret != null) {
                    cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:add-alias(alias=%s, secret-value=\"%s\")", storeName,
                            alias, secret));
                    assertCredentialValue(storeName, alias, secret);
                } else {
                    cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:add-alias(alias=%s)", storeName, alias));
                    assertCredentialValue(storeName, alias, "");
                }
            } finally {
                cli.sendLine(String.format("/subsystem=elytron/credential-store=%s:remove-alias(alias=%s)", storeName, alias), true);
            }
        }
    }

    /**
     * Makes request to {@link ReadCredentialServlet} to check read secret value from credential store. It asserts HTTP status
     * code in the response.
     */
    private String doReadCredentialPostReq(String credentialStore, String alias, int expectedStatus)
            throws URISyntaxException, UnsupportedEncodingException, IOException, ClientProtocolException {
        String body;
        final URI uri = new URI(url.toExternalForm() + ReadCredentialServlet.SERVLET_PATH.substring(1));
        final HttpPost post = new HttpPost(uri);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair(ReadCredentialServlet.PARAM_CREDENTIAL_STORE, credentialStore));
        nvps.add(new BasicNameValuePair(ReadCredentialServlet.PARAM_ALIAS, alias));
        post.setEntity(new UrlEncodedFormEntity(nvps, StandardCharsets.UTF_8));
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", expectedStatus, statusCode);
                body = EntityUtils.toString(response.getEntity());
            }
        }
        return body;
    }
}
