/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.iiop.transaction.timeout;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingletonRemote;
import org.jboss.as.test.integration.transactions.TransactionTestLookupUtil;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingException;
import javax.transaction.TransactionRolledbackException;
import java.rmi.RemoteException;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Tests on transaction timeout behavior with SLSB beans
 * that is contacted via IIOP.
 */
@RunAsClient
@RunWith(Arquillian.class)
public class IIOPTimeoutTestCase {
    private static final int DEFAULT_IIOP_PORT = 3528;
    private static final String DEPLOYMENT_NAME = "stateless-iiop-txn-timeout";

    @ContainerResource("iiop-client")
    private ManagementClient mgmtClient;

    private TransactionCheckerSingletonRemote checker;

    @Deployment(name = DEPLOYMENT_NAME)
    @TargetsContainer("iiop-client")
    public static Archive<?> deploy() {
        System.setProperty("com.sun.CORBA.ORBUseDynamicStub", "true");
        return ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME + ".jar")
                .addPackage(IIOPTimeoutTestCase.class.getPackage())
                .addPackage(TxTestUtil.class.getPackage())
                .addClasses(TimeoutUtil.class)
                .addAsManifestResource(IIOPTimeoutTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("<beans></beans>"),  "beans.xml")
                // grant necessary permissions for -Dsecurity.manager
                .addAsResource(createPermissionsXmlAsset(
                    new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
    }

    @Before
    public void startUp() throws NamingException {
        if(checker == null) {
            checker = TransactionTestLookupUtil.lookupEjbStateless(mgmtClient.getMgmtAddress(), 8080,
                    DEPLOYMENT_NAME, TransactionCheckerSingleton.class, TransactionCheckerSingletonRemote.class);
        }
        checker.resetAll();
    }

    @Test
    public void noTimeoutStateless() throws Exception {
        TestBeanRemote bean = lookupStateless();

        bean.testTransaction();

        // synchronization is not called as we use stateless bean
        Assert.assertFalse("Synchronization after begin should not be called", checker.isSynchronizedBegin());
        Assert.assertFalse("Synchronization before completion should not be called", checker.isSynchronizedBefore());
        Assert.assertFalse("Synchronization after completion should not be called", checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources for each commit happened", 2, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * <p>
     * Remote ejb iiop call to SLSB where timeout happens which leads to transaction rollback.
     * There is subsequent call afterwards which starts different transaction which is committed.
     * <p>
     * We can see multiple exception in server log. See
     * <a href="https://access.redhat.com/solutions/645963">https://access.redhat.com/solutions/645963</a>
     */
    @Test
    public void timeoutStateless() throws Exception {
        TestBeanRemote bean = lookupStateless();

        try {
            bean.testTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            // should be here rather javax.ejb.EJBException?
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.transaction.TransactionRolledbackException.class, e.getClass());
        }

        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        // for stateless bean this should be ok
        bean.touch();

        // let's lookup the bean again and do a transaction work
        bean = lookupStateless();
        bean.testTransaction();

        Assert.assertEquals("Expecting two commits happened on XA resources", 2, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened last time when timeouted", 2, checker.getRolledback());
    }

    /**
     * EJB iiop call to SFSB where transaction is started and successfully committed.
     */
    @Test
    public void noTimeoutStateful() throws Exception {
        TestBeanRemote bean = lookupStateful();

        bean.testTransaction();

        Assert.assertTrue("Synchronization after begin should be called", checker.isSynchronizedBegin());
        Assert.assertTrue("Synchronization before completion should be called", checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called", checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources for each commit happened", 2, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * EJB iiop call to SFSB where transaction is started but txn timeout happens
     * and resources are rolled back.
     */
    @Test
    public void timeoutStateful() throws Exception {
        TestBeanRemote bean = lookupStateful();

        try {
            bean.testTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            // the exception is wrong, there should be javax.ejb.EJBException (or TransactionRolledbackException) here
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    TransactionRolledbackException.class, e.getClass());
        }

        Assert.assertEquals("Synchronization after begin should be called", 1, checker.countSynchronizedBegin());
        Assert.assertEquals("Synchronization before completion should not be called", 0, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion should be called", 1, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        // the second call on the same stateful bean
        bean.testTransaction();

        Assert.assertEquals("Synchronization after begin should be called again", 2, checker.countSynchronizedBegin());
        Assert.assertEquals("Synchronization before completion should be called", 1, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion should be called again", 2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting two commits happened on XA resources", 2, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened last time when timeouted", 2, checker.getRolledback());
    }

    private TestBeanRemote lookupStateless() throws NamingException, RemoteException {
        TestBeanHome beanHome = TransactionTestLookupUtil.lookupIIOP(mgmtClient.getMgmtAddress(), DEFAULT_IIOP_PORT,
                TestBeanHome.class, StatelessBean.class);
        return beanHome.create();
    }

    private TestBeanRemote lookupStateful() throws NamingException, RemoteException {
        TestBeanHome beanHome = TransactionTestLookupUtil.lookupIIOP(mgmtClient.getMgmtAddress(), DEFAULT_IIOP_PORT,
                TestBeanHome.class, StatefulBean.class);
        return beanHome.create();
    }
}
