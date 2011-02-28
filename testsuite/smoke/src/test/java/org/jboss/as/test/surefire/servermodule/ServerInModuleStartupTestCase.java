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
package org.jboss.as.test.surefire.servermodule;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Properties;

import junit.framework.Assert;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.Main;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Here to prove the forked surefire plugin is capable of running
 * modular tests. This plugin will load up this test class in a module that can see
 * org.jboss.as.standalone.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ServerInModuleStartupTestCase {

    static ServiceContainer container;
    @BeforeClass
    public static void startServer() throws Exception {
        ServerEnvironment serverEnvironment = Main.determineEnvironment(new String[0], new Properties(System.getProperties()), System.getenv());
        Assert.assertNotNull(serverEnvironment);
        final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
        final Bootstrap.Configuration configuration = new Bootstrap.Configuration();
        configuration.setServerEnvironment(serverEnvironment);
        configuration.setModuleLoader(Module.getBootModuleLoader());
        configuration.setPortOffset(0);

        container = bootstrap.startup(configuration, Collections.<ServiceActivator>emptyList()).get();
        Assert.assertNotNull(container);
    }

    @AfterClass
    public static void testServerStartupAndShutDown() throws Exception {
        container.shutdown();
        container.awaitTermination();
        Assert.assertTrue(container.isShutdownComplete());
    }

    @Test
    public void testXmlConfigDemo() throws Exception {
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        try {
            ModelNode request = new ModelNode();
            request.get("operation").set("read-config-as-xml");
            request.get("address").setEmptyList();
            ModelNode r = client.execute(request);

            Assert.assertEquals(SUCCESS, r.require(OUTCOME).asString());



            //TODO parse and compare the result to standlone.xml?
        } finally {
            StreamUtils.safeClose(client);
        }
    }

    @Test
    public void testDescriptionDemo() throws Exception {
        ModelControllerClient client = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
        try {
            ModelNode request = new ModelNode();
            request.get("operation").set("read-resource");
            request.get("address").setEmptyList();
            request.get("recursive").set(true);
            ModelNode r = client.execute(request);

            Assert.assertEquals(SUCCESS, r.require(OUTCOME).asString());
        } finally {
            StreamUtils.safeClose(client);
        }
    }
}
