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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ReloadRequiredResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} implementation for the legacy mod_cluster SSL configuration resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author Radoslav Husar
 */
@Deprecated
class SSLResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement PATH = PathElement.pathElement("ssl", "configuration");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CA_CERTIFICATE_FILE("ca-certificate-file", ModelType.STRING, null),
        CA_REVOCATION_URL("ca-revocation-url", ModelType.STRING, null),
        CERTIFICATE_KEY_FILE("certificate-key-file", ModelType.STRING, new ModelNode().set(new ValueExpression("${user.home}/.keystore"))),
        CIPHER_SUITE("cipher-suite", ModelType.STRING, null),
        KEY_ALIAS("key-alias", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL);
            }
        },
        PASSWORD("password", ModelType.STRING, new ModelNode("changeit")) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.addAccessConstraint(SensitiveTargetAccessConstraintDefinition.CREDENTIAL);
            }
        },
        PROTOCOL("protocol", ModelType.STRING, new ModelNode("TLS")),
        ;

        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setRestartAllServices()
            ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    SSLResourceDefinition() {
        super(PATH, ModClusterExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH));

        this.setDeprecated(ModClusterModel.VERSION_5_0_0.getVersion());
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addExtraParameters(Attribute.class)
                ;

        for (Attribute attribute : Attribute.values()) {
            registration.registerReadWriteAttribute(attribute.getDefinition(), null, new ReloadRequiredWriteAttributeHandler() {
                @Override
                protected void validateUpdatedModel(OperationContext context, Resource model) {
                    context.addStep(new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext ctx, ModelNode op) throws OperationFailedException {
                            final ModelNode conf = ctx.readResourceFromRoot(ctx.getCurrentAddress().getParent(), false).getModel();
                            if (conf.hasDefined(ProxyConfigurationResourceDefinition.Attribute.SSL_CONTEXT.getName())) {
                                throw new OperationFailedException(ROOT_LOGGER.bothElytronAndLegacySslContextDefined());
                            }
                        }
                    }, OperationContext.Stage.MODEL);
                }
            });
        }

        new ReloadRequiredResourceRegistration(descriptor).register(registration);

        return registration;
    }


    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return ModClusterExtension.MOD_CLUSTER_SECURITY_DEF.wrapAsList();
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        // Nothing to transform
    }
}
