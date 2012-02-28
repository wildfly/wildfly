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
package org.jboss.as.test.clustering.cluster.ejb3;

import javax.ejb.NoSuchEJBException;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;
import org.jboss.as.test.clustering.NodeUtil;

/**
 * Tests that the stateful timeout annotation works
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class StatefulTimeoutTestCase {

    private static final String ARCHIVE_NAME = "StatefulTimeoutTestCase";
    @ArquillianResource
    private ContainerController controller;
    @ArquillianResource
    private Deployer deployer;
    @Inject
    private UserTransaction userTransaction;

    @BeforeClass
    public static void printSysProps() {
        System.out.println("System properties:\n" + System.getProperties());
    }

    @Deployment(name = DEPLOYMENT_1, managed = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createJar();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(StatefulTimeoutTestCase.class.getPackage());
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    private static Archive<?> createJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(StatefulTimeoutTestCase.class.getPackage());
        jar.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        return jar;
    }

    protected <T> T lookup(InitialContext iniCtx, Class<T> beanType) throws NamingException {
        return beanType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanType.getSimpleName() + "!" + beanType.getName()));
    }

    @Test
    @InSequence(-1)
    public void testStartContainers() {
        NodeUtil.start(controller, deployer, CONTAINER_1, DEPLOYMENT_1);
        NodeUtil.start(controller, deployer, CONTAINER_2, DEPLOYMENT_2);
    }

    @Test
    @InSequence(1)
    public void testStopContainers() {
        NodeUtil.stop(controller, deployer, CONTAINER_1, DEPLOYMENT_1);
        NodeUtil.stop(controller, deployer, CONTAINER_2, DEPLOYMENT_2);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void testStatefulTimeout(@ArquillianResource InitialContext iniCtx) throws Exception {

        ClusteredCacheBean.preDestroy = false;
        ClusteredCacheBean.prePassivate = false;
        ClusteredCacheBean sfsb1 = lookup(iniCtx, ClusteredCacheBean.class);
        Assert.assertFalse(ClusteredCacheBean.preDestroy);
        Assert.assertFalse(ClusteredCacheBean.prePassivate);
        sfsb1.increment();
        Assert.assertTrue(ClusteredCacheBean.prePassivate);
        Thread.sleep(1500);
        NoSuchEJBException exception = null;
        try {
            sfsb1.increment();
        } catch (NoSuchEJBException e) {
            exception = e;
        }
        Assert.assertNotNull(exception);
        Assert.assertTrue(ClusteredCacheBean.preDestroy);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void testClusteredStatefulTimeout(@ArquillianResource InitialContext iniCtx) throws Exception {

        ClusteredBean.preDestroy = false;
        ClusteredBean.prePassivate = false;
        ClusteredBean sfsb1 = lookup(iniCtx, ClusteredBean.class);
        Assert.assertFalse(ClusteredBean.preDestroy);
        Assert.assertFalse(ClusteredBean.prePassivate);
        sfsb1.increment();
        Assert.assertTrue(ClusteredBean.prePassivate);
        Thread.sleep(1500);
        NoSuchEJBException exception = null;
        try {
            sfsb1.increment();
        } catch (NoSuchEJBException e) {
            exception = e;
        }
        Assert.assertNotNull(exception);
        Assert.assertTrue(ClusteredBean.preDestroy);
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void testStatefulBeanNotDiscardedWhileInTransaction(@ArquillianResource InitialContext iniCtx) throws Exception {
        ClusteredBean.preDestroy = false;
        ClusteredBean.prePassivate = false;
        try {
            userTransaction.begin();
            ClusteredBean sfsb1 = lookup(iniCtx, ClusteredBean.class);
            Assert.assertFalse(ClusteredBean.preDestroy);
            Assert.assertFalse(ClusteredBean.prePassivate);
            sfsb1.increment();
            Assert.assertFalse(ClusteredBean.prePassivate);
            Thread.sleep(1500);
            sfsb1.increment();
            Assert.assertFalse(ClusteredBean.preDestroy);
//            Assert.assertFalse(ClusteredBean.prePassivate);
            userTransaction.commit();
            Assert.assertTrue(ClusteredBean.prePassivate);
        } catch (Exception e) {
            e.printStackTrace();
            userTransaction.rollback();
        }
    }

    @Test
    @OperateOnDeployment(DEPLOYMENT_1)
    public void testNested(@ArquillianResource InitialContext iniCtx) throws Exception {

        NestedBean.preDestroy = false;
        NestedBean sfsb1 = lookup(iniCtx, NestedBean.class);
        Assert.assertFalse(NestedBean.preDestroy);
        sfsb1.increment();
        Thread.sleep(1500);
        NoSuchEJBException exception = null;
        try {
            sfsb1.increment();
        } catch (NoSuchEJBException e) {
            exception = e;
        }
        Assert.assertNotNull(exception);
        Assert.assertTrue(NestedBean.preDestroy);
    }
}
