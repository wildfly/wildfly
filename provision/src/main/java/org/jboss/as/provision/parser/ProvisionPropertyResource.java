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
package org.jboss.as.provision.parser;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 03-May-2013
 */
public class ProvisionPropertyResource extends SimpleResourceDefinition {

    static final PathElement PROPERTY_PATH = PathElement.pathElement(ModelConstants.PROPERTY);
    static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelConstants.VALUE, ModelType.STRING)
            .addFlag(Flag.RESTART_NONE)
            .setAllowExpression(true)
            .build();

    private final SubsystemState subsystemState;

    ProvisionPropertyResource(SubsystemState subsystemState) {
        super(PROPERTY_PATH, ProvisionResolvers.getResolver("provision.property"), new ProvisionFrameworkPropertyAdd(subsystemState), new ProvisionFrameworkPropertyRemove(subsystemState));
        this.subsystemState = subsystemState;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(VALUE, null, new ProvisionFrameworkPropertyWrite(subsystemState));
    }
}
