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
