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
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
    private static final Logger log = Logger.getLogger(RemoveMethodUnitTestCase.class);

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "remove-method-test.jar").addPackage(
                RemoveMethodUnitTestCase.class.getPackage());
        log.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testRemoveStatefulRemote() throws Exception {
        RemoveStatefulRemote session = (RemoveStatefulRemote) ctx.lookup("java:module/" + StatefulRemoveBean.class.getSimpleName() + "!" + RemoveStatefulRemote.class.getName());
        String result = session.remove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testRemoveStatelessRemote() throws Exception {
        RemoveStatelessRemote session = (RemoveStatelessRemote) ctx.lookup("java:module/" + StatelessRemoveBean.class.getSimpleName() + "!" + RemoveStatelessRemote.class.getName());
        String result = session.remove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testRemoveStatefulLocalViaDelegate() throws Exception {
        Delegate session = (Delegate) ctx.lookup("java:module/" + DelegateBean.class.getSimpleName() + "!" + Delegate.class.getName());
        String result = session.invokeStatefulRemove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testRemoveStatelessLocalViaDelegate() throws Exception {
        Delegate session = (Delegate) ctx.lookup("java:module/" + DelegateBean.class.getSimpleName() + "!" + Delegate.class.getName());
        String result = session.invokeStatelessRemove();
        Assert.assertEquals(AbstractRemoveBean.RETURN_STRING, result);
    }

    @Test
    public void testExplicitExtensionEjbObjectInProxy() throws Exception {
        // Obtain stub
        Object obj = ctx.lookup("java:module/" + Ejb21ViewBean.class.getSimpleName() + "!" + Ejb21ViewHome.class.getName());
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
}
