/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.epcpropagation.hierarchy;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * AS7-1121 Tests that Jakarta Persistence interceptors are correctly registered if injection is done into the superclass of a SFSB
 * <p>
 * TODO: AS7-1120 Also tests that persistence context inheritance (Jakarta Persistence 7.6.2.1) works correctly when injecting one SFSB into another directly
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EPCPropagationHierarchyTestCase {
    private static final String ARCHIVE_NAME = "epc-hierarchy-propagation";

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(EPCPropagationHierarchyTestCase.class.getPackage());
        jar.addAsManifestResource(EPCPropagationHierarchyTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void testPersistenceContextPropagation() throws Exception {
        ChildStatefulBean stateful = lookup("ChildStatefulBean", ChildStatefulBean.class);
        stateful.testPropagation();
    }


}
