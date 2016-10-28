/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.configadmin.parser;

import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.configadmin.parser.ModelConstants.UPDATE;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ConfigurationResource extends SimpleResourceDefinition {

    static final PathElement PATH_ELEMENT = PathElement.pathElement(ModelConstants.CONFIGURATION);

    static final PropertiesAttributeDefinition ENTRIES = new PropertiesAttributeDefinition.Builder(ModelConstants.ENTRIES, false)
            .setAllowExpression(true)
            .setWrapXmlElement(false)
            .build();

    private static final OperationDefinition UPDATE_DEFINITION = new SimpleOperationDefinitionBuilder(UPDATE, ConfigAdminExtension.getResourceDescriptionResolver(ModelConstants.CONFIGURATION))
        .setParameters(ENTRIES)
        .build();

    public ConfigurationResource() {
        super(PATH_ELEMENT, ConfigAdminExtension.getResourceDescriptionResolver(ModelConstants.CONFIGURATION), ConfigurationAdd.INSTANCE, ModelOnlyRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        resourceRegistration.registerReadOnlyAttribute(ENTRIES, null);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(UPDATE_DEFINITION, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
            }
        });
    }

}
