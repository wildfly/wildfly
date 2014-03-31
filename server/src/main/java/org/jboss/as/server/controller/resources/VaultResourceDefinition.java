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
package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.MapAttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.services.security.AbstractVaultReader;
import org.jboss.as.server.services.security.VaultAddHandler;
import org.jboss.as.server.services.security.VaultRemoveHandler;
import org.jboss.as.server.services.security.VaultWriteAttributeHandler;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class VaultResourceDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CORE_SERVICE, VAULT);

    public static final SimpleAttributeDefinition CODE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CODE, ModelType.STRING, true)
            .addFlag(Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .setAllowExpression(true)
            .build();

    public static final SimpleAttributeDefinition MODULE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.MODULE, ModelType.STRING, true)
            .addFlag(Flag.RESTART_ALL_SERVICES)
            .setValidator(new ModelTypeValidator(ModelType.STRING, true))
            .setAllowExpression(true)
            .setRequires(CODE.getName())
            .build();


    public static final PropertiesAttributeDefinition VAULT_OPTIONS = new PropertiesAttributeDefinition.Builder(ModelDescriptionConstants.VAULT_OPTIONS, true)
            .addFlag(Flag.RESTART_ALL_SERVICES)
            .setAllowExpression(true)
            .setCorrector(MapAttributeDefinition.LIST_TO_MAP_CORRECTOR)
            .setValidator(new StringLengthValidator(1, true, true))
            .build();

    public static AttributeDefinition[] ALL_ATTRIBUTES = new AttributeDefinition[]{CODE, MODULE, VAULT_OPTIONS};

    private final List<AccessConstraintDefinition> accessConstraints;

    public VaultResourceDefinition(AbstractVaultReader vaultReader) {
        super(PATH,
                ServerDescriptions.getResourceDescriptionResolver(VAULT),
                new VaultAddHandler(vaultReader),
                new VaultRemoveHandler(vaultReader));
        this.accessConstraints = SensitiveTargetAccessConstraintDefinition.SECURITY_VAULT.wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        VaultWriteAttributeHandler write = new VaultWriteAttributeHandler(ALL_ATTRIBUTES);
        for (AttributeDefinition def : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, write);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }
}
