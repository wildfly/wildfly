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

package org.jboss.as.web;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Jean-Frederic Clere
 */
public class WebValveDefinition extends SimpleResourceDefinition {
    protected static final WebValveDefinition INSTANCE = new WebValveDefinition();

    protected static final SimpleAttributeDefinition MODULE = new SimpleAttributeDefinitionBuilder(Constants.MODULE, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();

    protected static final SimpleAttributeDefinition CLASS_NAME = new SimpleAttributeDefinitionBuilder(Constants.CLASS_NAME, ModelType.STRING)
            .setAllowNull(false)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1))
            .build();

    protected static final SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(Constants.ENABLED, ModelType.BOOLEAN)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .build();

    protected static final SimpleAttributeDefinition[] ATTRIBUTES = { MODULE, CLASS_NAME, ENABLED};

    protected static final PropertiesAttributeDefinition PARAMS = new PropertiesAttributeDefinition.Builder(Constants.PARAM, true)
            .setAllowExpression(true)
            .build();

    static final SimpleAttributeDefinition PARAM_NAME = new SimpleAttributeDefinitionBuilder(Constants.PARAM_NAME, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    static final SimpleAttributeDefinition PARAM_VALUE = new SimpleAttributeDefinitionBuilder(Constants.PARAM_VALUE, ModelType.STRING, true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true))
            .build();

    protected static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(Constants.PATH, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setValidator(new StringLengthValidator(1, true, true))
            .build();


    private static final OperationDefinition ADD_PARAM = new SimpleOperationDefinitionBuilder("add-param", WebExtension.getResourceDescriptionResolver("valve.param"))
            .setParameters(PARAM_NAME, PARAM_VALUE)
            .build();
    private static final OperationDefinition REMOVE_PARAM = new SimpleOperationDefinitionBuilder("remove-param", WebExtension.getResourceDescriptionResolver("valve.param"))
            .addParameter(PARAM_NAME)
            .build();

    private WebValveDefinition() {
        super(WebExtension.VALVE_PATH,
                WebExtension.getResourceDescriptionResolver(Constants.VALVE),
                WebValveAdd.INSTANCE,
                WebValveRemove.INSTANCE);
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration container) {
        super.registerOperations(container);
        container.registerOperationHandler(ADD_PARAM,WebValveParamAdd.INSTANCE);
        container.registerOperationHandler(REMOVE_PARAM,WebValveParamRemove.INSTANCE);
    }
    @Override
    public void registerAttributes(ManagementResourceRegistration valves) {
        for (AttributeDefinition def : ATTRIBUTES) {
            valves.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
        valves.registerReadWriteAttribute(PARAMS, null, new ReloadRequiredWriteAttributeHandler(PARAMS));
    }
}
