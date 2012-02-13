/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.smoke.embedded.deployment.ds;

import static org.jboss.as.test.smoke.embedded.deployment.ds.DsUtil.testConnection;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Ignore;

/**
 * Tests an ability to deploy *-ds.xml datasource definition JBQA-5872
 * 
 * @author Vladimir Rastseluev
 */

@RunWith(Arquillian.class)
public class DsDeploymentTestCase {
    private String user1 = "SA";

    @Deployment
    public static Archive<?> deploy() throws Exception {

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "test.jar")
            .addClasses(DsDeploymentTestCase.class, DsUtil.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
            .addAsLibrary(ja)
            .addAsManifestResource("ds/h2-ds.xml", "h2-ds.xml");
        return ear;
    }

    @ArquillianResource
    private InitialContext ctx;

    @Test
    public void testDataSourceDefinition() throws Exception {

        DataSource ds = (DataSource) ctx.lookup("java:jboss/datasources/DDs");
        Connection conn = null;

        try {
            conn = ds.getConnection();
            testConnection(conn, "select current_user()", user1);

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignore) {
                    // Ignore
                }

            }
        }
    }
}
