/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.view.basic;

import java.io.Externalizable;
import java.io.Serializable;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;

/**
 * Tests the number of views exposed by EJBs are correct
 *
 * @author Jaikiran Pai
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
public class BusinessViewAnnotationProcessorTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-view-test.jar");
        jar.addPackage(MyBean.class.getPackage());
        return jar;
    }

    /**
     * Tests a bean marked with an explicit view
     *
     * @throws Exception
     */
    @Test
    public void testBeanWithExplicitView() throws Exception {
        final Context ctx = new InitialContext();
        final MyInterface singleView = (MyInterface) ctx.lookup("java:module/" + MyBean.class.getSimpleName());
        assertNotNull("View " + MyInterface.class.getName() + " not found", singleView);

        final MyInterface myInterfaceView = (MyInterface) ctx.lookup("java:module/" + MyBean.class.getSimpleName() + "!" + MyInterface.class.getName());
        assertNotNull("View " + MyInterface.class.getName() + " not found", myInterfaceView);
    }

    /**
     * Tests a bean which has an implicit local business interface
     *
     * @throws Exception
     */
    @Test
    public void testImplicitLocalBusinessInterface() throws Exception {
        final Context ctx = new InitialContext();
        final MyInterface singleView = (MyInterface) ctx.lookup("java:module/" + ImplicitLocalBusinessInterfaceBean.class.getSimpleName());
        assertNotNull("View " + MyInterface.class.getName() + " not found", singleView);

        final MyInterface myInterfaceView = (MyInterface) ctx.lookup("java:module/" + ImplicitLocalBusinessInterfaceBean.class.getSimpleName() + "!" + MyInterface.class.getName());
        assertNotNull("View " + MyInterface.class.getName() + " not found", myInterfaceView);
    }

    /**
     * Tests a bean which has an implicit no-interface view
     *
     * @throws Exception
     */
    @Test
    public void testImplicitNoInterface() throws Exception {
        final Context ctx = new InitialContext();
        final ImplicitNoInterfaceBean singleView = (ImplicitNoInterfaceBean) ctx.lookup("java:module/" + ImplicitNoInterfaceBean.class.getSimpleName());
        assertNotNull("View " + ImplicitNoInterfaceBean.class.getName() + " not found", singleView);

        final ImplicitNoInterfaceBean noInterfaceBean = (ImplicitNoInterfaceBean) ctx.lookup("java:module/" + ImplicitNoInterfaceBean.class.getSimpleName() + "!" + ImplicitNoInterfaceBean.class.getName());
        assertNotNull("View " + MyInterface.class.getName() + " not found", noInterfaceBean);
    }


    /**
     * Tests a bean which has an implicit local business interface
     *
     * @throws Exception
     */
    @Test
    public void testInvocationOnNonPublicMethod() throws Exception {
        final Context ctx = new InitialContext();
        final ImplicitNoInterfaceBean singleView = (ImplicitNoInterfaceBean) ctx.lookup("java:module/" + ImplicitNoInterfaceBean.class.getSimpleName());
        assertNotNull("View " + MyInterface.class.getName() + " not found", singleView);
        Assert.assertEquals("Hello", singleView.sayHello());
        try {
            singleView.sayGoodbye();
            Assert.fail("should have been disallowed");
        } catch (EJBException expected) {

        }
    }


    /**
     * Tests that if a bean has a {@link javax.ejb.Remote} annotation without any specific value and if the bean implements n (valid) interfaces, then all those n (valid) interfaces are considered
     * as remote business interfaces
     *
     * @throws Exception
     */
    @Test
    public void testEJB32MultipleRemoteViews() throws Exception {
        final Context ctx = new InitialContext();

        final One interfaceOne = (One) ctx.lookup("java:module/" + MultipleRemoteViewBean.class.getSimpleName() + "!" + One.class.getName());
        assertNotNull("View " + One.class.getName() + " not found", interfaceOne);

        final Two interfaceTwo = (Two) ctx.lookup("java:module/" + MultipleRemoteViewBean.class.getSimpleName() + "!" + Two.class.getName());
        assertNotNull("View " + Two.class.getName() + " not found", interfaceTwo);


        final Three interfaceThree = (Three) ctx.lookup("java:module/" + MultipleRemoteViewBean.class.getSimpleName() + "!" + Three.class.getName());
        assertNotNull("View " + Three.class.getName() + " not found", interfaceThree);

        try {
            final Object view = ctx.lookup("java:module/" + MultipleRemoteViewBean.class.getSimpleName() + "!" + Serializable.class.getName());
            Assert.fail("Unexpected view found: " + view + " for interface " + Serializable.class.getName());
        } catch (NameNotFoundException nnfe) {
            // expected
        }

        try {
            final Object view = ctx.lookup("java:module/" + MultipleRemoteViewBean.class.getSimpleName() + "!" + Externalizable.class.getName());
            Assert.fail("Unexpected view found: " + view + " for interface " + Externalizable.class.getName());
        } catch (NameNotFoundException nnfe) {
            // expected
        }
    }

    /**
     * Tests that if a bean has a {@link javax.ejb.Local} annotation without any specific value and if the bean implements n (valid) interfaces, then all those n (valid) interfaces are considered
     * as local business interfaces
     *
     * @throws Exception
     */
    @Test
    public void testEJB32MultipleLocalViews() throws Exception {
        final Context ctx = new InitialContext();

        final One interfaceOne = (One) ctx.lookup("java:module/" + MultipleLocalViewBean.class.getSimpleName() + "!" + One.class.getName());
        assertNotNull("View " + One.class.getName() + " not found", interfaceOne);

        final Two interfaceTwo = (Two) ctx.lookup("java:module/" + MultipleLocalViewBean.class.getSimpleName() + "!" + Two.class.getName());
        assertNotNull("View " + Two.class.getName() + " not found", interfaceTwo);


        final Three interfaceThree = (Three) ctx.lookup("java:module/" + MultipleLocalViewBean.class.getSimpleName() + "!" + Three.class.getName());
        assertNotNull("View " + Three.class.getName() + " not found", interfaceThree);

        try {
            final Object view = ctx.lookup("java:module/" + MultipleLocalViewBean.class.getSimpleName() + "!" + Serializable.class.getName());
            Assert.fail("Unexpected view found: " + view + " for interface " + Serializable.class.getName());
        } catch (NameNotFoundException nnfe) {
            // expected
        }

        try {
            final Object view = ctx.lookup("java:module/" + MultipleLocalViewBean.class.getSimpleName() + "!" + Externalizable.class.getName());
            Assert.fail("Unexpected view found: " + view + " for interface " + Externalizable.class.getName());
        } catch (NameNotFoundException nnfe) {
            // expected
        }
    }

}
