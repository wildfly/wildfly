/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.function.Function;

import org.jboss.as.clustering.jgroups.auth.BinaryAuthToken;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a digest authentication token for use by the AUTH protocol.
 * @author Paul Ferraro
 */
public class DigestAuthTokenResourceDefinitionRegistrar extends AuthTokenResourceDefinitionRegistrar<BinaryAuthToken> {

    enum Attribute implements AttributeDefinitionProvider {
        ALGORITHM("algorithm", ModelType.STRING, new ModelNode("SHA-256")),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setDefaultValue(defaultValue)
                    .setRequired(defaultValue == null)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    DigestAuthTokenResourceDefinitionRegistrar() {
        super(Token.DIGEST);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.provideAttributes(EnumSet.allOf(Attribute.class));
    }

    @Override
    public ServiceDependency<Function<byte[], BinaryAuthToken>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String algorithm = Attribute.ALGORITHM.resolveModelAttribute(context, model).asString();
        return ServiceDependency.of(new Function<>() {
            @Override
            public BinaryAuthToken apply(byte[] secret) {
                try {
                    MessageDigest digest = MessageDigest.getInstance(algorithm);
                    return new BinaryAuthToken(digest.digest(secret));
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }
}
