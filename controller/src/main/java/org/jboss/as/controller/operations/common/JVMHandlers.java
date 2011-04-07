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

package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.JVMDescriptions;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public final class JVMHandlers {

    public static final String JVM_AGENT_LIB = "agent-lib";
    public static final String JVM_AGENT_PATH = "agent-path";
    public static final String JVM_DEBUG_ENABLED = "debug-enabled";
    public static final String JVM_DEBUG_OPTIONS = "debug-options";
    public static final String JVM_ENV_CLASSPATH_IGNORED = "env-classpath-ignored";
    public static final String JVM_ENV_VARIABLES = "environment-variables";
    public static final String JVM_HEAP = "heap-size";
    public static final String JVM_MAX_HEAP = "max-heap-size";
    public static final String JVM_JAVA_AGENT = "java-agent";
    public static final String JVM_JAVA_HOME = "java-home";
    public static final String JVM_OPTIONS = "jvm-options";
    public static final String JVM_OPTION = "jvm-option";
    public static final String ADD_JVM_OPTION = "add-jvm-option";
    public static final String JVM_PERMGEN = "permgen-size";
    public static final String JVM_MAX_PERMGEN = "max-permgen-size";
    public static final String JVM_STACK = "stack-size";
    public static final String SIZE = "size";
    public static final String MAX_SIZE = "max-size";

    static final String[] ATTRIBUTES = {JVM_AGENT_LIB, JVM_AGENT_PATH, JVM_DEBUG_ENABLED, JVM_DEBUG_OPTIONS, JVM_ENV_CLASSPATH_IGNORED,
        JVM_ENV_VARIABLES, JVM_HEAP, JVM_MAX_HEAP, JVM_JAVA_AGENT, JVM_JAVA_HOME, JVM_OPTIONS, JVM_PERMGEN, JVM_MAX_PERMGEN,
        JVM_STACK};

    private static final OperationHandler writeHandler = WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE;
    private static final OperationHandler booleanWriteHandler = new ModelUpdateOperationHandler() {
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
            try {
                final String name = operation.require(NAME).asString();
                final boolean value = operation.get(VALUE).asBoolean();
                context.getSubModel().get(name).set(value);
                resultHandler.handleResultComplete();
            } catch (Exception e) {
                resultHandler.handleFailed(new ModelNode().set(e.getMessage()));
            }
            return new BasicOperationResult();
        }
    };

    public static void register(final ModelNodeRegistration registration) {

        registration.registerOperationHandler(JVMAddHandler.OPERATION_NAME, JVMAddHandler.INSTANCE, JVMAddHandler.INSTANCE, false);
        registration.registerOperationHandler(JVMRemoveHandler.OPERATION_NAME, JVMRemoveHandler.INSTANCE, JVMRemoveHandler.INSTANCE, false);

        registration.registerReadWriteAttribute(JVM_AGENT_LIB, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_AGENT_PATH, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_DEBUG_ENABLED, null, booleanWriteHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_DEBUG_OPTIONS, null, booleanWriteHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_ENV_CLASSPATH_IGNORED, null, booleanWriteHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_ENV_VARIABLES, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_HEAP, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_MAX_HEAP, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_JAVA_AGENT, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_JAVA_HOME, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_PERMGEN, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_MAX_PERMGEN, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_STACK, null, writeHandler, Storage.CONFIGURATION);

        registration.registerOperationHandler(JVMOptionAddHandler.OPERATION_NAME, JVMOptionAddHandler.INSTANCE, JVMOptionAddHandler.INSTANCE, false);
        registration.registerOperationHandler(JVMOptionRemoveHandler.OPERATION_NAME, JVMOptionRemoveHandler.INSTANCE, JVMOptionRemoveHandler.INSTANCE, false);
    }

    private JVMHandlers() {
        //
    }

    static final class JVMOptionAddHandler implements ModelQueryOperationHandler, DescriptionProvider {

        static final String OPERATION_NAME = ADD_JVM_OPTION;
        static final JVMOptionAddHandler INSTANCE = new JVMOptionAddHandler();

        /** {@inheritDoc} */
        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

            final ModelNode option = operation.require(JVM_OPTION);

            final ModelNode compensatingOperation = new ModelNode();
            compensatingOperation.get(OP).set(JVMOptionRemoveHandler.OPERATION_NAME);
            compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
            compensatingOperation.get(JVM_OPTION).set(option);

            context.getSubModel().get(JVM_OPTIONS).add(option);

            resultHandler.handleResultComplete();

            return new BasicOperationResult(compensatingOperation);
        }

        /** {@inheritDoc} */
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JVMDescriptions.getOptionAddOperation(locale);
        }

    }

    static final class JVMOptionRemoveHandler implements ModelQueryOperationHandler, DescriptionProvider {

        static final String OPERATION_NAME = "remove-jvm-option";
        static final JVMOptionRemoveHandler INSTANCE = new JVMOptionRemoveHandler();

        /** {@inheritDoc} */
        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {

            final ModelNode option = operation.require(JVM_OPTION);

            final ModelNode compensatingOperation = new ModelNode();
            compensatingOperation.get(OP).set(JVMOptionAddHandler.OPERATION_NAME);
            compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
            compensatingOperation.get(JVM_OPTION).set(option);
            //
            final ModelNode subModel = context.getSubModel();
            if(subModel.hasDefined(JVM_OPTIONS)) {
                final ModelNode values = subModel.get(JVM_OPTIONS);
                context.getSubModel().get(JVM_OPTIONS).setEmptyList();

                for(ModelNode value : values.asList()) {
                    if(! value.equals(option)) {
                        subModel.get(JVM_OPTIONS).add(value);
                    }
                }
            }

            resultHandler.handleResultComplete();

            return new BasicOperationResult(compensatingOperation);
        }

        /** {@inheritDoc} */
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JVMDescriptions.getOptionRemoveOperation(locale);
        }

    }

}
