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

import static org.jboss.as.test.integration.ejb.security.SecurityTest.*;
import static junit.framework.Assert.*;

import java.sql.Connection;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Data source with security domain test JBQA-5952
 * 
 * @author <a href="mailto:vrastsel@redhat.com"> Vladimir Rastseluev</a>
 *
 */
@RunWith(Arquillian.class)
@Ignore("AS7-3923")
public class DsWithSecurityDomainTestCase {

    @Deployment
    public static Archive<?> deployment() {
        try {
            createSecurityDomain("DsRealm");
        } catch (Exception e) {
            e.printStackTrace();
        }

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "single.jar").addClasses(
                DsWithSecurityDomainTestCase.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear").addAsLibrary(jar)
                .addAsResource(DsWithSecurityDomainTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(DsWithSecurityDomainTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsManifestResource("jca/security/data-sources/security-ds.xml", "security-ds.xml");

        return ear;
    }

    @AfterClass
    public static void tearDown() throws Exception {
        removeSecurityDomain("DsRealm");
    }

    @ArquillianResource
    private InitialContext ctx;

    @Test
    public void deploymentTest() throws Exception {
        DataSource ds = (DataSource) ctx.lookup("java:jboss/datasources/securityDs");
        Connection con = null;
        try {
            con = ds.getConnection();
            assertNotNull(con);

        } finally {
            if (con != null)
                con.close();
        }
    }
}
