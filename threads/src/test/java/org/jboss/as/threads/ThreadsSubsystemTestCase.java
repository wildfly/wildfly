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
package org.jboss.as.threads;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.COUNT;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.PER_CPU;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREADS;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.CommonAttributes.TIME;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.UNIT;

import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import junit.framework.Assert;

import org.jboss.as.controller.AbstractControllerService;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ThreadsSubsystemTestCase {

    static final DescriptionProvider NULL_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            return new ModelNode();
        }
    };

    static ModelNode profileAddress = new ModelNode();
    static {
        profileAddress.add("profile", "test");
    }

    private static final DescriptionProvider profileDescriptionProvider = new DescriptionProvider() {

            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A named set of subsystem configs");
                node.get(ATTRIBUTES, NAME, TYPE).set(ModelType.STRING);
                node.get(ATTRIBUTES, NAME, DESCRIPTION).set("The name of the profile");
                node.get(ATTRIBUTES, NAME, REQUIRED).set(true);
                node.get(ATTRIBUTES, NAME, MIN_LENGTH).set(1);
                node.get(CHILDREN, SUBSYSTEM, DESCRIPTION).set("The subsystems that make up the profile");
                node.get(CHILDREN, SUBSYSTEM, MIN_OCCURS).set(1);
                node.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION);
                return node;
            }
        };

    private ModelNode model;

    private ServiceContainer container;
    private ModelController controller;

    @Before
    public void setupController() throws InterruptedException {
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        ModelControllerService svc = new ModelControllerService(processState);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        svc.latch.await();
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
    }

    @Test
    public void testGetModelDescription() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, "profile", "test");
        operation.get(RECURSIVE).set(true);
        operation.get(OPERATIONS).set(true);
        ModelNode result = executeForResult(operation);

        ModelNode threadsDescription = result.get(CHILDREN, SUBSYSTEM, MODEL_DESCRIPTION, THREADS);
        assertTrue(threadsDescription.isDefined());

        ModelNode threadFactoryDescription = threadsDescription.get(CHILDREN, THREAD_FACTORY, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, threadFactoryDescription.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, threadFactoryDescription.require(ATTRIBUTES).require(GROUP_NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, threadFactoryDescription.require(ATTRIBUTES).require(THREAD_NAME_PATTERN).require(TYPE)
                .asType());
        assertEquals(ModelType.INT, threadFactoryDescription.require(ATTRIBUTES).require(PRIORITY).require(TYPE).asType());
        assertEquals(ModelType.LIST, threadFactoryDescription.require(ATTRIBUTES).require(PROPERTIES).require(TYPE).asType());

        ModelNode boundedQueueThreadPoolDesc = threadsDescription.get(CHILDREN, BOUNDED_QUEUE_THREAD_POOL, MODEL_DESCRIPTION,
                "*");
        assertEquals(ModelType.STRING, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.LIST, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(TYPE).asType());
        assertEquals(ModelType.PROPERTY, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(VALUE_TYPE).asType());
        assertEquals(ModelType.OBJECT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(TYPE)
                .asType());
        assertEquals(
                ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(COUNT)
                        .require(TYPE).asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(PER_CPU)
                        .require(TYPE).asType());
        assertEquals(ModelType.OBJECT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(CORE_THREADS).require(TYPE)
                .asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(CORE_THREADS).require(VALUE_TYPE).require(COUNT)
                        .require(TYPE).asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(CORE_THREADS).require(VALUE_TYPE).require(PER_CPU)
                        .require(TYPE).asType());
        assertEquals(ModelType.OBJECT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(QUEUE_LENGTH).require(TYPE)
                .asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(QUEUE_LENGTH).require(VALUE_TYPE).require(COUNT)
                        .require(TYPE).asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(QUEUE_LENGTH).require(VALUE_TYPE).require(PER_CPU)
                        .require(TYPE).asType());
        assertEquals(ModelType.OBJECT, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(TYPE)
                .asType());
        assertEquals(ModelType.LONG, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());
        assertEquals(ModelType.BOOLEAN, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(BLOCKING).require(TYPE).asType());
        assertEquals(ModelType.BOOLEAN, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(ALLOW_CORE_TIMEOUT)
                .require(TYPE).asType());
        assertEquals(ModelType.STRING, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(HANDOFF_EXECUTOR).require(TYPE)
                .asType());

        ModelNode queueLessThreadPoolDesc = threadsDescription.get(CHILDREN, QUEUELESS_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, queueLessThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, queueLessThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.LIST, queueLessThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(TYPE).asType());
        assertEquals(ModelType.PROPERTY, queueLessThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(VALUE_TYPE).asType());
        assertEquals(
                ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(COUNT)
                        .require(TYPE).asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(PER_CPU)
                        .require(TYPE).asType());
        assertEquals(ModelType.LONG, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());
        assertEquals(ModelType.BOOLEAN, queueLessThreadPoolDesc.require(ATTRIBUTES).require(BLOCKING).require(TYPE).asType());
        assertEquals(ModelType.STRING, queueLessThreadPoolDesc.require(ATTRIBUTES).require(HANDOFF_EXECUTOR).require(TYPE)
                .asType());

        ModelNode scheduledThreadPoolDesc = threadsDescription.get(CHILDREN, SCHEDULED_THREAD_POOL, MODEL_DESCRIPTION, "*");
        assertEquals(ModelType.STRING, scheduledThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, scheduledThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.LIST, scheduledThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(TYPE).asType());
        assertEquals(ModelType.PROPERTY, scheduledThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(VALUE_TYPE).asType());
        assertEquals(
                ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(COUNT)
                        .require(TYPE).asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(PER_CPU)
                        .require(TYPE).asType());
        assertEquals(ModelType.LONG, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());

        ModelNode unboundedThreadPoolDesc = threadsDescription.get(CHILDREN, UNBOUNDED_QUEUE_THREAD_POOL, MODEL_DESCRIPTION,
                "*");
        assertEquals(ModelType.STRING, unboundedThreadPoolDesc.require(ATTRIBUTES).require(NAME).require(TYPE).asType());
        assertEquals(ModelType.STRING, unboundedThreadPoolDesc.require(ATTRIBUTES).require(THREAD_FACTORY).require(TYPE)
                .asType());
        assertEquals(ModelType.LIST, unboundedThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(TYPE).asType());
        assertEquals(ModelType.PROPERTY, unboundedThreadPoolDesc.require(ATTRIBUTES).require(PROPERTIES).require(VALUE_TYPE).asType());
        assertEquals(
                ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(COUNT)
                        .require(TYPE).asType());
        assertEquals(ModelType.INT,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(MAX_THREADS).require(VALUE_TYPE).require(PER_CPU)
                        .require(TYPE).asType());
        assertEquals(ModelType.LONG, boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE)
                .require(TIME).require(TYPE).asType());
        assertEquals(ModelType.STRING,
                boundedQueueThreadPoolDesc.require(ATTRIBUTES).require(KEEPALIVE_TIME).require(VALUE_TYPE).require(UNIT)
                        .require(TYPE).asType());

    }

    @Test
    public void testSimpleThreadFactory() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\"/>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("thread-factory");
        assertEquals(1, threadFactory.keys().size());
        assertEquals("test-factory", threadFactory.require("test-factory").require("name").asString());
    }

    @Test
    public void testSimpleThreadFactoryInvalidPriorityValue() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\" priority=\"12\"/>");
        assertEquals(2, updates.size());
        executeForResult(updates.get(0));
        try {
            executeForResult(updates.get(1));
            fail("Expected failure for invalid priority");
        } catch (OperationFailedException e) {
        }
    }

    @Test
    public void testFullThreadFactory() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\"" + "   group-name=\"test-group\""
                + "   thread-name-pattern=\"test-pattern\"" + "   priority=\"5\">" + "   <properties>"
                + "      <property name=\"propA\" value=\"valueA\"/>" + "      <property name=\"propB\" value=\"valueB\"/>"
                + "   </properties>" + "</thread-factory>");

        executeForResult(updates.get(0));
        controller.execute(updates.get(1), null, null, null);

        checkFullTreadFactory();
    }

    private void checkFullTreadFactory() {

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("thread-factory");
        assertEquals(1, threadFactory.keys().size());
        assertEquals("test-factory", threadFactory.require("test-factory").require("name").asString());
        assertEquals("test-group", threadFactory.require("test-factory").require("group-name").asString());
        assertEquals("test-pattern", threadFactory.require("test-factory").require("thread-name-pattern").asString());
        assertEquals(5, threadFactory.require("test-factory").require("priority").asInt());

        ModelNode props = threadFactory.require("test-factory").require("properties");
        assertTrue(props.isDefined());
        assertEquals(2, props.asList().size());
        for (ModelNode prop : props.asList()) {
            Property property = prop.asProperty();
            if (property.getName().equals("propA")) {
                assertEquals("valueA", property.getValue().asString());
            } else if (property.getName().equals("propB")) {
                assertEquals("valueB", property.getValue().asString());
            } else {
                fail("Unknown property " + property);
            }
        }
    }

    @Test
    public void testSeveralThreadFactories() throws Exception {
        List<ModelNode> updates = createSubSystem("<thread-factory name=\"test-factory\" group-name=\"A\"/>"
                + "<thread-factory name=\"test-factory1\" group-name=\"B\"/>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("thread-factory");
        assertEquals(2, threadFactory.keys().size());
        assertEquals("test-factory", threadFactory.require("test-factory").require("name").asString());
        assertEquals("A", threadFactory.require("test-factory").require("group-name").asString());
        assertEquals("test-factory1", threadFactory.require("test-factory1").require("name").asString());
        assertEquals("B", threadFactory.require("test-factory1").require("group-name").asString());
    }

    @Test
    public void testSimpleUnboundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<unbounded-queue-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</unbounded-queue-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
    }

    @Test
    public void testFullUnboundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                "<unbounded-queue-thread-pool name=\"test-pool\">" +
                "   <max-threads count=\"100\" per-cpu=\"5\"/>" +
                "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                "   <thread-factory name=\"test-factory\"/>" +
                "   <properties>" +
                "      <property name=\"propA\" value=\"valueA\"/>" +
                "      <property name=\"propB\" value=\"valueB\"/>" +
                "   </properties>" +
                "</unbounded-queue-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullUnboundedThreadPool();
    }

    private void checkFullUnboundedThreadPool() throws Exception {
        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
        assertEquals(new BigDecimal(100), threadPool.require("test-pool").require(MAX_THREADS).require(COUNT).asBigDecimal());
        assertEquals(new BigDecimal(5), threadPool.require("test-pool").require(MAX_THREADS).require(PER_CPU).asBigDecimal());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());

        ModelNode props = threadPool.require("test-pool").require("properties");
        assertTrue(props.isDefined());
        assertEquals(2, props.asList().size());
        for (ModelNode prop : props.asList()) {
            Property property = prop.asProperty();
            if (property.getName().equals("propA")) {
                assertEquals("valueA", property.getValue().asString());
            } else if (property.getName().equals("propB")) {
                assertEquals("valueB", property.getValue().asString());
            } else {
                fail("Unknown property " + property);
            }
        }
    }

    @Test
    public void testSeveralUnboundedQueueThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<unbounded-queue-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</unbounded-queue-thread-pool>"
                + "<unbounded-queue-thread-pool name=\"test-poolB\">" + "   <max-threads count=\"1\" per-cpu=\"2\"/>"
                + "</unbounded-queue-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("unbounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
        assertEquals("test-poolA", threadFactory.require("test-poolA").require("name").asString());
        assertEquals("test-poolB", threadFactory.require("test-poolB").require("name").asString());
    }

    @Test
    public void testSimpleScheduledThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<scheduled-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</scheduled-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("scheduled-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
    }

    @Test
    public void testFullScheduledThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                "<scheduled-thread-pool name=\"test-pool\">" +
                "   <max-threads count=\"100\" per-cpu=\"5\"/>" +
                "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                "   <thread-factory name=\"test-factory\"/>" +
                "   <properties>" +
                "      <property name=\"propA\" value=\"valueA\"/>" +
                "      <property name=\"propB\" value=\"valueB\"/>" +
                "   </properties>" +
                "</scheduled-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullScheduledThreadPool();
    }

    private void checkFullScheduledThreadPool() throws Exception {
        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("scheduled-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
        assertEquals(new BigDecimal(100), threadPool.require("test-pool").require(MAX_THREADS).require(COUNT).asBigDecimal());
        assertEquals(new BigDecimal(5), threadPool.require("test-pool").require(MAX_THREADS).require(PER_CPU).asBigDecimal());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).get(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).get(UNIT).asString());

        ModelNode props = threadPool.require("test-pool").require("properties");
        assertTrue(props.isDefined());
        assertEquals(2, props.asList().size());
        for (ModelNode prop : props.asList()) {
            Property property = prop.asProperty();
            if (property.getName().equals("propA")) {
                assertEquals("valueA", property.getValue().asString());
            } else if (property.getName().equals("propB")) {
                assertEquals("valueB", property.getValue().asString());
            } else {
                fail("Unknown property " + property);
            }
        }
    }

    @Test
    public void testSeveralScheduledThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<scheduled-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</scheduled-thread-pool>"
                + "<scheduled-thread-pool name=\"test-poolB\">" + "   <max-threads count=\"1\" per-cpu=\"2\"/>"
                + "</scheduled-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("scheduled-thread-pool");
        assertEquals(2, threadFactory.keys().size());
        assertEquals("test-poolA", threadFactory.require("test-poolA").require("name").asString());
        assertEquals("test-poolB", threadFactory.require("test-poolB").require("name").asString());
    }

    @Test
    public void testSimpleQueuelessThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</queueless-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
    }

    @Test
    public void testFullQueuelessThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                "<queueless-thread-pool name=\"test-pool\" blocking=\"true\">" +
                "   <max-threads count=\"100\" per-cpu=\"5\"/>" +
                "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                "   <thread-factory name=\"test-factory\"/>" +
                "   <handoff-executor name=\"other\"/>" +
                "   <properties>" +
                "      <property name=\"propA\" value=\"valueA\"/>" +
                "      <property name=\"propB\" value=\"valueB\"/>" +
                "   </properties>" +
                "</queueless-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullQueuelessThreadPool();

    }

    private void checkFullQueuelessThreadPool() throws Exception {
        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("queueless-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
        assertTrue(threadPool.require("test-pool").require("blocking").asBoolean());
        assertEquals(new BigDecimal(100), threadPool.require("test-pool").require(MAX_THREADS).require(COUNT).asBigDecimal());
        assertEquals(new BigDecimal(5), threadPool.require("test-pool").require(MAX_THREADS).require(PER_CPU).asBigDecimal());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
        assertEquals("other", threadPool.require("test-pool").require("handoff-executor").asString());

        ModelNode props = threadPool.require("test-pool").require("properties");
        assertTrue(props.isDefined());
        assertEquals(2, props.asList().size());
        for (ModelNode prop : props.asList()) {
            Property property = prop.asProperty();
            if (property.getName().equals("propA")) {
                assertEquals("valueA", property.getValue().asString());
            } else if (property.getName().equals("propB")) {
                assertEquals("valueB", property.getValue().asString());
            } else {
                fail("Unknown property " + property);
            }
        }

    }

    @Test
    public void testSeveralQueuelessThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<queueless-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "</queueless-thread-pool>"
                + "<queueless-thread-pool name=\"test-poolB\">" + "   <max-threads count=\"1\" per-cpu=\"2\"/>"
                + "</queueless-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("queueless-thread-pool");
        assertEquals(2, threadFactory.keys().size());
        assertEquals("test-poolA", threadFactory.require("test-poolA").require("name").asString());
        assertEquals("test-poolB", threadFactory.require("test-poolB").require("name").asString());
    }

    @Test
    public void testSimpleBoundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-pool\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>");
        assertEquals(2, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
    }

    @Test
    public void testFullBoundedQueueThreadPool() throws Exception {
        List<ModelNode> updates = createSubSystem(
                "<thread-factory name=\"test-factory\"/>" +
                "<bounded-queue-thread-pool name=\"test-pool\" blocking=\"true\" allow-core-timeout=\"true\">" +
                "   <core-threads count=\"200\" per-cpu=\"15\"/>" +
                "   <max-threads count=\"100\" per-cpu=\"5\"/>" +
                "   <queue-length count=\"300\" per-cpu=\"25\"/>" +
                "   <keepalive-time time=\"1000\" unit=\"MILLISECONDS\"/>" +
                "   <thread-factory name=\"test-factory\"/>" +
                "   <handoff-executor name=\"other\"/>" +
                "   <properties>" +
                "      <property name=\"propA\" value=\"valueA\"/>" +
                "      <property name=\"propB\" value=\"valueB\"/>" +
                "   </properties>" +
                "</bounded-queue-thread-pool>");

        executeForResult(updates.get(0));
        executeForResult(updates.get(1));
        executeForResult(updates.get(2));

        checkFullBoundedQueueThreadPool();
    }

    private void checkFullBoundedQueueThreadPool() throws Exception {
        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadPool = subsystem.require("bounded-queue-thread-pool");
        assertEquals(1, threadPool.keys().size());
        assertEquals("test-pool", threadPool.require("test-pool").require("name").asString());
        assertTrue(threadPool.require("test-pool").require("blocking").asBoolean());
        assertTrue(threadPool.require("test-pool").require("allow-core-timeout").asBoolean());
        assertEquals(new BigDecimal(200), threadPool.require("test-pool").require(CORE_THREADS).require(COUNT).asBigDecimal());
        assertEquals(new BigDecimal(15), threadPool.require("test-pool").require(CORE_THREADS).require(PER_CPU).asBigDecimal());
        assertEquals(new BigDecimal(300), threadPool.require("test-pool").require(QUEUE_LENGTH).require(COUNT).asBigDecimal());
        assertEquals(new BigDecimal(25), threadPool.require("test-pool").require(QUEUE_LENGTH).require(PER_CPU).asBigDecimal());
        assertEquals(new BigDecimal(100), threadPool.require("test-pool").require(MAX_THREADS).require(COUNT).asBigDecimal());
        assertEquals(new BigDecimal(5), threadPool.require("test-pool").require(MAX_THREADS).require(PER_CPU).asBigDecimal());
        assertEquals(1000L, threadPool.require("test-pool").require(KEEPALIVE_TIME).require(TIME).asLong());
        assertEquals("MILLISECONDS", threadPool.require("test-pool").require(KEEPALIVE_TIME).require(UNIT).asString());
        assertEquals("other", threadPool.require("test-pool").require("handoff-executor").asString());

        ModelNode props = threadPool.require("test-pool").require("properties");
        assertTrue(props.isDefined());
        assertEquals(2, props.asList().size());
        for (ModelNode prop : props.asList()) {
            Property property = prop.asProperty();
            if (property.getName().equals("propA")) {
                assertEquals("valueA", property.getValue().asString());
            } else if (property.getName().equals("propB")) {
                assertEquals("valueB", property.getValue().asString());
            } else {
                fail("Unknown property " + property);
            }
        }
    }

    @Test
    public void testSeveralBoundedQueueThreadPools() throws Exception {
        List<ModelNode> updates = createSubSystem("<bounded-queue-thread-pool name=\"test-poolA\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>" + "<bounded-queue-thread-pool name=\"test-poolB\">"
                + "   <max-threads count=\"1\" per-cpu=\"2\"/>" + "   <queue-length count=\"1\" per-cpu=\"2\"/>"
                + "</bounded-queue-thread-pool>");
        assertEquals(3, updates.size());
        for (ModelNode update : updates) {
            try {
                executeForResult(update);
            } catch (OperationFailedException e) {
                throw new RuntimeException(e.getFailureDescription().toString());
            }
        }

        ModelNode subsystem = model.require("profile").require("test").require("subsystem").require("threads");
        ModelNode threadFactory = subsystem.require("bounded-queue-thread-pool");
        assertEquals(2, threadFactory.keys().size());
        assertEquals("test-poolA", threadFactory.require("test-poolA").require("name").asString());
        assertEquals("test-poolB", threadFactory.require("test-poolB").require("name").asString());
    }

    private ModelNode createOperation(String operationName, String... address) {
        ModelNode operation = new ModelNode();
        operation.get(OP).set(operationName);
        if (address.length > 0) {
            for (String addr : address) {
                operation.get(OP_ADDR).add(addr);
            }
        } else {
            operation.get(OP_ADDR).setEmptyList();
        }

        return operation;
    }

    static class TestExtensionContext implements ExtensionContext {
        final ManagementResourceRegistration testProfileRegistration;
        ManagementResourceRegistration createdRegistration;

        TestExtensionContext(ManagementResourceRegistration testProfileRegistration) {
            this.testProfileRegistration = testProfileRegistration;
        }

        @Override
        public ProcessType getProcessType() {
            return ProcessType.EMBEDDED_SERVER;
        }

        @Override
        public RunningMode getRunningMode() {
            return RunningMode.NORMAL;
        }

        @Override
        public boolean isRuntimeOnlyRegistrationValid() {
            return getProcessType().isServer() && getRunningMode() != RunningMode.ADMIN_ONLY;
        }

        @Override
        public SubsystemRegistration registerSubsystem(final String name) throws IllegalArgumentException {
            return new SubsystemRegistration() {
                @Override
                public ManagementResourceRegistration registerSubsystemModel(final DescriptionProvider descriptionProvider) {
                    if (descriptionProvider == null) {
                        throw new IllegalArgumentException("descriptionProvider is null");
                    }
                    PathElement pe = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, name);
                    return registerSubsystemModel(new SimpleResourceDefinition(pe, descriptionProvider));
                }

                @Override
                public ManagementResourceRegistration registerSubsystemModel(ResourceDefinition resourceDefinition) {
                    if (resourceDefinition == null) {
                        throw new IllegalArgumentException("resourceDefinition is null");
                    }
                    createdRegistration = testProfileRegistration.registerSubModel(resourceDefinition);
                    Assert.assertEquals("threads", name);
                    return createdRegistration;
                }

                @Override
                public ManagementResourceRegistration registerDeploymentModel(final DescriptionProvider descriptionProvider) {
                    throw new IllegalStateException("Not implemented");
                }

                @Override
                public ManagementResourceRegistration registerDeploymentModel(ResourceDefinition resourceDefinition) {
                    throw new UnsupportedOperationException("Not implemented");
                }

                @Override
                public void registerXMLElementWriter(XMLElementWriter<SubsystemMarshallingContext> writer) {
                    Assert.assertNotNull(writer);
                }
            };
        }

        @Override
        public SubsystemRegistration registerSubsystem(String name, int majorVersion, int minorVersion) {
            return registerSubsystem(name);
        }
    }

    static List<ModelNode> createSubSystem(String subsystemContents) throws XMLStreamException {
        final String xmlContent = "      <subsystem xmlns=\"urn:jboss:domain:threads:1.0\">" + subsystemContents
                + "      </subsystem>"; // +

        final Reader reader = new StringReader(xmlContent);
        XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(reader);

        XMLMapper xmlMapper = XMLMapper.Factory.create();
        xmlMapper.registerRootElement(new QName(org.jboss.as.threads.Namespace.CURRENT.getUriString(), "subsystem"),
                ThreadsParser.INSTANCE);

        List<ModelNode> updates = new ArrayList<ModelNode>();
        xmlMapper.parseDocument(updates, xmlReader);

        // Process subsystems
        for (final ModelNode update : updates) {
            // Process relative subsystem path address
            final ModelNode subsystemAddress = profileAddress.clone();
            for (final Property path : update.get(OP_ADDR).asPropertyList()) {
                subsystemAddress.add(path.getName(), path.getValue().asString());
            }
            update.get(OP_ADDR).set(subsystemAddress);
        }

        return updates;
    }

//    static class TestResultHandler implements ResultHandler {
//        ModelNode failureDescription;
//
//        @Override
//        public void handleResultFragment(String[] location, ModelNode result) {
//        }
//
//        @Override
//        public void handleResultComplete() {
//        }
//
//        @Override
//        public void handleCancellation() {
//        }
//
//        @Override
//        public void handleFailed(ModelNode failureDescription) {
//            this.failureDescription = failureDescription;
//        }
//
//        void clear() {
//            failureDescription = null;
//        }
//
//
//    }

    public class ModelControllerService extends AbstractControllerService {

        private final CountDownLatch latch = new CountDownLatch(1);

        ModelControllerService(final ControlledProcessState processState) {
            super(ProcessType.EMBEDDED_SERVER, new RunningModeControl(RunningMode.NORMAL), new TestConfigurationPersister(), processState, NULL_PROVIDER, null, ExpressionResolver.DEFAULT);
        }

        @Override
        public void start(StartContext context) throws StartException {
            super.start(context);
            latch.countDown();
        }

        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
            rootRegistration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
            rootRegistration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

            rootRegistration.registerOperationHandler("setup", new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.createResource(PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement("profile", "test")));
                    context.completeStep();
                }
            }, new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(Locale locale) {
                    return new ModelNode();
                }
            });

            ManagementResourceRegistration profileRegistration = rootRegistration.registerSubModel(PathElement.pathElement("profile"), profileDescriptionProvider);
            TestExtensionContext context = new TestExtensionContext(profileRegistration);
            ThreadsExtension extension = new ThreadsExtension();
            extension.initialize(context);
            Assert.assertNotNull(context.createdRegistration);
        }

    }

    private class TestConfigurationPersister implements ConfigurationPersister{

        @Override
        public PersistenceResource store(final ModelNode model, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
            return new PersistenceResource() {
                @Override
                public void commit() {
                    ThreadsSubsystemTestCase.this.model = model;
                }

                @Override
                public void rollback() {
                }
            };
        }

        @Override
        public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
        }

        @Override
        public List<ModelNode> load() throws ConfigurationPersistenceException {
            return Collections.emptyList();
        }

        @Override
        public void successfulBoot() throws ConfigurationPersistenceException {
        }

        @Override
        public String snapshot() {
            return null;
        }

        @Override
        public SnapshotInfo listSnapshots() {
            return NULL_SNAPSHOT_INFO;
        }

        @Override
        public void deleteSnapshot(String name) {
        }
    }

    /**
     * Override to get the actual result from the response.
     *
     * @param operation the operation to execute
     *
     * @return the response's "result" child node
     *
     * @throws OperationFailedException if the response outcome is "failed"
     */
    public ModelNode executeForResult(ModelNode operation) throws OperationFailedException {
        ModelNode rsp = controller.execute(operation, null, null, null);
        if (FAILED.equals(rsp.get(OUTCOME).asString())) {
            throw new OperationFailedException(rsp.get(FAILURE_DESCRIPTION));
        }
        return rsp.get(RESULT);
    }
}
