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
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.metadata.ejb.jboss.IORTransportConfigMetaData;

/**
 * <p>
 * Defines a resource that encompasses the attributes used to configure the transport settings in generated IORs.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
class IORTransportConfigDefinition extends PersistentResourceDefinition {

    static final ParameterValidator VALIDATOR = new EnumValidator<IORTransportConfigValues>(
            IORTransportConfigValues.class, true, true);

    static final AttributeDefinition INTEGRITY =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_TRANSPORT_INTEGRITY, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(VALIDATOR)
                    .setAllowExpression(true)
                    .build();

    static final AttributeDefinition CONFIDENTIALITY =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_TRANSPORT_CONFIDENTIALITY, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(VALIDATOR)
                    .setAllowExpression(true)
                    .build();

    static final AttributeDefinition TRUST_IN_TARGET =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_TRANSPORT_TRUST_IN_TARGET, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<IORTransportConfigValues>(IORTransportConfigValues.class, true, true,
                            IORTransportConfigValues.NONE, IORTransportConfigValues.SUPPORTED))
                    .setAllowExpression(true)
                    .build();

    static final AttributeDefinition TRUST_IN_CLIENT =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_TRANSPORT_TRUST_IN_CLIENT, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(VALIDATOR)
                    .setAllowExpression(true)
                    .build();

    static final AttributeDefinition DETECT_REPLAY =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_TRANSPORT_DETECT_REPLAY, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(VALIDATOR)
                    .setAllowExpression(true)
                    .build();

    static final SimpleAttributeDefinition DETECT_MISORDERING =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_TRANSPORT_DETECT_MISORDERING, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(VALIDATOR)
                    .setAllowExpression(true)
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(INTEGRITY, CONFIDENTIALITY, TRUST_IN_TARGET,
            TRUST_IN_CLIENT, DETECT_REPLAY, DETECT_MISORDERING);

    static final IORTransportConfigDefinition INSTANCE = new IORTransportConfigDefinition();

    private IORTransportConfigDefinition() {
        super(PathElement.pathElement(JacORBSubsystemConstants.SETTING, JacORBSubsystemConstants.IOR_TRANSPORT_CONFIG),
                JacORBExtension.getResourceDescriptionResolver(JacORBSubsystemConstants.IOR_SETTINGS,
                        JacORBSubsystemConstants.IOR_TRANSPORT_CONFIG),
                new ReloadRequiredAddStepHandler(ATTRIBUTES),
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return Collections.singletonList((AccessConstraintDefinition) JacORBSubsystemDefinitions.JACORB_SECURITY_DEF);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    private enum IORTransportConfigValues {

        NONE("none"), SUPPORTED("supported"), REQUIRED("required");

        private String name;

        IORTransportConfigValues(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * <p>
     * Builds a {@code IORTransportConfigMetaData} using the specified {@code OperationContext} and {@code ModelNode}.
     * </p>
     *
     * @param context a reference to the {@code OperationContext}.
     * @param model a {@code ModelNode} containing the configured transport metadata.
     * @return the constructed {@code IORTransportConfigMetaData} or {@code null} if the specified model is undefined.
     * @throws OperationFailedException if an error occurs while creating the transport metadata,
     */
    protected IORTransportConfigMetaData getTransportConfigMetaData(final OperationContext context, final ModelNode model)
            throws OperationFailedException {

        if (!model.isDefined())
            return null;

        IORTransportConfigMetaData metaData = new IORTransportConfigMetaData();
        metaData.setIntegrity(INTEGRITY.resolveModelAttribute(context, model).asString());
        metaData.setConfidentiality(CONFIDENTIALITY.resolveModelAttribute(context, model).asString());
        metaData.setEstablishTrustInTarget(TRUST_IN_TARGET.resolveModelAttribute(context, model).asString());
        metaData.setEstablishTrustInClient(TRUST_IN_CLIENT.resolveModelAttribute(context, model).asString());
        metaData.setDetectMisordering(DETECT_MISORDERING.resolveModelAttribute(context, model).asString());
        metaData.setDetectReplay(DETECT_REPLAY.resolveModelAttribute(context, model).asString());
        return metaData;
    }
}
