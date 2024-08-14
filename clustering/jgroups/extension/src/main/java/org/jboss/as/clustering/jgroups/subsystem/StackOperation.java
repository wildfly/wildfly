/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.subsystem.resource.executor.RuntimeOperation;

/**
 * @author Paul Ferraro
 */
public enum StackOperation implements RuntimeOperation<ChannelFactory> {

    EXPORT_NATIVE_CONFIGURATION("export-native-configuration", ModelType.STRING) {
        @Override
        public ModelNode execute(ExpressionResolver expressionResolver, ModelNode operation, ChannelFactory factory) throws OperationFailedException {
            // Create a temporary channel, but don't connect it
            try (JChannel channel = factory.createChannel(UUID.randomUUID().toString())) {
                // ProtocolStack.printProtocolSpecAsXML() is very hacky and only works on an uninitialized stack
                List<Protocol> protocols = channel.getProtocolStack().getProtocols();
                Collections.reverse(protocols);
                ProtocolStack stack = new ProtocolStack();
                stack.addProtocols(protocols);
                return new ModelNode(stack.printProtocolSpecAsXML());
            } catch (Exception e) {
                throw new OperationFailedException(e);
            }
        }
    },
    ;
    private final OperationDefinition definition;

    StackOperation(String name, ModelType replyType) {
        this.definition = new SimpleOperationDefinitionBuilder(name, JGroupsSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(JGroupsResourceRegistration.STACK.getPathElement())).setReplyType(replyType).setReadOnly().build();
    }

    @Override
    public OperationDefinition getOperationDefinition() {
        return this.definition;
    }
}
