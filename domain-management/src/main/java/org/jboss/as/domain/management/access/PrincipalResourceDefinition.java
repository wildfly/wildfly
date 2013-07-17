/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXCLUDE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE;

import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelType;

/**
 * A {@link ResourceDefinition} representing a Principal either specified in the include or exclude list of a role mapping.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PrincipalResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition TYPE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.TYPE, ModelType.STRING, false)
            .build();

    public static final SimpleAttributeDefinition REALM = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.REALM, ModelType.STRING, true)
            .build();

    public static final SimpleAttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .build();

    private PrincipalResourceDefinition(final PathElement pathElement) {
        super(pathElement, DomainManagementResolver.getResolver("core.access-control.role-mapping.principal"),
                PrincipalAdd.INSTANCE, PrincipalRemove.INSTANCE);
    }

    static PrincipalResourceDefinition includeResourceDefinition() {
        return new PrincipalResourceDefinition(PathElement.pathElement(INCLUDE));
    }

    static PrincipalResourceDefinition excludeResourceDefinition() {
        return new PrincipalResourceDefinition(PathElement.pathElement(EXCLUDE));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ModelOnlyWriteAttributeHandler(TYPE, REALM, NAME);
        resourceRegistration.registerReadWriteAttribute(TYPE, null, writeHandler); // TODO - This will need validating to ensure it is only group or user.
        resourceRegistration.registerReadWriteAttribute(REALM, null, writeHandler);
        resourceRegistration.registerReadWriteAttribute(NAME, null, writeHandler);
    }

}
