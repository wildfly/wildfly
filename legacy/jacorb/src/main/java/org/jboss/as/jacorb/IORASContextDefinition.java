/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.metadata.ejb.jboss.IORASContextMetaData;

/**
 * <p>
 * Defines a resource that encompasses the attributes used to configure the authentication service (AS) settings in
 * generated IORs.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class IORASContextDefinition extends PersistentResourceDefinition {

    static final AttributeDefinition AUTH_METHOD =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_AS_CONTEXT_AUTH_METHOD, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(AuthMethodValues.USERNAME_PASSWORD.toString()))
                    .setValidator(new EnumValidator<AuthMethodValues>(AuthMethodValues.class, true, true))
                    .setAllowExpression(true)
                    .build();

    static final AttributeDefinition REALM =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_AS_CONTEXT_REALM, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
                    .setAllowExpression(true)
                    .build();

    static final AttributeDefinition REQUIRED =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_AS_CONTEXT_REQUIRED, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(false))
                    .setAllowExpression(true)
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(AUTH_METHOD, REALM, REQUIRED);

    static final IORASContextDefinition INSTANCE = new IORASContextDefinition();

    private IORASContextDefinition() {
        super(PathElement.pathElement(JacORBSubsystemConstants.SETTING, JacORBSubsystemConstants.IOR_AS_CONTEXT),
                JacORBExtension.getResourceDescriptionResolver(JacORBSubsystemConstants.IOR_SETTINGS,
                        JacORBSubsystemConstants.IOR_AS_CONTEXT),
                new ReloadRequiredAddStepHandler(ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.singletonList((AccessConstraintDefinition)JacORBSubsystemDefinitions.JACORB_SECURITY_DEF);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    private enum AuthMethodValues {

        NONE("none"), USERNAME_PASSWORD("username_password");

        private String name;

        AuthMethodValues(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * <p>
     * Builds a {@code IORASContextMetaData} using the specified {@code OperationContext} and {@code ModelNode}.
     * </p>
     *
     * @param context a reference to the {@code OperationContext}.
     * @param model a {@code ModelNode} containing the configured authentication service (AS) metadata.
     * @return the constructed {@code IORASContextMetaData} or {@code null} if the specified model is undefined.
     * @throws OperationFailedException if an error occurs while creating the transport metadata,
     */
    protected IORASContextMetaData getIORASContextMetaData(final OperationContext context, final ModelNode model)
            throws OperationFailedException {

        if (!model.isDefined())
            return null;

        IORASContextMetaData metaData = new IORASContextMetaData();
        metaData.setAuthMethod(AUTH_METHOD.resolveModelAttribute(context, model).asString());
        if (model.hasDefined(REALM.getName())) {
            metaData.setRealm(REALM.resolveModelAttribute(context, model).asString());
        }
        metaData.setRequired(REQUIRED.resolveModelAttribute(context, model).asBoolean());
        return metaData;
    }
}