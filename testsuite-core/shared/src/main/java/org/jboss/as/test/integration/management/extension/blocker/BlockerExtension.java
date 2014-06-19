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

package org.jboss.as.test.integration.management.extension.blocker;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Set;
import java.util.logging.Logger;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.test.integration.management.extension.EmptySubsystemParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Extension that can block threads.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class BlockerExtension implements Extension {

    public static final String MODULE_NAME = "org.wildfly.extension.blocker-test";
    public static final String SUBSYSTEM_NAME = "blocker-test";
    public static final AttributeDefinition CALLER = SimpleAttributeDefinitionBuilder.create("caller", ModelType.STRING, true)
            .setDefaultValue(new ModelNode("unknown")).build();
    public static final AttributeDefinition TARGET_HOST = SimpleAttributeDefinitionBuilder.create(HOST, ModelType.STRING, true).build();
    public static final AttributeDefinition TARGET_SERVER = SimpleAttributeDefinitionBuilder.create(SERVER, ModelType.STRING, true).build();
    public static final AttributeDefinition BLOCK_POINT = SimpleAttributeDefinitionBuilder.create("block-point", ModelType.STRING)
            .setValidator(EnumValidator.create(BlockPoint.class, false, false))
            .build();
    public static final AttributeDefinition BLOCK_TIME = SimpleAttributeDefinitionBuilder.create("block-time", ModelType.LONG, true)
            .setDefaultValue(new ModelNode(20000))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .build();

    public static final AttributeDefinition FOO = SimpleAttributeDefinitionBuilder.create("foo", ModelType.BOOLEAN, true).build();


    private static final EmptySubsystemParser PARSER = new EmptySubsystemParser("urn:wildfly:extension:blocker-test:1.0");
    private static final Logger log = Logger.getLogger(BlockerExtension.class.getCanonicalName());

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0, 0);
        subsystem.registerSubsystemModel(new BlockerSubsystemResourceDefinition(context.getProcessType() == ProcessType.HOST_CONTROLLER));
        subsystem.registerXMLElementWriter(PARSER);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PARSER.getNamespace(), PARSER);
    }

    private static class BlockerSubsystemResourceDefinition extends SimpleResourceDefinition {

        private final boolean forHost;
        private BlockerSubsystemResourceDefinition(boolean forHost) {
            super(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), new NonResolvingResourceDescriptionResolver(),
                    new AbstractAddStepHandler(),
                    ReloadRequiredRemoveStepHandler.INSTANCE);
            this.forHost = forHost;
        }

        @Override
        public void registerOperations(ManagementResourceRegistration resourceRegistration) {
            super.registerOperations(resourceRegistration);
            resourceRegistration.registerOperationHandler(BlockHandler.DEFINITION, new BlockHandler());
            if (forHost) {
                resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
            }
            log.info("Registered blocker-test operations");
        }

        @Override
        public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
            super.registerAttributes(resourceRegistration);
            resourceRegistration.registerReadWriteAttribute(FOO, null, new ModelOnlyWriteAttributeHandler(FOO));
        }
    }

    public static enum BlockPoint {
        MODEL,
        RUNTIME,
        SERVICE_START,
        SERVICE_STOP,
        VERIFY,
        COMMIT,
        ROLLBACK
    }

    private static class BlockHandler implements OperationStepHandler {

        private static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder("block", new NonResolvingResourceDescriptionResolver())
                .setParameters(CALLER, TARGET_HOST, TARGET_SERVER, BLOCK_POINT, BLOCK_TIME)
                .build();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode targetServer = TARGET_SERVER.resolveModelAttribute(context, operation);
            ModelNode targetHost = TARGET_HOST.resolveModelAttribute(context, operation);
            final BlockPoint blockPoint = BlockPoint.valueOf(BLOCK_POINT.resolveModelAttribute(context, operation).asString());
            log.info("block requested by " + CALLER.resolveModelAttribute(context, operation).asString() + " for " +
                targetHost.asString() + "/" + targetServer.asString() + "(" + blockPoint + ")");
            boolean forMe = false;
            if (context.getProcessType() == ProcessType.STANDALONE_SERVER) {
                forMe = true;
            } else {
                Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
                if (targetServer.isDefined()) {
                    if (context.getProcessType().isServer()) {
                        String name = System.getProperty(ServerEnvironment.SERVER_NAME);
                        forMe = targetServer.asString().equals(name);
                    }
                } else if (context.getProcessType() == ProcessType.HOST_CONTROLLER) {
                    Set<String> hosts = rootResource.getChildrenNames(HOST);
                    String name;
                    if (hosts.size() > 1) {
                        name = "master";
                    } else {
                        name = hosts.iterator().next();
                    }
                    if (!targetHost.isDefined()) {
                        throw new OperationFailedException("target-host required");
                    }
                    forMe = targetHost.asString().equals(name);
                }
            }
            if (forMe) {
                final long blockTime = BLOCK_TIME.resolveModelAttribute(context, operation).asLong();
                log.info("will block at " + blockPoint + " for " + blockTime);
                switch (blockPoint) {
                    case MODEL: {
                        block(blockTime);
                        break;
                    }
                    case RUNTIME: {
                        context.addStep(new BlockStep(blockTime), OperationContext.Stage.RUNTIME);
                        break;
                    }
                    case SERVICE_START:
                    case SERVICE_STOP: {
                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                BlockingService service = new BlockingService(blockTime, blockPoint == BlockPoint.SERVICE_START);

                                context.getServiceTarget().addService(BlockingService.SERVICE_NAME, service).install();

                                context.completeStep(new OperationContext.ResultHandler() {
                                    @Override
                                    public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                                        log.info("BlockingService step completed: result = " + resultAction);
                                        context.removeService(BlockingService.SERVICE_NAME);
                                    }
                                });
                            }
                        }, OperationContext.Stage.RUNTIME);
                        break;
                    }
                    case VERIFY: {
                        context.addStep(new BlockStep(blockTime), OperationContext.Stage.VERIFY);
                        break;
                    }
                    case ROLLBACK:
                        context.addStep(new OperationStepHandler() {
                            @Override
                            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                                context.getFailureDescription().set("rollback");
                                context.setRollbackOnly();
                                context.stepCompleted();
                            }
                        }, OperationContext.Stage.MODEL);
                        break;
                    case COMMIT:
                        break;
                    default:
                        throw new IllegalStateException(blockPoint.toString());
                }
                context.completeStep(new OperationContext.ResultHandler() {
                    @Override
                    public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                        if ((blockPoint == BlockPoint.COMMIT && resultAction == OperationContext.ResultAction.KEEP)
                            || (blockPoint == BlockPoint.ROLLBACK && resultAction == OperationContext.ResultAction.ROLLBACK)) {
                            block(blockTime);
                        }
                    }
                });
            } else {

                context.stepCompleted();
            }
        }

        private static void block(long time) {
            try {
                log.info("blocking");
                Thread.sleep(time);
            } catch (InterruptedException e) {
                log.info("interrupted");
                throw new RuntimeException(e);
            }
        }

        private static class BlockStep implements OperationStepHandler {
            private final long blockTime;

            private BlockStep(long blockTime) {
                this.blockTime = blockTime;
            }

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                block(blockTime);
                context.stepCompleted();
            }
        }
    }

    private static class BlockingService implements Service<BlockingService> {

        private static final ServiceName SERVICE_NAME = ServiceName.of("jboss", "test", "blocking-service");
        private final long blockTime;
        private final boolean blockStart;

        private final Object waitObject = new Object();

        private BlockingService(long blockTime, boolean blockStart) {
            this.blockTime = blockTime;
            this.blockStart = blockStart;
        }

        @Override
        public void start(final StartContext context) throws StartException {
            if (blockStart) {
//                Runnable r = new Runnable() {
//                    @Override
//                    public void run() {
                        try {
                            synchronized (waitObject) {
                                log.info("BlockService blocking in start");
                                waitObject.wait(blockTime);
                            }
                            context.complete();
                        } catch (InterruptedException e) {
                            log.info("BlockService interrupted");
//                            context.failed(new StartException(e));
                            throw new StartException(e);
                        }
//                    }
//                };
//                Thread thread = new Thread(r);
//                thread.start();
//                context.asynchronous();
            } else {
                // Not yet used
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public void stop(final StopContext context) {
            if (!blockStart) {
                // Not yet used
                throw new UnsupportedOperationException();
            } else {
                synchronized (waitObject) {
                    log.info("BlockService Stopping");
                    waitObject.notifyAll();
                }
            }
        }

        @Override
        public BlockingService getValue() throws IllegalStateException, IllegalArgumentException {
            return this;
        }
    }
}
