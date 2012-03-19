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
package org.jboss.as.controller.services.path;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class PathResourceDefinition extends SimpleResourceDefinition {

    private static final String SPECIFIED_PATH_RESOURCE_PREFIX = "specified_path";
    private static final String NAMED_PATH_RESOURCE_PREFIX = "named_path";

    static final PathElement PATH_ADDRESS = PathElement.pathElement(ModelDescriptionConstants.PATH);

    static final SimpleAttributeDefinition NAME =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING, false)
                .setValidator(new StringLengthValidator(1, false))
                .setStorageRuntime()
                .build();

    static final SimpleAttributeDefinition PATH_SPECIFIED =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PATH, ModelType.STRING, false)
                .setValidator(new StringLengthValidator(1, false))
                .build();

    static final SimpleAttributeDefinition PATH_NAMED =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PATH, ModelType.STRING, true)
                .setValidator(new StringLengthValidator(1, true))
                .build();

    static final SimpleAttributeDefinition RELATIVE_TO =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
                .setValidator(new StringLengthValidator(1, true))
                .build();

    static final SimpleAttributeDefinition READ_ONLY =
            SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.READ_ONLY, ModelType.STRING, true)
                .setDefaultValue(new ModelNode(false))
                .setStorageRuntime()
                .build();


    protected final PathManagerService pathManager;
    private final boolean specified;
    private final boolean services;

    private PathResourceDefinition(PathManagerService pathManager, ResourceDescriptionResolver resolver, PathAddHandler addHandler, PathRemoveHandler removeHandler, boolean specified, boolean services) {
        super(PATH_ADDRESS, resolver, addHandler, removeHandler);
        this.pathManager = pathManager;
        this.specified = specified;
        this.services = services;
    }

    public static PathResourceDefinition createSpecified(PathManagerService pathManager) {
        return new SpecifiedPathResourceDefinition(pathManager);
    }

    public static PathResourceDefinition createNamed(PathManagerService pathManager) {
        return new NamedPathResourceDefinition(pathManager);
    }

    public static PathResourceDefinition createSpecifiedNoServices(PathManagerService pathManager) {
        return new SpecifiedNoServicesPathResourceDefinition(pathManager);
    }

    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(NAME, null);
        resourceRegistration.registerReadWriteAttribute(RELATIVE_TO, null, new PathWriteAttributeHandler(pathManager, RELATIVE_TO, services));
        SimpleAttributeDefinition pathAttr = specified ? PATH_SPECIFIED : PATH_NAMED;
        resourceRegistration.registerReadWriteAttribute(pathAttr, null, new PathWriteAttributeHandler(pathManager, pathAttr, services));
        resourceRegistration.registerReadOnlyAttribute(READ_ONLY, null);
    }

    private static class SpecifiedPathResourceDefinition extends PathResourceDefinition {
        SpecifiedPathResourceDefinition(PathManagerService pathManager){
            super(pathManager,
                    ManagementDescription.getResourceDescriptionResolver(SPECIFIED_PATH_RESOURCE_PREFIX),
                    PathAddHandler.createSpecifiedInstance(pathManager),
                    PathRemoveHandler.createSpecifiedInstance(pathManager),
                    true,
                    true);
        }
    }

    private static class NamedPathResourceDefinition extends PathResourceDefinition {
        NamedPathResourceDefinition(PathManagerService pathManager){
            super(pathManager,
                    ManagementDescription.getResourceDescriptionResolver(NAMED_PATH_RESOURCE_PREFIX),
                    PathAddHandler.createNamedInstance(pathManager),
                    PathRemoveHandler.createNamedInstance(pathManager),
                    false,
                    false);
        }
    }

    private static class SpecifiedNoServicesPathResourceDefinition extends PathResourceDefinition {
        SpecifiedNoServicesPathResourceDefinition(PathManagerService pathManager){
            super(pathManager,
                    ManagementDescription.getResourceDescriptionResolver(SPECIFIED_PATH_RESOURCE_PREFIX),
                    PathAddHandler.createSpecifiedNoServicesInstance(pathManager),
                    PathRemoveHandler.createSpecifiedNoServicesInstance(pathManager),
                    true,
                    false);
        }
    }
}
