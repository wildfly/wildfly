/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.jndi;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Ensure that JNDI handling works.
 *
 * @author Scott Marlow
 */
@RunWith(Arquillian.class)
public class JNDITestCase {

    private static final String ARCHIVE_NAME = "JNDITestCase";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(JNDITestCase.class.getPackage())
        .addAsManifestResource(JNDITestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private static InitialContext iniCtx;


    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testVersion() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        // tx1 will create the employee
        sfsb1.createEmployee("Sally", "1 home street", 1);
        assertTrue("Persistence.xml configured hibernate.jndi.class " +
                "was not used as InitialContextFactoryImpl.wasCalled() returned false", InitialContextFactoryImpl.wasCalled());
    }

}
