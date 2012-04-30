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

package org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.*;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import static org.jboss.as.test.clustering.ClusteringTestConstants.*;

import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that invocations on a clustered stateful session EJB2 bean from a remote EJB client, failover to
 * other node(s) in cases like a node going down.
 * This test is taken from test of ejb3 beans.
 *
 * @author Jaikiran Pai, Radoslav Husar, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteEJBClientStatefulBeanFailoverTestCase extends RemoteEJBClientStatefulFaliloverTestBase {
    // private static final Logger log = Logger.getLogger(RemoteEJBClientStatefulBeanFailoverTestCase.class);

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;
    
    @Deployment(name = DEPLOYMENT_1_SINGLE, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1Singleton() {
        return createDeploymentSingleton();
    }

    @Deployment(name = DEPLOYMENT_2_SINGLE, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2Singleton() {
        return createDeploymentSingleton();
    }    

    @Deployment(name = DEPLOYMENT_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1() {
        return createDeployment();
    }

    @Deployment(name = DEPLOYMENT_2, managed = false, testable = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> createDeploymentForContainer2() {
        return createDeployment();
    }

    private static Archive<?> createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(CounterBaseBean.class, CounterBean.class, CounterRemote.class, CounterRemoteHome.class, CounterResult.class);
        jar.addClass(NodeNameGetter.class);
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment." + ARCHIVE_NAME_SINGLE + ".jar\n"), "MANIFEST.MF");
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        Properties env = new Properties();
        env.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(env);
    }
    
    @Override
    @InSequence(1)
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception {
        failoverFromRemoteClient(container, deployer, false);
    }

    @Override
    @InSequence(2)
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeUndeploys() throws Exception {
        failoverFromRemoteClient(container, deployer, true);
    }
}
