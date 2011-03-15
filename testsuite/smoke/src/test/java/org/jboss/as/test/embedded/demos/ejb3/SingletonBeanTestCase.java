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

package org.jboss.as.test.embedded.demos.ejb3;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.api.Run;
import org.jboss.arquillian.api.RunModeType;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.CallTrackerSingletonBean;
import org.jboss.as.demos.ejb3.archive.SimpleSingletonBean;
import org.jboss.as.demos.ejb3.archive.SimpleSingletonLocal;
import org.jboss.as.demos.ejb3.archive.StartupSingleton;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Testcase for testing the basic functionality of a EJB3 singleton session bean.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@Run(RunModeType.IN_CONTAINER)
public class SingletonBeanTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-singleton-bean-example.jar");
        jar.addManifestResource("archives/ejb3-example.jar/META-INF/MANIFEST.MF", "MANIFEST.MF");
        jar.addPackage(SimpleSingletonBean.class.getPackage());
        jar.addPackage(StartupSingleton.class.getPackage());
        jar.addClass(ReadOnlySingletonBean.class);
        jar.addClass(LongWritesSingletonBean.class);
        jar.addClass(SingletonBeanTestCase.class);
        return jar;
    }

    /**
     * Test a basic invocation on @Singleton bean
     *
     * @throws Exception
     */
    @Test
    public void testSingletonEJB() throws Exception {
        Context ctx = new InitialContext();
        SimpleSingletonLocal singletonBean = (SimpleSingletonLocal) ctx.lookup("java:global/ejb3-singleton-bean-example/" + SimpleSingletonBean.class.getSimpleName() + "!" + SimpleSingletonLocal.class.getName());
        final int NUM_TIMES = 10;
        for (int i = 0; i < NUM_TIMES; i++) {
            singletonBean.increment();
        }
        Assert.assertEquals("Unexpected count returned from singleton bean", NUM_TIMES, singletonBean.getCount());

    }

    @Test
    public void testStartupSingleton() throws Exception {
        Context ctx = new InitialContext();
        CallTrackerSingletonBean callTrackerSingletonBean = (CallTrackerSingletonBean) ctx.lookup("java:global/ejb3-singleton-bean-example/" + CallTrackerSingletonBean.class.getSimpleName() + "!" + CallTrackerSingletonBean.class.getName());
        Assert.assertTrue("@Startup singleton bean was not created", callTrackerSingletonBean.wasStartupSingletonBeanCreated());

    }

    @Test
    public void testReadOnlySingleton() throws Exception {
        Context ctx = new InitialContext();
        ReadOnlySingletonBean readOnlySingletonBean = (ReadOnlySingletonBean) ctx.lookup("java:global/ejb3-singleton-bean-example/" + ReadOnlySingletonBean.class.getSimpleName() + "!" + ReadOnlySingletonBean.class.getName());
        final int NUM_THREADS = 10;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        Future<String>[] results = new Future[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            results[i] = executor.submit(new ReadOnlySingletonBeanInvoker(readOnlySingletonBean, i));
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            String result = results[i].get(10, TimeUnit.SECONDS);
            Assert.assertEquals("Unexpected value from singleton bean", String.valueOf(i), result);
        }
    }

    @Test
    @Ignore ("Disabled till we figure out why javax.ejb.* isn't available in the testcase module")
    public void testLongWritesSingleton() throws Exception {
        Context ctx = new InitialContext();
        LongWritesSingletonBean singletonBean = (LongWritesSingletonBean) ctx.lookup("java:global/ejb3-singleton-bean-example/" + LongWritesSingletonBean.class.getSimpleName() + "!" + LongWritesSingletonBean.class.getName());
        final int NUM_THREADS = 10;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        Future<?>[] results = new Future[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; i++) {
            results[i] = executor.submit(new LongWritesSingletonBeanInvoker(singletonBean));
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            results[i].get(10, TimeUnit.SECONDS);
        }
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
            bean.threeSecondWriteOperation();
            return null;
        }
    }
}
