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

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ADMIN_OBJECTS_NAME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class AdminObjectResourceDefinition extends SimpleResourceDefinition {

    private final boolean readOnly;

    public AdminObjectResourceDefinition(boolean readOnly) {
        super(PathElement.pathElement(ADMIN_OBJECTS_NAME), ResourceAdaptersExtension.getResourceDescriptionResolver(ADMIN_OBJECTS_NAME), readOnly ? null : AdminObjectAdd.INSTANCE, readOnly ? null : ReloadRequiredRemoveStepHandler.INSTANCE);
        this.readOnly = readOnly;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        for (final AttributeDefinition attribute : CommonAttributes.ADMIN_OBJECTS_NODE_ATTRIBUTE) {
            if (readOnly) {
                resourceRegistration.registerReadOnlyAttribute(attribute, null);
            } else {
                resourceRegistration.registerReadWriteAttribute(attribute, null, new  ReloadRequiredWriteAttributeHandler(attribute));
            }
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new ConfigPropertyResourceDefinition(AOConfigPropertyAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE));
    }
}
