/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
