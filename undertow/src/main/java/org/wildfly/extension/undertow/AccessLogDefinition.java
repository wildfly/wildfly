/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class AccessLogDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.ACCESS_LOG);
    static final RuntimeCapability<Void> ACCESS_LOG_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_ACCESS_LOG, true, AccessLogService.class)
              .setDynamicNameMapper(DynamicNameMappers.GRAND_PARENT)
              .build();


    protected static final SimpleAttributeDefinition PATTERN = new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("common"))
            .setValidator(new StringLengthValidator(1, true))
            .setRestartAllServices()
            .build();
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .setCapabilityReference(Capabilities.REF_IO_WORKER)
            .build();
    protected static final SimpleAttributeDefinition PREFIX = new SimpleAttributeDefinitionBuilder(Constants.PREFIX, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("access_log."))
            .setValidator(new StringLengthValidator(1, true))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    protected static final SimpleAttributeDefinition SUFFIX = new SimpleAttributeDefinitionBuilder(Constants.SUFFIX, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("log"))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    protected static final SimpleAttributeDefinition ROTATE = new SimpleAttributeDefinitionBuilder(Constants.ROTATE, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    protected static final SimpleAttributeDefinition DIRECTORY = new SimpleAttributeDefinitionBuilder(Constants.DIRECTORY, ModelType.STRING)
            .setRequired(false)
            .setValidator(new StringLengthValidator(1, true))
            .setDefaultValue(new ModelNode(new ValueExpression("${jboss.server.log.dir}")))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING)
            .setRequired(false)
            .setValidator(new StringLengthValidator(1, true))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition USE_SERVER_LOG = new SimpleAttributeDefinitionBuilder(Constants.USE_SERVER_LOG, ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition EXTENDED = new SimpleAttributeDefinitionBuilder(Constants.EXTENDED, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition PREDICATE = new SimpleAttributeDefinitionBuilder(Constants.PREDICATE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(PredicateValidator.INSTANCE)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition CLOSE_RETRY_COUNT = new SimpleAttributeDefinitionBuilder(Constants.CLOSE_RETRY_COUNT, ModelType.INT, true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(60))
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1))
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition CLOSE_RETRY_DELAY = new SimpleAttributeDefinitionBuilder(Constants.CLOSE_RETRY_DELAY, ModelType.INT, true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(50))
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1))
            .setRestartAllServices()
            .build();

    static final Collection<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(
            // IMPORTANT -- keep these in xsd order as this order controls marshalling
            WORKER,
            PATTERN,
            PREFIX,
            SUFFIX,
            ROTATE,
            DIRECTORY,
            USE_SERVER_LOG,
            RELATIVE_TO,
            EXTENDED,
            PREDICATE,
            CLOSE_RETRY_COUNT,
            CLOSE_RETRY_DELAY
    );
    private final List<AccessConstraintDefinition> accessConstraints;

    AccessLogDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getValue()))
                .setAddHandler(AccessLogAdd.INSTANCE)
                .setRemoveHandler(AccessLogRemove.INSTANCE)
                .setCapabilities(ACCESS_LOG_CAPABILITY)
        );
        SensitivityClassification sc = new SensitivityClassification(UndertowExtension.SUBSYSTEM_NAME, "web-access-log", false, false, false);
        this.accessConstraints = new SensitiveTargetAccessConstraintDefinition(sc).wrapAsList();
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        //noinspection unchecked
        return (Collection) ATTRIBUTES;
    }
}
