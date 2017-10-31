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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ResourceAdapterResourceDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOLVER = ResourceAdaptersExtension.getResourceDescriptionResolver(RESOURCEADAPTER_NAME);
    private static final OperationDefinition ACTIVATE_DEFINITION = new SimpleOperationDefinitionBuilder(Constants.ACTIVATE, RESOLVER).build();

    // The ManagedConnectionPool implementation used by default by versions < 4.0.0 (WildFly 10)


    private final boolean readOnly;
    private final boolean runtimeOnlyRegistrationValid;
    private final List<AccessConstraintDefinition> accessConstraints;

    public ResourceAdapterResourceDefinition(boolean readOnly, boolean runtimeOnlyRegistrationValid) {
        super(PathElement.pathElement(RESOURCEADAPTER_NAME), RESOLVER, readOnly ? null : RaAdd.INSTANCE, readOnly ? null : RaRemove.INSTANCE);
        this.readOnly = readOnly;
        this.runtimeOnlyRegistrationValid = runtimeOnlyRegistrationValid;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(ResourceAdaptersExtension.SUBSYSTEM_NAME, RESOURCEADAPTER_NAME);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(ACTIVATE_DEFINITION, RaActivate.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final AttributeDefinition attribute : CommonAttributes.RESOURCE_ADAPTER_ATTRIBUTE) {
            if (readOnly) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            } else {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new  ReloadRequiredWriteAttributeHandler(attribute));

            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConfigPropertyResourceDefinition(readOnly ? null : ConfigPropertyAdd.INSTANCE, readOnly ? null : ReloadRequiredRemoveStepHandler.INSTANCE));
        resourceRegistration.registerSubModel(new ConnectionDefinitionResourceDefinition(readOnly, runtimeOnlyRegistrationValid));
        resourceRegistration.registerSubModel(new AdminObjectResourceDefinition(readOnly));
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

}
