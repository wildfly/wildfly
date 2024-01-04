/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.elytron;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 * Smoke test for web application authentication using Elytron.
 * <p>
 * Configuration: The {@link SecurityDomainsSetup} server setup task creates a new Elytron domain backed by a PropertyRealm and
 * maps Undertow application domain (referenced as &lt;security-domain&gt; from {@code jboss-web.xml}) to it.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ ElytronDomainTestCase.SecurityDomainsSetup.class })
@RunAsClient
public class ElytronDomainTestCase {

    private static final String NAME = ElytronDomainTestCase.class.getSimpleName();

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleSecuredServlet.class, SimpleServlet.class)
                .addAsWebInfResource(new StringAsset("<web-app>\n" + //
                        "  <login-config><auth-method>BASIC</auth-method><realm-name>Test realm</realm-name></login-config>\n" + //
                        "</web-app>"), "web.xml")
                .addAsWebInfResource(new StringAsset("<jboss-web>\n" + //
                        "  <security-domain>" + NAME + "</security-domain>\n" + //
                        "</jboss-web>"), "jboss-web.xml");
    }

    /**
     * Tests successful authentication and authorization.
     */
    @Test
    public void testAuthnAuthz(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));

        // successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "elytron1", "password", 200));
    }

    /**
     * Tests successful authentication and failed authorization.
     */
    @Test
    public void testAuthnNoAuthz(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));

        // successful authentication and authorization
        assertEquals("Response body is not correct.", SimpleSecuredServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "elytron1", "password", 200));
        // successful authentication and unsuccessful authorization
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron2", "password", 403);
        // wrong password
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron1", "pass", 401);
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron2", "pass", 401);
        // no such user
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron3", "pass", 401);
    }

    /**
     * Tests unsuccessful authentication.
     */
    @Test
    public void testNoAuthn(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + SimpleSecuredServlet.SERVLET_PATH.substring(1));

        // wrong password
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron1", "pass", 401);
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron2", "pass", 401);
        // no such user
        Utils.makeCallWithBasicAuthn(servletUrl, "elytron3", "pass", 401);
    }

    /**
     * Create properties-file backed Elytron domain with 2 users and mapping in Undertow.
     */
    static class SecurityDomainsSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[] { PropertyFileBasedDomain.builder().withName(NAME)
                    .withUser("elytron1", "password", SimpleSecuredServlet.ALLOWED_ROLE).withUser("elytron2", "password")
                    .build(), UndertowDomainMapper.builder().withName(NAME).build() };
        }
    }

}
