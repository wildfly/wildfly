/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.loginmodules;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.AddRoleLoginModule;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.JSSE;
import org.jboss.as.test.integration.security.common.config.SecureStore;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.integration.security.common.servlets.PrincipalPrintingServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.logging.Logger;
import org.jboss.security.auth.spi.CertRolesLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link CertRolesLoginModule} which uses truststore with trusted
 * certificates for authentication of users and property file with users roles
 * for authorization.
 * 
 * @author Filip Bogyai
 */
@RunWith(Arquillian.class)
@ServerSetup({ AbstractCertificateLoginModuleTestCase.HTTPSConnectorSetup.class, //
        CertificateRolesLoginModuleTestCase.SecurityDomainsSetup.class //
})
@RunAsClient
public class CertificateRolesLoginModuleTestCase extends AbstractCertificateLoginModuleTestCase {

    private static Logger LOGGER = Logger.getLogger(CertificateRolesLoginModuleTestCase.class);

    private static final String APP_NAME = "certificate_roles";
    private static final String SECURITY_DOMAIN_CERT = "certificate_roles_domain";
    private static final String SECURITY_DOMAIN_JSSE = "jsse_truststore_domain";

    private static final String ROLES = "CN\\=client" + "=" + SimpleSecuredServlet.ALLOWED_ROLE + "\n" + "CN\\=untrusted=testRole";

    @Deployment(name = APP_NAME)
    public static WebArchive deployment() {
        LOGGER.info("Start deployment " + APP_NAME);
        final WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war");
        war.addClasses(AddRoleLoginModule.class, SimpleServlet.class, SimpleSecuredServlet.class, PrincipalPrintingServlet.class);
        war.addAsWebInfResource(DatabaseCertLoginModuleTestCase.class.getPackage(), "web-cert-authn.xml", "web.xml");
        war.addAsWebInfResource(Utils.getJBossWebXmlAsset(SECURITY_DOMAIN_CERT), "jboss-web.xml");
        war.addAsResource(new StringAsset(ROLES), "roles.properties");

        return war;
    }

    /**
     * Test authentication against application which uses security domain with
     * configured {@link CertRolesLoginModule}.
     * 
     */
    @Test
    public void testDatabaseCertLoginModule() throws Exception {

        testLoginWithCertificate(APP_NAME);
    }

    // Embedded class ------------------------------------------------------

    /**
     * A {@link ServerSetupTask} instance which creates security domains for
     * this test case.
     * 
     * @author Filip Bogyai
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        /**
         * Returns SecurityDomains configuration for this testcase.
         * 
         * @see org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask#getSecurityDomains()
         */
        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_CERT)
                    .loginModules(
                            new SecurityModule.Builder().name(CertRolesLoginModule.class.getName())
                                    .putOption("securityDomain", SECURITY_DOMAIN_JSSE).putOption("password-stacking", "useFirstPass")
                                    .putOption("rolesProperties", "roles.properties").build()).build();
            final SecurityDomain sdJsse = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_JSSE)
                    .jsse(new JSSE.Builder().trustStore(
                            new SecureStore.Builder().type("JKS").url(SERVER_TRUSTSTORE_FILE.toURI().toURL())
                                    .password(SecurityTestConstants.KEYSTORE_PASSWORD).build()) //
                            .build()) //
                    .build();

            return new SecurityDomain[] { sdJsse, sd };
        }
    }

}
