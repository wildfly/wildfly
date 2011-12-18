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
package org.jboss.as.test.smoke.surefire.servermodule;

import java.util.Collections;
import java.util.Properties;

import junit.framework.Assert;

import org.jboss.as.server.Bootstrap;
import org.jboss.as.server.EmbeddedStandAloneServerFactory;
import org.jboss.as.server.Main;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public abstract class AbstractServerInModuleTestCase {

    protected static String serverDetails = "";
    static ServiceContainer container;

    @BeforeClass
    public static void startServer() throws Exception {

        EmbeddedStandAloneServerFactory.setupCleanDirectories(System.getProperties());

        ServerEnvironment serverEnvironment = Main.determineEnvironment(new String[0], new Properties(System.getProperties()), System.getenv(), ServerEnvironment.LaunchType.EMBEDDED);

        serverDetails += "AS server details: ";
        serverDetails += "server homedir = " + serverEnvironment.getHomeDir();
        serverDetails += ", javaextdirs = " +serverEnvironment.getJavaExtDirs();
        serverDetails += ", modules_dir = " +serverEnvironment.getModulesDir();
        serverDetails += ", server_base = " +serverEnvironment.getServerBaseDir();
        serverDetails += ", server_config_dir = " +serverEnvironment.getServerConfigurationDir();
        serverDetails += ", server_config_file = " +serverEnvironment.getServerConfigurationFile();

        Assert.assertNotNull(serverEnvironment);
        final Bootstrap bootstrap = Bootstrap.Factory.newInstance();
        final Bootstrap.Configuration configuration = new Bootstrap.Configuration(serverEnvironment);
        configuration.setModuleLoader(Module.getBootModuleLoader());
        configuration.setPortOffset(0);

        container = bootstrap.startup(configuration, Collections.<ServiceActivator>emptyList()).get();
        Assert.assertNotNull(serverDetails,container);
    }

    @AfterClass
    public static void testServerStartupAndShutDown() throws Exception {
        container.shutdown();
        container.awaitTermination();
        Assert.assertTrue(serverDetails, container.isShutdownComplete());
    }

    public AbstractServerInModuleTestCase() {
        super();
    }

}
