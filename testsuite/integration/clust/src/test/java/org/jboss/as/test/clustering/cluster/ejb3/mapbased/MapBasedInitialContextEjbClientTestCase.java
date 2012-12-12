/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.clustering.cluster.ejb3.mapbased;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.clustering.cluster.ejb3.mapbased.beans.StatefulBean;
import org.jboss.as.test.clustering.cluster.ejb3.mapbased.beans.StatefulIface;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_1;
import static org.jboss.as.test.clustering.ClusteringTestConstants.CONTAINER_2;

/**
 * Tests for EJBCLIENT-34: properties-based JNDI InitialContext for EJB clients.
 * Clustered version of the test.
 * @author Jan Martiska / jmartisk@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MapBasedInitialContextEjbClientTestCase {

    private static final String ARCHIVE_NAME = "map-based-client-2";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    private static String classLoaderScanPropertyPreviousValue;


    /**
     * in case anyone puts jboss-ejb-client.properties on the classpath: we don't want to use that file
     */
    @BeforeClass
    public static void prepare() {
        classLoaderScanPropertyPreviousValue = System.getProperty("jboss.ejb.client.properties.skip.classloader.scan");
        System.setProperty("jboss.ejb.client.properties.skip.classloader.scan", "true");
    }

    @AfterClass
    public static void after() {
        if (classLoaderScanPropertyPreviousValue != null) {
            System.setProperty("jboss.ejb.client.properties.skip.classloader.scan", classLoaderScanPropertyPreviousValue);
        }
    }

    @Deployment(name = "dep1", managed = false)
    @TargetsContainer(CONTAINER_1)
    public static Archive<?> getDeployment1() {
        return createDeployment();
    }

    @Deployment(name = "dep2", managed = false)
    @TargetsContainer(CONTAINER_2)
    public static Archive<?> getDeployment2() {
        return createDeployment();
    }

    public static Archive<?> createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        archive.addClasses(StatefulBean.class, StatefulIface.class);
        return archive;
    }

    @Test
    public void doTestCluster() throws InterruptedException, NamingException {
        controller.start(CONTAINER_1);
        controller.start(CONTAINER_2);
        deployer.deploy("dep2");
        deployer.deploy("dep1");
        Thread.sleep(5000);
        Context ctx = new InitialContext(getEjbClientProperties(System.getProperty("node0", "127.0.0.1"), 4447));
        String lookupName = "ejb:/"+ ARCHIVE_NAME +"/"+StatefulBean.class.getSimpleName()+"!"+StatefulIface.class.getCanonicalName()+"?stateful";
        StatefulIface beanStateful = (StatefulIface)ctx.lookup(lookupName);
        beanStateful.setNumber(20);
        controller.stop(CONTAINER_1);
        Assert.assertEquals("Unexpected return value from EJB call", 20, beanStateful.getNumber());
        ctx.close();
        if(controller.isStarted(CONTAINER_1))
            controller.stop(CONTAINER_1);
        if(controller.isStarted(CONTAINER_2))
            controller.stop(CONTAINER_2);
    }

    private Properties getEjbClientProperties(String node, int port) {
        Properties props = new Properties();
        props.put("org.jboss.ejb.client.scoped.context", true);
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        props.put("endpoint.name", "client");
        props.put("remote.connections", "main");
        props.put("remote.connection.main.host", node);
        props.put("remote.connection.main.port", Integer.toString(port));
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOANONYMOUS", "false");
        props.put("remote.connection.main.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "true");
        props.put("remote.connectionprovider.create.options.org.xnio.Options.SSL_ENABLED", "false");
        return props;
    }

}
