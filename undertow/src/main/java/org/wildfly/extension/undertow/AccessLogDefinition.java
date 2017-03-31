/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class AccessLogDefinition extends PersistentResourceDefinition {
    static final RuntimeCapability<Void> ACCESS_LOG_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_ACCESS_LOG, true, AccessLogService.class)
              .setDynamicNameMapper(path -> new String[]{
                      path.getParent().getParent().getLastElement().getValue(),
                      path.getParent().getLastElement().getValue(),
                      path.getLastElement().getValue()})
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
            .setDefaultValue(new ModelNode(true))
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
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition EXTENDED = new SimpleAttributeDefinitionBuilder(Constants.EXTENDED, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    protected static final SimpleAttributeDefinition PREDICATE = new SimpleAttributeDefinitionBuilder(Constants.PREDICATE, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(PredicateValidator.INSTANCE)
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
            PREDICATE
    );
    static final AccessLogDefinition INSTANCE = new AccessLogDefinition();
    private final List<AccessConstraintDefinition> accessConstraints;


    private AccessLogDefinition() {
        super(new Parameters(UndertowExtension.PATH_ACCESS_LOG, UndertowExtension.getResolver(Constants.ACCESS_LOG))
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
