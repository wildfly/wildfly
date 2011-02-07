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

import java.util.Collections;
import java.util.Properties;

import junit.framework.Assert;

import org.jboss.as.server.Main;
import org.jboss.as.server.NewBootstrap;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
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

    @Test
    public void testServerStartupAndShutDown() throws Exception {
        System.out.println(Main.class.getClassLoader());
        ServerEnvironment serverEnvironment = Main.determineEnvironment(new String[0], new Properties(System.getProperties()), System.getenv());
        Assert.assertNotNull(serverEnvironment);
        final NewBootstrap bootstrap = NewBootstrap.Factory.newInstance();
        final NewBootstrap.Configuration configuration = new NewBootstrap.Configuration();
        configuration.setServerEnvironment(serverEnvironment);
        configuration.setModuleLoader(Module.getSystemModuleLoader());
        configuration.setPortOffset(0);

        final ServiceContainer container = bootstrap.start(configuration, Collections.<ServiceActivator>emptyList()).get();
        Assert.assertNotNull(container);
        container.shutdown();
        container.awaitTermination();
        Assert.assertTrue(container.isShutdownComplete());



    }
}
