/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.spec.ejb3;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.CallTrackerSingletonBean;
import org.jboss.as.demos.ejb3.archive.SimpleSingletonBean;
import org.jboss.as.demos.ejb3.archive.SimpleSingletonLocal;
import org.jboss.as.demos.ejb3.archive.StartupSingleton;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.EJB;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Testcase for testing the basic functionality of a EJB3 singleton session bean.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class SingletonBeanTestCase {

    private static final Logger log = Logger.getLogger(SingletonBeanTestCase.class.getName());

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-singleton-bean-example.jar");
        jar.addPackage(SimpleSingletonBean.class.getPackage());
        jar.addPackage(StartupSingleton.class.getPackage());
        jar.addClass(ReadOnlySingletonBean.class);
        jar.addClass(LongWritesSingletonBean.class);
        jar.addClass(SingletonBeanTestCase.class);
        log.info(jar.toString(true));
        return jar;
    }

    @EJB(mappedName = "java:global/test/SimpleSingletonBean!org.jboss.as.demos.ejb3.archive.SimpleSingletonLocal")
    private SimpleSingletonLocal singletonBean;

    @EJB(mappedName = "java:global/test/CallTrackerSingletonBean!org.jboss.as.demos.ejb3.archive.CallTrackerSingletonBean")
    private CallTrackerSingletonBean callTrackerSingletonBean;

    @EJB(mappedName = "java:global/test/ReadOnlySingletonBean!org.jboss.as.test.spec.ejb3.ReadOnlySingletonBean")
    private ReadOnlySingletonBean readOnlySingletonBean;

    @EJB(mappedName = "java:global/test/LongWritesSingletonBean!org.jboss.as.test.spec.ejb3.LongWritesSingletonBean")
    private LongWritesSingletonBean longWritesSingletonBean;

    /**
     * Test a basic invocation on @Singleton bean
     *
     * @throws Exception
     */
    @Test
    public void testSingletonEJB() throws Exception {
        final int NUM_TIMES = 10;
        for (int i = 0; i < NUM_TIMES; i++) {
            singletonBean.increment();
        }
        Assert.assertEquals("Unexpected count returned from singleton bean", NUM_TIMES, singletonBean.getCount());

    }

    /**
     * Tests that a @Startup @Singleton bean is inited when the deployment is completed.
     *
     * @throws Exception
     */
    @Test
    public void testStartupSingleton() throws Exception {
        Assert.assertTrue("@Startup singleton bean was not created", callTrackerSingletonBean.wasStartupSingletonBeanCreated());
    }

    /**
     * Tests that the concurrency on a singleton bean with lock type READ works as expected
     *
     * @throws Exception
     */
    @Test
    public void testReadOnlySingleton() throws Exception {
        final int NUM_THREADS = 10;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        @SuppressWarnings("unchecked")
        Future<String>[] results = new Future[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            results[i] = executor.submit(new ReadOnlySingletonBeanInvoker(readOnlySingletonBean, i));
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            String result = results[i].get(10, TimeUnit.SECONDS);
            Assert.assertEquals("Unexpected value from singleton bean", String.valueOf(i), result);
        }
    }

    /**
     * Tests that invocation on a singleton bean method with write lock results in ConcurrentAccessTimeoutException
     * for subsequent invocations, if the previous invocation(s) hasn't yet completed.
     *
     * @throws Exception
     */
    @Test
    public void testLongWritesSingleton() throws Exception {

        // let's invoke a bean method (with WRITE lock semantics) which takes a long time to complete
        final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        final Future<?> firstInvocationResult = singleThreadExecutor.submit(new LongWritesSingletonBeanInvoker(this.longWritesSingletonBean));

        // let's now try and invoke on this bean while the previous operation is in progress.
        // we expect a ConcurrentAccessTimeoutException
        final int NUM_THREADS = 10;
        final ExecutorService nextTenInvocations = Executors.newFixedThreadPool(NUM_THREADS);
        Future<?>[] results = new Future[NUM_THREADS];
        // let the 10 threads invoke on the bean's method (which has WRITE lock semantics) which has a accesstimeout value
        // set on it
        for (int i = 0; i < NUM_THREADS; i++) {
            results[i] = nextTenInvocations.submit(new LongWritesSingletonBeanInvoker(longWritesSingletonBean));
        }
        // Now fetch the results.
        // all are expected to timeout
        for (int i = 0; i < NUM_THREADS; i++) {
            try {
                results[i].get(10, TimeUnit.SECONDS);
                Assert.fail("Invocation on a singleton bean with WRITE lock was expected to fail for thread numbered: " + i);
            } catch (ExecutionException ee) {
                Assert.assertTrue("Unexpected exception during invocation on a singleton bean with WRITE lock", ee.getCause() instanceof ConcurrentAccessTimeoutException);
            }
        }
        // get/wait (for) the result of the first invocation. Is expected to have completed successfully
        firstInvocationResult.get(10, TimeUnit.SECONDS);
        // only one call succeeded, so count should be 1
        Assert.assertEquals("Unexpected count on singleton bean after invocation on method with WRITE lock semantic: ", 1, this.longWritesSingletonBean.getCount());
    }

    private class ReadOnlySingletonBeanInvoker implements Callable<String> {

        private ReadOnlySingletonBean bean;

        private int num;

        ReadOnlySingletonBeanInvoker(ReadOnlySingletonBean bean, int num) {
            this.bean = bean;
            this.num = num;
        }

        @Override
        public String call() throws Exception {
            return bean.twoSecondEcho(String.valueOf(this.num));
        }
    }

    private class LongWritesSingletonBeanInvoker implements Callable<Object> {

        private LongWritesSingletonBean bean;

        LongWritesSingletonBeanInvoker(LongWritesSingletonBean bean) {
            this.bean = bean;
        }

        @Override
        public Object call() throws Exception {
            bean.fiveSecondWriteOperation();
            return null;
        }
    }
}
