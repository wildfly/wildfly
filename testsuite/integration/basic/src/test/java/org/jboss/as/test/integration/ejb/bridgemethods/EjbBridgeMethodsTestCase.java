/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.bridgemethods;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that bridge methods are correctly intercepted, and the correct method object is used in the invocation context
 *
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class EjbBridgeMethodsTestCase {

    @Deployment
    public static JavaArchive depoy() {
        return ShrinkWrap.create(JavaArchive.class, "testBridgeMethods.jar")
                .addPackage(EjbBridgeMethodsTestCase.class.getPackage())
                .addAsManifestResource(new StringAsset("<beans><interceptors><class>" + CDIInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");

    }

    @Test
    public void testInterceptedBridgeMethod() throws NamingException {
        ConcreteInterface ejb = (ConcreteInterface)new InitialContext().lookup("java:module/" + BridgeMethodEjb.class.getSimpleName() + "!" + ConcreteInterface.class.getName());
        Assertions.assertEquals(1, (int) ejb.method(false));
        Assertions.assertEquals(1, ((GenericInterface)ejb).method(false));
    }

    @Test
    public void testCdiInterceptedBridgeMethod() throws NamingException {
        ConcreteInterface ejb = (ConcreteInterface)new InitialContext().lookup("java:module/" + BridgeMethodEjb.class.getSimpleName() + "!" + ConcreteInterface.class.getName());
        Assertions.assertEquals(1, (int)ejb.cdiMethod(false));
        Assertions.assertEquals(1, ((GenericInterface) ejb).cdiMethod(false));
    }

}
