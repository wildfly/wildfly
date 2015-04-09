/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.BackupConfiguration.BackupStrategy;
import org.infinispan.configuration.cache.BackupFailurePolicy;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.jboss.as.clustering.controller.OperationHandler;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.RestartParentAddHandler;
import org.jboss.as.clustering.controller.RestartParentRemoveHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Definition of a backup site resource.
 *
 * @author Paul Ferraro
 */
public class BackupResourceDefinition extends SimpleResourceDefinition implements Registration {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("backup", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        ENABLED("enabled", ModelType.BOOLEAN, new ModelNode(true), null),
        FAILURE_POLICY("failure-policy", ModelType.STRING, new ModelNode(BackupFailurePolicy.WARN.name()), new EnumValidator<>(BackupFailurePolicy.class, true, true)),
        STRATEGY("strategy", ModelType.STRING, new ModelNode(BackupStrategy.ASYNC.name()), new EnumValidator<>(BackupStrategy.class, true, true)),
        TAKE_OFFLINE_AFTER_FAILURES("after-failures", ModelType.INT, new ModelNode(1), null),
        TAKE_OFFLINE_MIN_WAIT("min-wait", ModelType.LONG, new ModelNode(0L), null),
        TIMEOUT("timeout", ModelType.LONG, new ModelNode(10000L), null),
        ;
        private final AttributeDefinition definition;

        private Attribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    .setValidator(validator)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // Nothing to transform
    }

    private final boolean runtimeRegistration;
    private final ResourceServiceBuilderFactory<SitesConfiguration> parentBuilderFactory;

    BackupResourceDefinition(ResourceServiceBuilderFactory<SitesConfiguration> parentBuilderFactory, boolean runtimeRegistration) {
        super(WILDCARD_PATH, new InfinispanResourceDescriptionResolver(WILDCARD_PATH));
        this.parentBuilderFactory = parentBuilderFactory;
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        new ReloadRequiredWriteAttributeHandler(Attribute.class).register(registration);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        new RestartParentAddHandler<>(this.getResourceDescriptionResolver(), this.parentBuilderFactory).addAttributes(Attribute.class).register(registration);
        new RestartParentRemoveHandler<>(this.getResourceDescriptionResolver(), this.parentBuilderFactory).register(registration);

        if (this.runtimeRegistration) {
            new OperationHandler<>(new BackupOperationExecutor(), BackupOperation.class).register(registration);
        }
    }

    @Override
    public void register(ManagementResourceRegistration registration) {
        registration.registerSubModel(this);
    }
}
