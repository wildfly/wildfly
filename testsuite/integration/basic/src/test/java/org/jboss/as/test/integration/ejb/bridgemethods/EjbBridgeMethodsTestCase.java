/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.bridgemethods;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that bridge methods are correctly intercepted, and the correct method object is used in the invocation context
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
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
        Assert.assertEquals(1, (int) ejb.method(false));
        Assert.assertEquals(1, ((GenericInterface)ejb).method(false));
    }

    @Test
    public void testCdiInterceptedBridgeMethod() throws NamingException {
        ConcreteInterface ejb = (ConcreteInterface)new InitialContext().lookup("java:module/" + BridgeMethodEjb.class.getSimpleName() + "!" + ConcreteInterface.class.getName());
        Assert.assertEquals(1, (int)ejb.cdiMethod(false));
        Assert.assertEquals(1, ((GenericInterface) ejb).cdiMethod(false));
    }

}
