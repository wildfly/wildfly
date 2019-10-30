/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.http;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.junit.Test;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.SimpleHttpAuthenticationFactory;

/**
 * Abstract parent for Elytron HTTP mechanisms tests.
 *
 * @author Jan Kalina
 */
abstract class AbstractMechTestBase {

    static final String APP_DOMAIN = "MechTestAppDomain";
    private static final String DEFAULT_SECURITY_DOMAIN = "ApplicationDomain";
    private static final String DEFAULT_MECHANISM_FACTORY = "global";
    private static final String HTTP_FACTORY = "MechTestHttpFactory";

    @ArquillianResource
    protected URL url;

    @ArquillianResource
    protected ManagementClient mgmtClient;

    @Test
    public void testUnprotected() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "unprotected"));
        HttpClientContext context = HttpClientContext.create();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(request, context)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                assertEquals("Unexpected content of HTTP response.", SimpleServlet.RESPONSE_BODY, EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Test
    public void testUnauthorized() throws Exception {
        HttpGet request = new HttpGet(new URI(url.toExternalForm() + "role1"));
        HttpClientContext context = HttpClientContext.create();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            try (CloseableHttpResponse response = httpClient.execute(request, context)) {
                int statusCode = response.getStatusLine().getStatusCode();
                assertEquals("Unexpected status code in HTTP response.", SC_UNAUTHORIZED, statusCode);
            }
        }
    }

    abstract static class ServerSetup extends AbstractElytronSetupTask {

        protected abstract MechanismConfiguration getMechanismConfiguration();

        protected String getSecurityDomain() {
            return DEFAULT_SECURITY_DOMAIN;
        }

        protected boolean useAuthenticationFactory() {
            return true;
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ConfigurableElement[] elements = useAuthenticationFactory() ? new ConfigurableElement[2] : new ConfigurableElement[1];
            if (useAuthenticationFactory()) {
                elements[0] = SimpleHttpAuthenticationFactory.builder()
                                  .withName(HTTP_FACTORY)
                                  .withHttpServerMechanismFactory(DEFAULT_MECHANISM_FACTORY)
                                  .withSecurityDomain(getSecurityDomain())
                                  .addMechanismConfiguration(getMechanismConfiguration())
                                  .build();
            }
            elements[elements.length - 1] = new ConfigurableElement() {

                @Override
                public String getName() {
                    return "Configure undertow application-security-domain " + APP_DOMAIN;
                }

                @Override
                public void create(CLIWrapper cli) throws Exception {
                    String argument = useAuthenticationFactory() ? "http-authentication-factory=" + HTTP_FACTORY : "security-domain=" + getSecurityDomain();
                    cli.sendLine("/subsystem=undertow/application-security-domain=" + APP_DOMAIN + ":add(" + argument + ")");
                }

                @Override
                public void remove(CLIWrapper cli) throws Exception {
                    cli.sendLine("/subsystem=undertow/application-security-domain=" + APP_DOMAIN + ":remove");
                }

            };

            return elements;
        }
    }

}
