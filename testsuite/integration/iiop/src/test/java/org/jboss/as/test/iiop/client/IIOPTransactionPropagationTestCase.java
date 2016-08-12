/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.iiop.client;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.Status;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CORBA.SystemException;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IIOPTransactionPropagationTestCase {

    @ArquillianResource
    private static ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    public static final String DEFAULT_JBOSSAS = "iiop-server";
    private static final String ARCHIVE_NAME = "iiop-jts-ctx-propag-test";

    private static InitialContext context;

    @Deployment(name = ARCHIVE_NAME, managed = true)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> deploy() throws InvalidName, SystemException {
        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, ARCHIVE_NAME + ".jar")
                .addClass(IIOPTestBean.class)
                .addClass(IIOPTestBeanHome.class)
                .addClass(IIOPTestRemote.class)
                .addAsManifestResource(IIOPTransactionPropagationTestCase.class.getPackage(), "jboss-ejb3.xml",
                        "jboss-ejb3.xml");
        // File testPackage = new File("/tmp", ARCHIVE_NAME + ".jar");
        // jar.as(ZipExporter.class).exportTo(testPackage, true);
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws InvalidName, SystemException, NamingException {
        // // Orb presseting has to be done before the ORB is started to be used
        Util.presetOrb();
        context = Util.getContext();
    }

    @AfterClass
    public static void afterClass() throws NamingException {
        Util.tearDownOrb();
    }

    @Test
    public void testIIOPInvocation() throws Throwable {
        final Object iiopObj = context.lookup(IIOPTestBean.class.getSimpleName());
        final IIOPTestBeanHome beanHome = (IIOPTestBeanHome) PortableRemoteObject.narrow(iiopObj, IIOPTestBeanHome.class);
        final IIOPTestRemote bean = beanHome.create();

        try {
            Util.startCorbaTx();
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
            Assert.assertEquals("transaction-attribute-mandatory", bean.callMandatory());
            Util.commitCorbaTx();
        } catch (Throwable e) {
            // Util.rollbackCorbaTx();
            throw e;
        }
    }

    @Test
    public void testIIOPNeverCallInvocation() throws Throwable {
        final Object iiopObj = context.lookup(IIOPTestBean.class.getSimpleName());
        final IIOPTestBeanHome beanHome = (IIOPTestBeanHome) PortableRemoteObject.narrow(iiopObj, IIOPTestBeanHome.class);
        final IIOPTestRemote bean = beanHome.create();

        try {
            Util.startCorbaTx();
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
            bean.callNever();
            Assert.fail("Exception is supposed to be here thrown from TransactionAttribute.NEVER method");
        } catch (Exception e) {
            // this is OK - is expected never throwing that TS exists
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
        } finally {
            Util.rollbackCorbaTx();
        }
    }

    @Test
    public void testIIOPInvocationWithRollbackOnly() throws Throwable {
        final Object iiopObj = context.lookup(IIOPTestBean.class.getSimpleName());
        final IIOPTestBeanHome beanHome = (IIOPTestBeanHome) PortableRemoteObject.narrow(iiopObj, IIOPTestBeanHome.class);
        final IIOPTestRemote bean = beanHome.create();

        try {
            Util.startCorbaTx();
            Assert.assertEquals(Status.STATUS_ACTIVE, bean.transactionStatus());
            bean.callRollbackOnly();
            Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK, bean.transactionStatus());
        } finally {
            Util.rollbackCorbaTx();
        }
    }
}
