/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.management.extension.streams;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.test.integration.management.extension.EmptySubsystemParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Extension that can provide log files as a stream.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class LogStreamExtension implements Extension {

    public static final String MODULE_NAME = "org.wildfly.extension.log-stream-test";
    public static final String SUBSYSTEM_NAME = "log-stream-test";
    public static final String STREAM_LOG_FILE = "stream-log-file";
    public static final String LOG_MESSAGE_PROP = "wildfly.test.stream.response.key";

    private static final EmptySubsystemParser PARSER = new EmptySubsystemParser("urn:wildfly:extension:log-stream-test:1.0");
    private static final Logger log = Logger.getLogger(LogStreamExtension.class.getCanonicalName());

    public static final AttributeDefinition LOG_FILE = SimpleAttributeDefinitionBuilder.create("log-file", ModelType.INT)
            .setStorageRuntime()
            .setAllowNull(true)
            .build();

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0, 0);
        subsystem.registerSubsystemModel(new LogStreamSubsystemResourceDefinition());
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PARSER.getNamespace(), PARSER);
    }

    public static String getLogMessage(String key) {
        return "LogStreamHandler invoked with key " + key;
    }

    private static class LogStreamSubsystemResourceDefinition extends SimpleResourceDefinition {

        private final OperationStepHandler handler;
        private LogStreamSubsystemResourceDefinition() {
            super(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler(),
                    ReloadRequiredRemoveStepHandler.INSTANCE);
            this.handler = new LogStreamHandler();
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            resourceRegistration.registerOperationHandler(LogStreamHandler.DEFINITION, handler);
            resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

            log.info("Registered log-stream-test operations");
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

            resourceRegistration.registerReadOnlyAttribute(LOG_FILE, handler);
            log.info("Registered log-stream-test attributes");
        }
    }

    private static class LogStreamHandler implements OperationStepHandler {

        private static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(STREAM_LOG_FILE,
                new NonResolvingResourceDescriptionResolver())
                .setReplyType(ModelType.INT)
                .build();


        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

            // Put an entry in the log that the test driver can use to verify a complete log
            logPropValue(context);

            String path;
            if (context.getProcessType().isServer()) {
                path = System.getProperty("jboss.server.log.dir") + File.separatorChar + "server.log";
            } else {
                path = System.getProperty("jboss.domain.log.dir") + File.separatorChar + "host-controller.log";
            }
            File f = new File(path);
            try {
                String uuid = context.attachResultStream("text/plain", new FileInputStream(f));
                context.getResult().set(uuid);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            context.stepCompleted();
        }

        private void logPropValue(OperationContext context) {
            Resource res = context.readResourceFromRoot(PathAddress.pathAddress(SYSTEM_PROPERTY, LOG_MESSAGE_PROP));
            String key = res.getModel().get(VALUE).asString();
            log.info(getLogMessage(key));
        }

    }
}
