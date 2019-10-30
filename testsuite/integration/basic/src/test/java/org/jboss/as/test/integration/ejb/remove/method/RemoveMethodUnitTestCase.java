/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.remove.method;

import java.rmi.NoSuchObjectException;
import javax.ejb.EJBObject;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (ejbthree-786) to AS7 [JIRA JBQA-5483].
 * We want a remove action on a backing bean.
 *
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class RemoveMethodUnitTestCase {
    private static final String DD_BASED_MODULE_NAME = "remove-method-dd-test";
    private static final String ANNOTATION_BASED_MODULE_NAME = "remove-method-test";

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive ejb3Jar = ShrinkWrap.create(JavaArchive.class, ANNOTATION_BASED_MODULE_NAME + ".jar").addPackage(
                RemoveMethodUnitTestCase.class.getPackage());

        final JavaArchive ejb2Jar = ShrinkWrap.create(JavaArchive.class, DD_BASED_MODULE_NAME + ".jar").addClasses(Ejb21ViewDDBean.class,
                Ejb21View.class, Ejb21ViewHome.class, RemoveMethodUnitTestCase.class)
                .addAsManifestResource(RemoveMethodUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "remove-method-test.ear");
        ear.addAsModules(ejb2Jar, ejb3Jar);

        return ear;
    }

    @Test
    public void testRemoveStatefulRemote() throws Exception {
        RemoveStatefulRemote session = (RemoveStatefulRemote) ctx.lookup("java:app/" + ANNOTATION_BASED_MODULE_NAME + "/" + StatefulRemoveBean.class.getSimpleName() + "!" + RemoveStatefulRemote.class.getName());
        String result = session.remove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testRemoveStatelessRemote() throws Exception {
        RemoveStatelessRemote session = (RemoveStatelessRemote) ctx.lookup("java:app/" + ANNOTATION_BASED_MODULE_NAME + "/" + StatelessRemoveBean.class.getSimpleName() + "!" + RemoveStatelessRemote.class.getName());
        String result = session.remove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testRemoveStatefulLocalViaDelegate() throws Exception {
        Delegate session = (Delegate) ctx.lookup("java:app/" + ANNOTATION_BASED_MODULE_NAME + "/" + DelegateBean.class.getSimpleName() + "!" + Delegate.class.getName());
        String result = session.invokeStatefulRemove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testRemoveStatelessLocalViaDelegate() throws Exception {
        Delegate session = (Delegate) ctx.lookup("java:app/" + ANNOTATION_BASED_MODULE_NAME + "/" + DelegateBean.class.getSimpleName() + "!" + Delegate.class.getName());
        String result = session.invokeStatelessRemove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testExplicitExtensionEjbObjectInProxy() throws Exception {
        // Obtain stub
        Object obj = ctx.lookup("java:app/" + ANNOTATION_BASED_MODULE_NAME + "/" + Ejb21ViewBean.class.getSimpleName() + "!" + Ejb21ViewHome.class.getName());
        Ejb21ViewHome home = (Ejb21ViewHome) PortableRemoteObject.narrow(obj, Ejb21ViewHome.class);
        Ejb21View session = home.create();

        // Ensure EJBObject
        Assert.assertTrue(session instanceof EJBObject);

        // Cast and remove appropriately, ensuring removed
        boolean removed = false;
        String result = session.test();
        Assert.assertEquals(Ejb21ViewBean.TEST_STRING, result);
        session.remove();
        try {
            session.test();
        } catch (Exception e) {
            if (e instanceof NoSuchObjectException || e.getCause() instanceof NoSuchObjectException) {
                removed = true;
            } else {
                Assert.fail("Exception received, " + e + ", was not expected and should have had root cause of " + NoSuchObjectException.class);
            }
        }
        Assert.assertTrue("SFSB instance was not removed as expected", removed);

    }

    /**
     * Test that ejbRemove() method was invoked on the bean when the remove() is invoked on the EJBObject
     * of an EJB2.x view of a bean
     *
     * @throws Exception
     */
    @Test
    public void testEjbRemoveInvokedOnRemoval() throws Exception {
        // Obtain stub
        Object obj = ctx.lookup("java:app/" + DD_BASED_MODULE_NAME + "/" + Ejb21ViewDDBean.class.getSimpleName() + "!" + Ejb21ViewHome.class.getName());
        Ejb21ViewHome home = (Ejb21ViewHome) PortableRemoteObject.narrow(obj, Ejb21ViewHome.class);
        Ejb21View bean = home.create();

        // Ensure EJBObject
        Assert.assertTrue(bean instanceof EJBObject);

        String result = bean.test();

        bean.remove();
        try {
            bean.test();
            Assert.fail("Invocation on a removed bean was expected to fail");
        } catch (NoSuchObjectException e) {
            // expected
        }
        final RemoveMethodInvocationTracker ejbRemoveMethodInvocationTracker = (RemoveMethodInvocationTracker) ctx.lookup("java:app/" + ANNOTATION_BASED_MODULE_NAME + "/" + RemoveMethodInvocationTrackerBean.class.getSimpleName() + "!" + RemoveMethodInvocationTracker.class.getName());
        Assert.assertTrue("ejbRemove() method was not invoked after bean removal", ejbRemoveMethodInvocationTracker.wasEjbRemoveCallbackInvoked());
    }
}
