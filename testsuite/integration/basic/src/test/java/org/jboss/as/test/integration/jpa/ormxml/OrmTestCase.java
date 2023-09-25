/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.ormxml;

import static org.junit.Assert.assertNull;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Transaction tests
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class OrmTestCase {

    private static final String ARCHIVE_NAME = "jpa_OrmTestCase";

    private static final String orm_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<entity-mappings xmlns=\"http://java.sun.com/xml/ns/persistence/orm\" version=\"2.0\">" +
                    "<entity class=\"org.jboss.as.test.integration.jpa.ormxml.Employee\"/>" +
                    "</entity-mappings>";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(OrmTestCase.class,
                Employee.class,
                SFSBCMT.class
        );

        jar.addAsManifestResource(OrmTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        jar.addAsResource(new StringAsset(orm_xml), "META-INF/orm.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Test
    public void testOrmXmlDefinedEmployeeEntity() throws Exception {
        SFSBCMT sfsbcmt = lookup("SFSBCMT", SFSBCMT.class);
        Employee emp = sfsbcmt.queryEmployeeName(1);
        assertNull("entity shouldn't exist", emp);

    }


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }
}
