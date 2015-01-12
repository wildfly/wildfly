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
import org.jboss.as.clustering.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 *
 */
public class BackupSiteResourceDefinition extends SimpleResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement(ModelKeys.BACKUP, name);
    }

    static final SimpleAttributeDefinition FAILURE_POLICY = new SimpleAttributeDefinitionBuilder(ModelKeys.BACKUP_FAILURE_POLICY, ModelType.STRING, true)
            .setXmlName(Attribute.BACKUP_FAILURE_POLICY.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(BackupFailurePolicy.class, true, true))
            .setDefaultValue(new ModelNode().set(BackupFailurePolicy.WARN.name()))
            .build();

    static final SimpleAttributeDefinition STRATEGY = new SimpleAttributeDefinitionBuilder(ModelKeys.BACKUP_STRATEGY, ModelType.STRING, true)
            .setXmlName(Attribute.STRATEGY.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new EnumValidator<>(BackupStrategy.class, true, true))
            .setDefaultValue(new ModelNode().set(BackupStrategy.ASYNC.name()))
            .build();

    static final SimpleAttributeDefinition REPLICATION_TIMEOUT = new SimpleAttributeDefinitionBuilder(ModelKeys.TIMEOUT, ModelType.STRING, true)
            .setXmlName(Attribute.TIMEOUT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(10000L))
            .build();

    static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(ModelKeys.ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.ENABLED.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(true))
            .build();

    static final SimpleAttributeDefinition TAKE_OFFLINE_AFTER_FAILURES = new SimpleAttributeDefinitionBuilder(ModelKeys.TAKE_BACKUP_OFFLINE_AFTER_FAILURES, ModelType.INT, true)
            .setXmlName(Attribute.TAKE_BACKUP_OFFLINE_AFTER_FAILURES.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(0))
            .build();

    static final SimpleAttributeDefinition TAKE_OFFLINE_MIN_WAIT = new SimpleAttributeDefinitionBuilder(ModelKeys.TAKE_BACKUP_OFFLINE_MIN_WAIT, ModelType.INT, true)
            .setXmlName(Attribute.TAKE_BACKUP_OFFLINE_MIN_WAIT.getLocalName())
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode().set(0))
            .build();

    static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { FAILURE_POLICY, STRATEGY, REPLICATION_TIMEOUT, ENABLED, TAKE_OFFLINE_AFTER_FAILURES, TAKE_OFFLINE_MIN_WAIT };

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        // ResourceTransformationDescriptionBuilder builder = parent.addChildResource(WILDCARD_PATH);
    }

    private final boolean runtimeRegistration;

    BackupSiteResourceDefinition(final boolean runtimeRegistration) {
        super(WILDCARD_PATH, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.BACKUP), new ReloadRequiredAddStepHandler(ATTRIBUTES), ReloadRequiredRemoveStepHandler.INSTANCE);
        this.runtimeRegistration = runtimeRegistration;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registration) {
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(ATTRIBUTES);
        for (AttributeDefinition attribute: ATTRIBUTES) {
            registration.registerReadWriteAttribute(attribute, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        if (this.runtimeRegistration) {
            for (BackupSiteOperation operation : BackupSiteOperation.values()) {
                registration.registerOperationHandler(operation.getDefinition(), new BackupSiteOperationHandler(operation));
            }
        }
    }
}
