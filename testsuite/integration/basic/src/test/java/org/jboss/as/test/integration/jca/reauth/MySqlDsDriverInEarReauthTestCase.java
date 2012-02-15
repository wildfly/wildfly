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
package org.jboss.as.test.integration.jca.reauth;

import static org.jboss.as.test.integration.jca.reauth.DsUtil.testConnection;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.annotation.Resource;
import org.junit.Ignore;

/**
 * Tests re-authentication support for MySQL data source. JDBC driver and DS configuration deployed inside .ear archive.
 * JBQA-5119,JBQA 5916.
 * 
 * @author Vladimir Rastseluev
 */

@RunWith(Arquillian.class)
@Ignore("JBQA-5119")
public class MySqlDsDriverInEarReauthTestCase {
    private static String user1 = "dsreauth1";
    private static String user2 = "dsreauth2";

    @Deployment
    public static Archive<?> deploy() throws Exception {

        
        File jdbcJar = new File(System.getProperty("jbossas.ts.integ.dir", "."),
                "/basic/src/test/resources/mysql-connector-java-5.1.15.jar");

        JavaArchive ja = ShrinkWrap.createFromZipFile(JavaArchive.class, jdbcJar);
        JavaArchive jt = ShrinkWrap.create(JavaArchive.class, "test.jar").addClasses(MySqlDsDriverInEarReauthTestCase.class,
                DsUtil.class);
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
                .addAsLibrary(ja)
                .addAsLibrary(jt)
                .addAsManifestResource("jca/mysql-ds.xml", "mysql-ds.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.ironjacamar.jdbcadapters\n"),
                        "MANIFEST.MF");
        return ear;
    }

    @Resource(mappedName = "java:jboss/UserTransaction")
    private UserTransaction trans;
    @Resource(mappedName = "java:jboss/datasources/MySqlDs")
    private DataSource ds;

    @Test
    public void testDataSourceDefinition() throws Exception {
        Connection conn = null;

        try {
            trans.begin();
            conn = ds.getConnection();
            testConnection(conn, "select user()", user1);
            conn.close();
            conn = ds.getConnection(user2, user2);
            testConnection(conn, "select user()", user2);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignore) {
                    // Ignore
                }
            }
            trans.commit();
        }

    }
}