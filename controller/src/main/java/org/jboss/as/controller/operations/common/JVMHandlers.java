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

import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import org.jboss.as.controller.descriptions.common.JVMDescriptions;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
public final class JVMHandlers {

    public static final DescriptionProvider SERVER_MODEL_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JVMDescriptions.getServerJVMDescription(locale);
        }
    };

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

    static final String[] ATTRIBUTES = {JVM_AGENT_LIB, JVM_AGENT_PATH, JVM_ENV_CLASSPATH_IGNORED, JVM_ENV_VARIABLES,
            JVM_HEAP, JVM_MAX_HEAP, JVM_JAVA_AGENT, JVM_JAVA_HOME, JVM_OPTIONS, JVM_PERMGEN, JVM_MAX_PERMGEN, JVM_STACK};

    static final String[] SERVER_ATTRIBUTES = {JVM_DEBUG_ENABLED, JVM_DEBUG_OPTIONS};

    private static final OperationStepHandler writeHandler = WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE;
    private static final OperationStepHandler booleanWriteHandler = new OperationStepHandler() {

        @Override
        public void execute(OperationContext context, ModelNode operation) {
            try {
                final String name = operation.require(NAME).asString();
                final boolean value = operation.get(VALUE).asBoolean();
                ModelNode valNode = context.readModelForUpdate(PathAddress.EMPTY_ADDRESS).get(name);
                boolean oldVal = valNode.asBoolean();
                valNode.set(value);
            } catch (Exception e) {
                context.getFailureDescription().set(e.toString());
            }
            context.completeStep();
        }
    };

    public static void register(final ManagementResourceRegistration registration) {
        register(registration, false);
    }

    public static void register(final ManagementResourceRegistration registration, final boolean server) {

        registration.registerOperationHandler(JVMAddHandler.OPERATION_NAME, JVMAddHandler.INSTANCE, JVMAddHandler.INSTANCE, false);
        registration.registerOperationHandler(JVMRemoveHandler.OPERATION_NAME, JVMRemoveHandler.INSTANCE, JVMRemoveHandler.INSTANCE, false);

        registration.registerReadWriteAttribute(JVM_AGENT_LIB, null, writeHandler, Storage.CONFIGURATION);
        registration.registerReadWriteAttribute(JVM_AGENT_PATH, null, writeHandler, Storage.CONFIGURATION);
        if (server) {
            registration.registerReadWriteAttribute(JVM_DEBUG_ENABLED, null, booleanWriteHandler, Storage.CONFIGURATION);
            registration.registerReadWriteAttribute(JVM_DEBUG_OPTIONS, null, booleanWriteHandler, Storage.CONFIGURATION);
        }
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

    static final class JVMOptionAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

        static final String OPERATION_NAME = ADD_JVM_OPTION;
        static final JVMOptionAddHandler INSTANCE = new JVMOptionAddHandler();

        protected void populateModel(ModelNode operation, ModelNode model) {
            final ModelNode option = operation.require(JVM_OPTION);
            model.get(JVM_OPTIONS).add(option);
        }

        protected boolean requiresRuntime(OperationContext context) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JVMDescriptions.getOptionAddOperation(locale);
        }
    }

    static final class JVMOptionRemoveHandler extends AbstractRemoveStepHandler implements DescriptionProvider {

        static final String OPERATION_NAME = "remove-jvm-option";
        static final JVMOptionRemoveHandler INSTANCE = new JVMOptionRemoveHandler();

        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) {
            final ModelNode option = operation.require(JVM_OPTION);
            if (model.hasDefined(JVM_OPTIONS)) {
                final ModelNode values = model.get(JVM_OPTIONS);
                model.get(JVM_OPTIONS).setEmptyList();

                for (ModelNode value : values.asList()) {
                    if (!value.equals(option)) {
                        model.get(JVM_OPTIONS).add(value);
                    }
                }
            }
        }

        protected boolean requiresRuntime(OperationContext context) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return JVMDescriptions.getOptionRemoveOperation(locale);
        }
    }

}
