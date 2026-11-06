/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.requiresnew;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the propagation rules for Extended Persistence Contexts respect nestled transactions.
 * <p>
 * As a transaction scoped EM has already been bound to the transaction, if REQUIRES_NEW is not used then
 * an exception will be thrown by the SFSB with the EPC.
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EPCPropagationNewTransactionTestCase {
    private static final String ARCHIVE_NAME = "epc-hierarchy-propagation";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(EPCPropagationNewTransactionTestCase.class.getPackage());
        jar.addAsManifestResource(EPCPropagationNewTransactionTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    @Order(1)
    public void testRequiresNewXPCPropagation() throws Exception {
        BikeManagerBean stateful = lookup(BikeManagerBean.class.getSimpleName(), BikeManagerBean.class);
        stateful.runTest();
    }

    @Test
    @Order(2)
    public void testXPCIsAssociatedWithTX() throws Exception {
        BikeManagerBean stateful = lookup(BikeManagerBean.class.getSimpleName(), BikeManagerBean.class);
        stateful.runTest2();
    }

}
