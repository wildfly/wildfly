/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.tracer;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.dmr.ModelNode;

/**
 * Adding new log file handler wich is then bound to a logger with specific category.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class LogHandlerCreationSetup implements ServerSetupTask {
    public static String JCA_LOG_FILE_PARAM = "jca-server.log";
    public static final String SERVER_LOG_DIR_PARAM = "jboss.server.log.dir";
    public static String SERVER_LOG_DIR_VALUE;

    private static final String HANDLER_NAME = "jca-log-handler";
    private static final String LOGGER_CATEGORY_VALUE = "org.jboss.jca.core.tracer";
    private static final ModelNode LOGGING_ADDRESS = new ModelNode()
        .add(SUBSYSTEM, "logging");
    private static final ModelNode FILE_HANDLER_ADDRESS = new ModelNode()
        .set(LOGGING_ADDRESS)
        .add(FILE_HANDLER, HANDLER_NAME);
    private static final ModelNode LOGGER_ADDRESS = new ModelNode()
        .set(LOGGING_ADDRESS)
        .add(LOGGER, LOGGER_CATEGORY_VALUE);

    static {
        LOGGING_ADDRESS.protect();
        FILE_HANDLER_ADDRESS.protect();
        LOGGER_ADDRESS.protect();
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {

        // /subsystem=logging/file-handler=jca-log-handler:add(append=false, file={relative-to=jboss.server.log.dir, path=jca-server.log})
        ModelNode fileHandler = new ModelNode();
        fileHandler.get(OP).set(ADD);
        fileHandler.get(OP_ADDR).set(FILE_HANDLER_ADDRESS);

        ModelNode file = new ModelNode();
        file.get("relative-to").set(SERVER_LOG_DIR_PARAM);
        file.get("path").set(JCA_LOG_FILE_PARAM);
        fileHandler.get(FILE).set(file);
        fileHandler.get("append").set("false");

        ManagementOperations.executeOperation(managementClient.getControllerClient(), fileHandler);

        // /subsystem=logging/logger=org.jboss.jca.core.tracer:add(category=org.jboss.jca.core.tracer, level=TRACE, handlers=[jca-log-handler])
        ModelNode logger = new ModelNode();
        logger.get(OP).set(ADD);
        logger.get(OP_ADDR).set(LOGGER_ADDRESS);
        logger.get("category").set(LOGGER_CATEGORY_VALUE);
        logger.get("level").set("TRACE");
        ModelNode handlers = new ModelNode()
            .add(HANDLER_NAME);
        logger.get("handlers").set(handlers);

        ManagementOperations.executeOperation(managementClient.getControllerClient(), logger);

        ModelNode getLogDir = new ModelNode();
        getLogDir.get(OP).set("resolve-expression");
        getLogDir.get("expression").set("${" + SERVER_LOG_DIR_PARAM + "}");
        SERVER_LOG_DIR_VALUE = ManagementOperations
            .executeOperation(managementClient.getControllerClient(), getLogDir).asString();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        ModelNode logger = new ModelNode();
        logger.get(OP).set(REMOVE);
        logger.get(OP_ADDR).set(LOGGER_ADDRESS);

        ManagementOperations.executeOperation(managementClient.getControllerClient(), logger);

        ModelNode fileHandler = new ModelNode();
        fileHandler.get(OP).set(REMOVE);
        fileHandler.get(OP_ADDR).set(FILE_HANDLER_ADDRESS);

        ManagementOperations.executeOperation(managementClient.getControllerClient(), fileHandler);

        new File(SERVER_LOG_DIR_VALUE, JCA_LOG_FILE_PARAM).delete();
    }
}
