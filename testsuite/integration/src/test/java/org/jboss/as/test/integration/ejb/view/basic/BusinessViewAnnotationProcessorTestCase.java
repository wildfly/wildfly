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

import static org.junit.Assert.assertNotNull;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the number of views exposed by EJBs are correct
 *
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
     * Tests a bean marked with a explicit view
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

}
