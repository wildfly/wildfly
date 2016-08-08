/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.clustering.cluster.ejb2.invalidation;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.as.test.clustering.NodeInfoServlet;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.cluster.ejb2.StatefulBean;
import org.jboss.as.test.clustering.cluster.ejb2.StatefulBeanBase;
import org.jboss.as.test.clustering.cluster.ejb2.StatefulRemote;
import org.jboss.as.test.clustering.cluster.ejb2.StatefulRemoteHome;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Properties;

import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

/**
 * Port of CacheInvalidationUnitTestCase from AS5 testsuite
 *
 * @author Jan Martiska / jmartisk@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CacheInvalidationTestCase {

    private static InitialContext context;
    private static ContextSelector<EJBClientContext> previousSelector;

    @ArquillianResource
    private static ContainerController controller;

    @ArquillianResource
    private static Deployer deployer;

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> deployment0() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> deployment1() {
        return createDeployment();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(env);
    }

    public static Archive<?> createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cache-invalidation-test.jar");
        archive.addClasses(CacheInvalidationTestCase.class, StatefulBean.class, StatefulBeanBase.class, StatefulRemote.class, StatefulRemoteHome.class);
        archive.addClasses(NodeNameGetter.class, NodeInfoServlet.class);
        return archive;
    }

    @AfterClass
    public static void after() {
        if (previousSelector != null) {
            EJBClientContext.setSelector(previousSelector);
        }
        if(controller.isStarted(CONTAINER_1))
            controller.stop(CONTAINER_1);
        if(controller.isStarted(CONTAINER_2))
            controller.stop(CONTAINER_2);
    }


    @Test
    public void testCacheInvalidation() throws NamingException, IOException, CreateException {
        controller.start(CONTAINER_1);
        deployer.deploy(DEPLOYMENT_1);
        controller.start(CONTAINER_2);
        deployer.deploy(DEPLOYMENT_2);
        previousSelector = EJBClientContextSelector.setup("cluster/ejb3/stateless/jboss-ejb-client.properties");
        StatefulRemoteHome home = (StatefulRemoteHome) context.lookup("ejb:/" + "cache-invalidation-test" + "//" + StatefulBean.class.getSimpleName() + "!"
                + StatefulRemoteHome.class.getName());
        StatefulRemote remote = home.create();
        for(int i=0; i<25; i++) {
            remote.incrementNumber();
        }
        controller.stop(CONTAINER_1);
        int x = remote.getNumber();
        controller.start(CONTAINER_1);
        int y = remote.getNumber();
        deployer.undeploy(DEPLOYMENT_2);
        controller.stop(CONTAINER_2);
        int z = remote.getNumber();
        deployer.undeploy(DEPLOYMENT_1);
        controller.stop(CONTAINER_1);
        Assert.assertEquals(25, x);
        Assert.assertEquals(25, y);
        Assert.assertEquals(25, z);

    }

}
