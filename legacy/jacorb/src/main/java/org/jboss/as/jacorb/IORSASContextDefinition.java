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
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.metadata.ejb.jboss.IORSASContextMetaData;

/**
 * <p>
 * Defines a resource that encompasses the attributes used to configure the secure attribute service (SAS) settings in
 * generated IORs.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class IORSASContextDefinition extends PersistentResourceDefinition {

    static final AttributeDefinition CALLER_PROPAGATION =
            new SimpleAttributeDefinitionBuilder(JacORBSubsystemConstants.IOR_SAS_CONTEXT_CALLER_PROPAGATION, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode(CallerPropagationValues.NONE.toString()))
                    .setValidator(new EnumValidator<CallerPropagationValues>(CallerPropagationValues.class, true, true))
                    .setAllowExpression(true)
                    .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = Arrays.asList(CALLER_PROPAGATION);

    static final IORSASContextDefinition INSTANCE = new IORSASContextDefinition();

    private IORSASContextDefinition() {
        super(PathElement.pathElement(JacORBSubsystemConstants.SETTING, JacORBSubsystemConstants.IOR_SAS_CONTEXT),
                JacORBExtension.getResourceDescriptionResolver(JacORBSubsystemConstants.IOR_SETTINGS,
                        JacORBSubsystemConstants.IOR_SAS_CONTEXT),
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

    private enum CallerPropagationValues {

        NONE("none"), SUPPORTED("supported");

        private String name;

        CallerPropagationValues(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * <p>
     * Builds a {@code IORSASContextMetaData} using the specified {@code OperationContext} and {@code ModelNode}.
     * </p>
     *
     * @param context a reference to the {@code OperationContext}.
     * @param model a {@code ModelNode} containing the configured secure attribute service (SAS) metadata.
     * @return the constructed {@code IORSASContextMetaData} or {@code null} if the specified model is undefined.
     * @throws OperationFailedException if an error occurs while creating the transport metadata,
     */
    protected IORSASContextMetaData getIORSASContextMetaData(final OperationContext context, final ModelNode model)
            throws OperationFailedException {

        if (!model.isDefined())
            return null;

        IORSASContextMetaData metaData = new IORSASContextMetaData();
        metaData.setCallerPropagation(CALLER_PROPAGATION.resolveModelAttribute(context, model).asString());
        return metaData;
    }
}