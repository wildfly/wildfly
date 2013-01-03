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
package org.jboss.as.osgi.parser;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.osgi.management.BundleResourceHandler;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BundleResource extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOLVER = OSGiResolvers.getResolver(ModelConstants.BUNDLE);
    private static final OperationDefinition[] OPERATIONS = new OperationDefinition[] {
        new SimpleOperationDefinitionBuilder(ModelConstants.START, RESOLVER).build(),
        new SimpleOperationDefinitionBuilder(ModelConstants.STOP, RESOLVER).build(),
    };

    public static final SimpleAttributeDefinition ID = createAttribute(ModelConstants.ID, ModelType.LONG, false);
    public static final SimpleAttributeDefinition LOCATION = createAttribute(ModelConstants.LOCATION, ModelType.STRING, false);
    public static final SimpleAttributeDefinition STARTLEVEL = createAttribute(ModelConstants.STARTLEVEL, ModelType.INT, true);
    public static final SimpleAttributeDefinition STATE = createAttribute(ModelConstants.STATE, ModelType.STRING, false);
    public static final SimpleAttributeDefinition SYMBOLIC_NAME = createAttribute(ModelConstants.SYMBOLIC_NAME, ModelType.STRING, false);
    public static final SimpleAttributeDefinition TYPE = createAttribute(ModelConstants.TYPE, ModelType.STRING, false);
    public static final SimpleAttributeDefinition VERSION = createAttribute(ModelConstants.VERSION, ModelType.STRING, false);

    static final SimpleAttributeDefinition[] ATTRIBUTES = new SimpleAttributeDefinition[] {
        ID,
        LOCATION,
        STARTLEVEL,
        STATE,
        SYMBOLIC_NAME,
        TYPE,
        VERSION
    };


    private static SimpleAttributeDefinition createAttribute(String name, ModelType type, boolean nillable) {
        return new SimpleAttributeDefinitionBuilder(name, type, nillable)
            .setStorageRuntime()
            .build();
    }

    public BundleResource() {
        super(PathElement.pathElement(ModelConstants.BUNDLE), RESOLVER);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        for (OperationDefinition op : OPERATIONS) {
            resourceRegistration.registerOperationHandler(op, BundleResourceHandler.INSTANCE);
        }
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (SimpleAttributeDefinition def : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(def, BundleResourceHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
    }
}
