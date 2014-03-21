/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.logging.operations;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.QueueHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CustomHandlerOperationsTestCase extends AbstractLoggingOperationsTestCase {

    private static final String FILE_NAME = "custom-handler-file.log";
    private static final String CUSTOM_HANDLER_NAME = "customFileHandler";
    private static final PathAddress CUSTOM_HANDLER_ADDRESS = createCustomHandlerAddress(CUSTOM_HANDLER_NAME);
    @ContainerResource
    private ManagementClient managementClient;
    @ArquillianResource(DefaultLoggingServlet.class)
    private URL url;
    private File logFile = null;

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "default-logging.war");
        archive.addClasses(DefaultLoggingServlet.class);
        return archive;
    }

    @Before
    public void setLogFile() throws Exception {
        if (logFile == null) {
            logFile = getAbsoluteLogFilePath(managementClient, FILE_NAME);
        }
    }

    @Override
    protected ManagementClient getManagementClient() {
        return managementClient;
    }

    @Test
    public void testCustomHandler() throws Exception {
        testCustomHandler(null);

        // Create the profile
        final String profileName = "test-profile";
        final ModelNode profileAddress = createAddress("logging-profile", profileName).toModelNode();
        final ModelNode addOp = Operations.createAddOperation(profileAddress);
        executeOperation(addOp);

        testCustomHandler(profileName);

        // Clean-up
        executeOperation(Operations.createRemoveOperation(profileAddress));
        verifyRemoved(profileAddress);
    }

    private void testCustomHandler(final String profileName) throws Exception {
        final ModelNode address = createCustomHandlerAddress(profileName, "CONSOLE").toModelNode();

        // Add the handler
        final ModelNode addOp = Operations.createAddOperation(address);
        addOp.get("module").set("org.jboss.logmanager");
        addOp.get("class").set(ConsoleHandler.class.getName());
        executeOperation(addOp);

        // Write each attribute and check the value
        testWrite(address, "level", "INFO");
        testWrite(address, "enabled", true);
        testWrite(address, "encoding", "utf-8");
        testWrite(address, "formatter", "[test] %d{HH:mm:ss,SSS} %-5p [%c] %s%E%n");
        testWrite(address, "filter-spec", "deny");
        testWrite(address, "class", QueueHandler.class.getName());
        testWrite(address, "module", "org.jboss.logmanager");
        // Create a properties value
        final ModelNode properties = new ModelNode().setEmptyObject();
        properties.get("autoFlush").set(true);
        properties.get("target").set("SYSTEM_OUT");
        testWrite(address, "properties", properties);

        // Undefine attributes
        testUndefine(address, "level");
        testUndefine(address, "enabled");
        testUndefine(address, "encoding");
        testUndefine(address, "formatter");
        testUndefine(address, "filter-spec");
        testUndefine(address, "properties");

        // Clean-up
        executeOperation(Operations.createRemoveOperation(address));
        verifyRemoved(address);
    }
}
