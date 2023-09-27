/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.sibling;

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
 * EntityManagerFactory tests
 *
 * @author Zbynek Roubalik
 */
@RunWith(Arquillian.class)
public class SiblingXPCInheritanceTestCase {

    private static final String ARCHIVE_NAME = "jpa_SiblingXPCInheritanceTestCase";

    private static final String persistence_xml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                    "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">" +
                    "  <persistence-unit name=\"mypc\">" +
                    "    <description>Persistence Unit." +
                    "    </description>" +
                    "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
                    "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>" +
                    "</properties>" +
                    "  </persistence-unit>" +
                    "</persistence>";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(SiblingXPCInheritanceTestCase.class,
                SFSBTopLevel.class, Employee.class, DAO1.class, DAO2.class);

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        try {
            return interfaceType.cast(iniCtx.lookup(name));
        } catch (NamingException e) {
            throw e;
        }
    }

    /**
     * Test that EntityManagerFactory can be bind to specified JNDI name
     */
    @Test
    public void testSibling() throws Exception {
        SFSBTopLevel sfsb1 = lookup("SFSBTopLevel", SFSBTopLevel.class);
        sfsb1.createEmployee("SiblingTest", "1 home street", 123);
        sfsb1.testfunc();   // sibling xpc test
    }
}
