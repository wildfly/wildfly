/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.security;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class MappingModuleDefinition extends SimpleResourceDefinition {

    protected static final SimpleAttributeDefinition CODE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CODE, ModelType.STRING)
            .setRequired(true)
            .build();

    protected static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.TYPE, ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition MODULE = LoginModuleResourceDefinition.MODULE;

    protected static final PropertiesAttributeDefinition MODULE_OPTIONS = new PropertiesAttributeDefinition.Builder(Constants.MODULE_OPTIONS, true)
            .setAllowExpression(true)
            .build();
    private static final AttributeDefinition[] ATTRIBUTES = {CODE, TYPE, MODULE, MODULE_OPTIONS};


    MappingModuleDefinition(String key) {
        super(PathElement.pathElement(key),
                SecurityExtension.getResourceDescriptionResolver(Constants.MAPPING_MODULE),
                null,
                new SecurityDomainReloadRemoveHandler()
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        super.registerAddOperation(resourceRegistration, new AbstractAddStepHandler() {
            @Override
            protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                for (AttributeDefinition attr : getAttributes()) {
                    attr.validateAndSet(operation, model);
                }
            }
        }, OperationEntry.Flag.RESTART_NONE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        SecurityDomainReloadWriteHandler writeHandler = new SecurityDomainReloadWriteHandler(getAttributes());
        for (AttributeDefinition attr : getAttributes()) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    public AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }


}
