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

package org.jboss.as.testsuite.integration.jpa.hibernate;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Hibernate session factory tests
 * will need to use org.hibernate:hibernate-infinispan
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class SecondLevelCacheTestCase {

    private static final String ARCHIVE_NAME = "jpa_SecondLevelCacheTestCase";

    private static final String persistence_xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
            "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
            "  <persistence-unit name=\"mypc\">" +
            "    <description>Persistence Unit." +
            "    </description>" +
            "  <jta-data-source>java:/H2DS</jta-data-source>" +
            " <shared-cache-mode>ENABLE_SELECTIVE</shared-cache-mode>" +
            "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
            "<property name=\"hibernate.show_sql\" value=\"true\"/>" +
            "<property name=\"hibernate.cache.use_second_level_cache\" value=\"true\"/>" +
            "<property name=\"hibernate.cache.use_query_cache\" value=\"false\"/>" +
            "<property name=\"hibernate.cache.region.factory_class\" value=\"org.hibernate.cache.infinispan.JndiInfinispanRegionFactory\"/>"+
            "<property name=\"hibernate.cache.infinispan.cachemanager\" value=\"java:jboss/infinispan/hibernate\"/>"+
            "</properties>" +
            "  </persistence-unit>" +
            "</persistence>";

    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(SecondLevelCacheTestCase.class,
            Employee.class,
            SFSB1.class,
            SFSBHibernateSession.class,
            SFSBHibernateSessionFactory.class
        );

        jar.addResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected static <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    @Test
    public void testMultipleNonTXTransactionalEntityManagerInvocations() throws Exception {
        Exception error = null;
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        sfsb1.createEmployee("Kelly Smith", "Watford, England", 10);
        sfsb1.createEmployee("Alex Scott", "London, England", 20);
        sfsb1.getEmployeeNoTX(10);
        sfsb1.getEmployeeNoTX(20);

        DataSource ds = rawLookup("java:/H2DS", DataSource.class);
        Connection conn = ds.getConnection();
        int deleted = conn.prepareStatement("delete from Employee").executeUpdate();

        // verify that delete worked (or test is invalid)
        assertTrue("was able to delete added rows.  delete count=" + deleted, deleted > 1);

        // read deleted data from second level cache
        Employee emp = sfsb1.getEmployeeNoTX(10);

        assertTrue("was able to read deleted database row from second level cache", emp != null);
    }

}
