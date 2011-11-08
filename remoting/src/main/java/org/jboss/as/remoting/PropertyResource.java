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
package org.jboss.as.remoting;

import static org.jboss.as.remoting.CommonAttributes.PROPERTY;
import static org.jboss.as.remoting.CommonAttributes.VALUE;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PropertyResource extends SimpleResourceDefinition {

    static final PropertyResource INSTANCE = new PropertyResource();

    static final SimpleAttributeDefinition VALUE_ATTRIBUTE = new NamedValueAttributeDefinition(VALUE, Attribute.VALUE, null, ModelType.STRING, true);

    private PropertyResource() {
        super(PathElement.pathElement(PROPERTY),
                RemotingExtension.getResourceDescriptionResolver(PROPERTY),
                PropertyAdd.INSTANCE,
                PropertyRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(VALUE_ATTRIBUTE, null);
    }


}
