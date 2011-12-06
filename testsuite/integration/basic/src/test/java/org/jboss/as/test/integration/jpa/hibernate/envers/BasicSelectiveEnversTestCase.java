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
package org.jboss.as.test.integration.jpa.hibernate.envers;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Madhumita Sadhukhan
 */
@RunWith(Arquillian.class)
public class BasicSelectiveEnversTestCase {
    private static final String ARCHIVE_NAME = "jpa_BasicSelectiveEnversTestCase";

    private static final String persistence_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
            + "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">"
            + "  <persistence-unit name=\"myOrg\">" + "    <description>Persistence Unit." + "    </description>"
            + "    <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" + "    <properties> "
            + "      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" + "    </properties>"
            + "  </persistence-unit>" + "</persistence>";

    @ArquillianResource
    private static InitialContext iniCtx;

    @BeforeClass
    public static void beforeClass() throws NamingException {
        iniCtx = new InitialContext();
    }

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(Organization.class, SLSBOrg.class);
        jar.add(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }

    protected static <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType
                .cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testSelectiveEnversOperations() throws Exception {

        SLSBOrg slsbOrg = lookup("SLSBOrg", SLSBOrg.class);

        Organization o1 = slsbOrg.createOrg("REDHAT", "Software Co", "10/10/1994", "eternity", "Raleigh");
        Organization o2 = slsbOrg.createOrg("HALDIRAMS", "Food Co", "10/10/1974", "eternity", "Delhi");
        o2.setStartDate("10/10/1924");
        o2.setName("BIKANER");

        slsbOrg.updateOrg(o2);
        o1.setStartDate("10/10/1924");

        slsbOrg.updateOrg(o1);
        slsbOrg.deleteOrg(o1);
        testSelectiveEnversOperationonAuditedandNonAuditedProperty(o2, slsbOrg);
        testSelectiveEnversOperationonFetchbyEntityName(o2, slsbOrg);
        testEnversOperationonDelete(o1, slsbOrg);
    }

    public void testSelectiveEnversOperationonAuditedandNonAuditedProperty(Organization o2, SLSBOrg slsbOrg) throws Exception {

        Organization ret1 = slsbOrg.retrieveOldOrgbyId(o2.getId());
        // check that property startDate is audited
        Assert.assertEquals("10/10/1974", ret1.getStartDate());
        Assert.assertEquals("HALDIRAMS", ret1.getName());
        // check that property location is notaudited
        Assert.assertNull(ret1.getLocation());

    }

    public void testSelectiveEnversOperationonFetchbyEntityName(Organization o2, SLSBOrg slsbOrg) throws Exception {

        Organization ret1 = slsbOrg.retrieveOldOrgbyEntityName(Organization.class.getName(), o2.getId());
        // check that fetch by Entityname works
        Assert.assertNotNull(ret1.getName());
        Assert.assertEquals("BIKANER", ret1.getName());

    }

    public void testEnversOperationonDelete(Organization o1, SLSBOrg slsbOrg) throws Exception {

        Organization ret1 = slsbOrg.retrieveDeletedOrgbyId(o1.getId());
        // check that revisions of deleted entity can be retrieved
        Assert.assertNotNull(ret1);

    }

}
