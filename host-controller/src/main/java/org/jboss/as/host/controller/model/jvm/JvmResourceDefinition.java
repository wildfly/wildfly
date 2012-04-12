/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller.model.jvm;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.host.controller.descriptions.HostDescriptionProviders;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JvmResourceDefinition extends SimpleResourceDefinition {

    public static final JvmResourceDefinition GLOBAL = new JvmResourceDefinition(false);

    public static final JvmResourceDefinition SERVER = new JvmResourceDefinition(true);

    private final boolean server;

    protected JvmResourceDefinition(boolean server) {
        super(PathElement.pathElement(ModelDescriptionConstants.JVM),
                new StandardResourceDescriptionResolver("jvm", HostDescriptionProviders.class.getPackage().getName() + ".LocalDescriptions", HostDescriptionProviders.class.getClassLoader(), true, false),
                new JVMAddHandler(JvmAttributes.getAttributes(server)),
                JVMRemoveHandler.INSTANCE);
        this.server = server;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : JvmAttributes.getAttributes(server)) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new WriteAttributeHandlers.AttributeDefinitionValidatingHandler(attr));
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(JVMOptionAddHandler.OPERATION_NAME, JVMOptionAddHandler.INSTANCE, JVMOptionAddHandler.INSTANCE, false);
        resourceRegistration.registerOperationHandler(JVMOptionRemoveHandler.OPERATION_NAME, JVMOptionRemoveHandler.INSTANCE, JVMOptionRemoveHandler.INSTANCE, false);
    }
}
