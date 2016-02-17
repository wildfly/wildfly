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

package org.jboss.as.test.integration.ejb.transaction.cmt.timeout.xa;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.transaction.utils.SingletonChecker;
import org.jboss.as.test.integration.ejb.transaction.utils.SingletonCheckerRemote;
import org.jboss.as.test.integration.ejb.transaction.utils.TestLookupUtil;
import org.jboss.as.test.integration.ejb.transaction.utils.TxTestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import javax.ejb.NoSuchEJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Tests on transaction timeout behavior with Stateful beans.
 * It's checked if synchronization is called correctly and
 * if transaction is aborted.
 */
@RunWith(Arquillian.class)
@Ignore("WFLY-6212")
public class StatefulTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    private SingletonCheckerRemote checker;
    

    @Deployment
    public static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "stateful-txn-timeout.jar")
            .addPackage(StatefulTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addAsManifestResource(new StringAsset("<beans></beans>"),  "beans.xml");
        return jar;
    }

    @Before
    public void startUp() throws NamingException {
        this.checker = TestLookupUtil.lookupModule(initCtx, SingletonChecker.class);
        checker.resetAll();
    }

    // --------------------------------------------------------
    // --------------- SUNNY SCENARIO - NO TIMEOUT ------------
    // --------------------------------------------------------
    @Test
    public void noTimeout() throws Exception {
        
        StatefulBean bean = TestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertTrue("Synchronization before completion has to be called",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion has to be called",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources for each commit happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }
    
    @Test
    public void noTimeoutWithAnnotation() throws Exception {
        StatefulWithAnnotationBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        bean.testTransaction();
        bean.testTransaction();
        
        Assert.assertEquals("Synchronization after begin has to be called", 2, checker.countSynchronizedBegin());
        Assert.assertEquals("Synchronization before completion has to be called", 2, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion has to be called", 2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources for each commit happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }
    
    @Test
    public void noTimeoutWithAnnotationAndInterface() throws Exception {
        StatefulWithAnnotationAndInterfaceBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationAndInterfaceBean.class);
        bean.testTransaction();
        bean.testTransaction();

        Assert.assertEquals("Synchronization after begin has to be called", 2, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization before completion has to be called", 2, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion has to be called", 2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting a XA resource committed for each test call", 2, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }
    
    @Test
    public void noTimeoutWithRegistry() throws Exception {
        StatefulWithRegistryBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithRegistryBean.class);
        bean.testTransaction();
        bean.testTransaction();
        
        Assert.assertTrue("Synchronization before completion has to be called",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion has to be called",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting two XA resources for each commit happened", 4, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }


    // --------------------------------------------------------
    // ---------------- SCENARIOS WITH TIMEOUT ----------------
    // --------------------------------------------------------
    @Test
    public void timeout() throws Exception {
        StatefulBean bean = TestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        try {
            bean.testTransactionTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            // 14.3.7 Exceptions from Other Container-invoked Callbacks
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    @Test
    public void timeoutJtaSynchro() throws Exception {
        StatefulBean bean = TestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        try {
            bean.testTransactionTimeoutWithSynchronization();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            // 14.3.7:  Exceptions from Other Container-invoked Callbacks
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }

        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());

        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }
    
    @Test
    public void rollbackOnly() throws Exception {
        StatefulBean bean = TestLookupUtil.lookupModule(initCtx, StatefulBean.class);
        try {
            bean.testTransactionRollbackOnly();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            // 14.3.7:  Exceptions from Other Container-invoked Callbacks
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting one rollback happened on XA resource as TM was directed to rollback "
                + "just after first resource was enlisted", 1, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    @Test
    public void timeoutWithAnnotation() throws Exception {
        StatefulWithAnnotationBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        try {
            bean.testTransactionTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }
    
    @Test
    public void timeoutWithAnnotationAddingJtaSynchro() throws Exception {
        StatefulWithAnnotationBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        try {
            bean.testTransactionTimeoutSynchroAdded();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertEquals("Synchronization before completion should not be called as rollback happened",
                0, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion should be called twice",
                2, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    @Test
    public void rollbackOnlyWithAnnotation() throws Exception {
        StatefulWithAnnotationBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithAnnotationBean.class);
        try {
            bean.testTransactionRollbackOnly();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            // 14.3.7:  Exceptions from Other Container-invoked Callbacks
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting one rollback happened on XA resource as TM was directed to rollback "
                + "just after first resource was enlisted", 1, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }

    @Test
    public void timeoutWithRegistry() throws Exception {
        StatefulWithRegistryBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithRegistryBean.class);
        try {
            bean.testTransactionTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }
    
    @Test
    public void timeoutWithRegistryAddingJtaSynchro() throws Exception {
        StatefulWithRegistryBean bean = TestLookupUtil.lookupModule(initCtx, StatefulWithRegistryBean.class);
        try {
            bean.testTransactionWithSynchronizationTimeout();
            Assert.fail("Excpected rollback exception being thrown");
        } catch (Exception e) {
            Assert.assertEquals("Expecting rollback happened and transaction rollback exception being thrown",
                    javax.ejb.EJBException.class, e.getClass());
        }
        
        Assert.assertFalse("Synchronization before completion should not be called as rollback happened",
                checker.isSynchronizedBefore());
        Assert.assertTrue("Synchronization after completion should be called even when rollback happened",
                checker.isSynchronizedAfter());
        Assert.assertEquals("Expecting no commit happened on any XA resource", 0, checker.getCommitted());
        Assert.assertEquals("Expecting two rollback happened on XA resources", 2, checker.getRolledback());
        
        try {
            bean.touch();
            Assert.fail("Expecting NoSuchEjbException as bean was discarded by rollback");
        } catch (NoSuchEJBException expected) {
            // this is highly expected
        }
    }
}
