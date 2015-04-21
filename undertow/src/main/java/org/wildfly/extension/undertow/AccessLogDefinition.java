/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.ValueExpression;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class AccessLogDefinition extends PersistentResourceDefinition {


    protected static final SimpleAttributeDefinition PATTERN = new SimpleAttributeDefinitionBuilder(Constants.PATTERN, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("common"))
            .setValidator(new StringLengthValidator(1, true))
            .build();
    protected static final SimpleAttributeDefinition WORKER = new SimpleAttributeDefinitionBuilder(Constants.WORKER, ModelType.STRING)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .setDefaultValue(new ModelNode("default"))
            .build();
    protected static final SimpleAttributeDefinition PREFIX = new SimpleAttributeDefinitionBuilder(Constants.PREFIX, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("access_log"))
            .setValidator(new StringLengthValidator(1, true))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition SUFFIX = new SimpleAttributeDefinitionBuilder(Constants.SUFFIX, ModelType.STRING, true)
            .setDefaultValue(new ModelNode(".log"))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition ROTATE = new SimpleAttributeDefinitionBuilder(Constants.ROTATE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .build();
    protected static final SimpleAttributeDefinition DIRECTORY = new SimpleAttributeDefinitionBuilder(Constants.DIRECTORY, ModelType.STRING)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1, true))
            .setDefaultValue(new ModelNode(new ValueExpression("${jboss.server.log.dir}")))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(Constants.RELATIVE_TO, ModelType.STRING)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1, true))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition USE_SERVER_LOG = new SimpleAttributeDefinitionBuilder(Constants.USE_SERVER_LOG, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
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
            RELATIVE_TO
    );
    static final AccessLogDefinition INSTANCE = new AccessLogDefinition();
    private final List<AccessConstraintDefinition> accessConstraints;


    private AccessLogDefinition() {
        super(UndertowExtension.PATH_ACCESS_LOG, UndertowExtension.getResolver(Constants.ACCESS_LOG), new AccessLogAdd(), ReloadRequiredRemoveStepHandler.INSTANCE);
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
