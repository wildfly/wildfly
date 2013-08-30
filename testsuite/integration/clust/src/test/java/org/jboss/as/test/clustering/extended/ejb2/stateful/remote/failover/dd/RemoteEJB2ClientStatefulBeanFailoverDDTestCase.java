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

package org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover.dd;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.as.test.clustering.NodeNameGetter;
import org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover.CounterBaseBean;
import org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover.CounterRemote;
import org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover.CounterRemoteHome;
import org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover.CounterResult;
import org.jboss.as.test.clustering.extended.ejb2.stateful.remote.failover.RemoteEJBClientStatefulFailoverTestBase;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteEJB2ClientStatefulBeanFailoverDDTestCase extends RemoteEJBClientStatefulFailoverTestBase {
    private static final Logger log = Logger.getLogger(RemoteEJB2ClientStatefulBeanFailoverDDTestCase.class);

    @Deployment(name = DEPLOYMENT_HELPER_1, managed = false, testable = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> createDeploymentForContainer1Singleton() {
        return createDeploymentSingleton();
    }

    @Deployment(name = DEPLOYMENT_HELPER_2, managed = false, testable = false)
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
        jar.addClasses(CounterBaseBean.class, CounterBeanDD.class, CounterRemote.class, CounterRemoteHome.class, CounterResult.class);
        jar.addClass(NodeNameGetter.class);
        jar.addAsManifestResource(RemoteEJB2ClientStatefulBeanFailoverDDTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(RemoteEJB2ClientStatefulBeanFailoverDDTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment." + ARCHIVE_NAME_SINGLE + ".jar\n"), "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @Ignore("JBPAPP-8726")
    @Override
    @InSequence(1)
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeGoesDown() throws Exception {
        failoverFromRemoteClient(false);
    }

    @Ignore("JBPAPP-8726")
    @Override
    @InSequence(2)
    @Test
    public void testFailoverFromRemoteClientWhenOneNodeUndeploys() throws Exception {
        failoverFromRemoteClient(true);
    }
}
