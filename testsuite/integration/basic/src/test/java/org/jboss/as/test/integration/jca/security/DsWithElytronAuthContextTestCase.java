/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.security;

import static org.junit.Assert.assertNotNull;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.MatchRules;
import org.wildfly.test.security.common.elytron.SimpleAuthConfig;
import org.wildfly.test.security.common.elytron.SimpleAuthContext;

/**
 * Data source with security domain test JBQA-5952
 *
 * @author <a href="mailto:vrastsel@redhat.com"> Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(DsWithElytronAuthContextTestCase.ElytronSetup.class)
public class DsWithElytronAuthContextTestCase {

    private static final String AUTH_CONFIG = "MyAuthConfig";
    private static final String AUTH_CONTEXT = "MyAuthContext";
    private static final String DATABASE_USER = "elytron";
    private static final String DATABASE_PASSWORD = "passWD12#$";
    private static final String DATASOURCE_NAME = "ElytronDSTest";

    static class ElytronSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            final CredentialReference credRefPwd = CredentialReference.builder().withClearText(DATABASE_PASSWORD).build();
            final ConfigurableElement authenticationConfiguration = SimpleAuthConfig.builder().withName(AUTH_CONFIG)
                    .withAuthenticationName(DATABASE_USER).withCredentialReference(credRefPwd).build();
            final MatchRules matchRules = MatchRules.builder().withAuthenticationConfiguration(AUTH_CONFIG).build();
            final ConfigurableElement authenticationContext = SimpleAuthContext.builder().withName(AUTH_CONTEXT).
                    withMatchRules(matchRules).build();

            return new ConfigurableElement[] {authenticationConfiguration, authenticationContext};
        }
    }

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar").addClasses(DsWithElytronAuthContextTestCase.class);
        jar.addClasses(AbstractElytronSetupTask.class);
        return ShrinkWrap.create(EnterpriseArchive.class, "test.ear").addAsLibrary(jar)
                .addAsManifestResource(DsWithSecurityDomainTestCase.class.getPackage(), "security-ds-elytron.xml", "security-ds.xml");
    }

    @ArquillianResource
    private InitialContext ctx;

    @Test
    public void deploymentTest() throws Exception {
        DataSource ds = (DataSource) ctx.lookup("java:jboss/datasources/" + DATASOURCE_NAME);
        Connection con = null;
        try {
            con = ds.getConnection();
            assertNotNull(con);

        } finally {
            if (con != null) { con.close(); }
        }
    }
}
