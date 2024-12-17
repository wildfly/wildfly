/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.preview.microprofile.telemetry;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * A ServerSetupTask to set up logger categories to file handlers for log messages check in the tests.
 *
 * @author <a href="mailto:aoingl@gmail.com">Lin Gao</a>
 */
public class LoggingServerSetupTask implements ServerSetupTask {

    private static final ModelNode LOGGING_ADDRESS = Operations.createAddress(SUBSYSTEM, "logging");

    static final String SMALLRYE_OPENTELEMETRY_LOG_FILE = "smallrye-opentelemetry.log";
    static final String VERTX_FEATURE_PACK_LOG_FILE = "vertx-feature-pack.log";
    private static final String SMALLRYE_OPENTELEMETRY_LOGGER_CATEGORY = "io.smallrye.opentelemetry.implementation.exporters";
    private static final String SMALLRYE_OPENTELEMETRY_LOGGER_HANDLER = "vertxExporterProvider";
    private static final String VERTX_FEATURE_PACK_LOGGER_CATEGORY = "org.wildfly.extension.vertx";
    private static final String VERTX_FEATURE_PACK_LOGGER_HANDLER = "vertxLogger";

    private final String smallryeOpentelemetryLogFile;
    private final String vertxSubsystemLogFile;

    public LoggingServerSetupTask() {
        this(SMALLRYE_OPENTELEMETRY_LOG_FILE, VERTX_FEATURE_PACK_LOG_FILE);
    }

    protected LoggingServerSetupTask(String smallryeOpentelemetryLogFile, String vertxSubsystemLogFile) {
        this.smallryeOpentelemetryLogFile = smallryeOpentelemetryLogFile;
        this.vertxSubsystemLogFile = vertxSubsystemLogFile;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        setupLogger(managementClient, SMALLRYE_OPENTELEMETRY_LOGGER_CATEGORY, SMALLRYE_OPENTELEMETRY_LOGGER_HANDLER, smallryeOpentelemetryLogFile);
        setupLogger(managementClient, VERTX_FEATURE_PACK_LOGGER_CATEGORY, VERTX_FEATURE_PACK_LOGGER_HANDLER, vertxSubsystemLogFile);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private void setupLogger(ManagementClient managementClient, String category, String loggerHandler, String logFileName) throws Exception {
        ModelNode fileHandlerAddress = new ModelNode().set(LOGGING_ADDRESS).add(FILE_HANDLER, loggerHandler);
        ModelNode fileHandler = new ModelNode();
        fileHandler.get(OP).set(ADD);
        fileHandler.get(OP_ADDR).set(fileHandlerAddress);
        ModelNode file = new ModelNode();
        file.get("relative-to").set("jboss.server.log.dir");
        file.get("path").set(logFileName);
        fileHandler.get(FILE).set(file);
        fileHandler.get("append").set(false);
        fileHandler.get("level").set("DEBUG");
        ManagementOperations.executeOperation(managementClient.getControllerClient(), fileHandler);

        ModelNode loggerAddress = new ModelNode().set(LOGGING_ADDRESS).add("logger", category);
        ModelNode logger = new ModelNode();
        logger.get(OP).set(ADD);
        logger.get(OP_ADDR).set(loggerAddress);
        logger.get("category").set(category);
        logger.get("level").set("DEBUG");
        ModelNode handlers = new ModelNode().add(loggerHandler);
        logger.get("handlers").set(handlers);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), logger);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        removeLogger(managementClient, SMALLRYE_OPENTELEMETRY_LOGGER_CATEGORY, SMALLRYE_OPENTELEMETRY_LOGGER_HANDLER);
        removeLogger(managementClient, VERTX_FEATURE_PACK_LOGGER_CATEGORY, VERTX_FEATURE_PACK_LOGGER_HANDLER);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    private void removeLogger(ManagementClient managementClient, String category, String loggerHandler) throws Exception {
        ModelNode loggerAddress = new ModelNode().set(LOGGING_ADDRESS).add("logger", category);
        ModelNode logger = new ModelNode();
        logger.get(OP).set(REMOVE);
        logger.get(OP_ADDR).set(loggerAddress);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), logger);

        ModelNode fileHandlerAddress = new ModelNode().set(LOGGING_ADDRESS).add(FILE_HANDLER, loggerHandler);
        ModelNode fileHandler = new ModelNode();
        fileHandler.get(OP).set(REMOVE);
        fileHandler.get(OP_ADDR).set(fileHandlerAddress);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), fileHandler);
    }
}
