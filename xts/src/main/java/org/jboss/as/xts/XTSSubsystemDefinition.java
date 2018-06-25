/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class XTSSubsystemDefinition extends SimpleResourceDefinition {
    protected static final SimpleAttributeDefinition HOST_NAME =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.HOST, ModelType.STRING)
                    .setRequired(false)
                    .setDefaultValue(new ModelNode("default-host"))
                    .setAllowExpression(true)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();

    protected static final SimpleAttributeDefinition ENVIRONMENT_URL =
            new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.URL, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setXmlName(Attribute.URL.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                            //.setDefaultValue(new ModelNode().setExpression("http://${jboss.bind.address:127.0.0.1}:8080/ws-c11/ActivationService"))
                    .build();

    protected static final SimpleAttributeDefinition DEFAULT_CONTEXT_PROPAGATION =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_CONTEXT_PROPAGATION, ModelType.BOOLEAN, true)
                    .setAllowExpression(false)
                    .setXmlName(Attribute.ENABLED.getLocalName())
                    .setFlags(AttributeAccess.Flag.RESTART_JVM)
                    .build();

    protected static final SimpleAttributeDefinition ASYNC_REGISTRATION =
            new SimpleAttributeDefinitionBuilder(CommonAttributes.ASYNC_REGISTRATION, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .setXmlName(Attribute.ENABLED.getLocalName())
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES) // we need to register new WS endpoints
            .setDefaultValue(new ModelNode(false))
            .build();

    @Deprecated //just legacy support
    private static final ObjectTypeAttributeDefinition ENVIRONMENT = ObjectTypeAttributeDefinition.
            Builder.of(CommonAttributes.XTS_ENVIRONMENT, ENVIRONMENT_URL)
            .setDeprecated(ModelVersion.create(2,0,0))
            .build();


    protected XTSSubsystemDefinition() {
        super(XTSExtension.SUBSYSTEM_PATH,
                XTSExtension.getResourceDescriptionResolver(null),
                XTSSubsystemAdd.INSTANCE,
                XTSSubsystemRemove.INSTANCE);
    }

    /**
     * {@inheritDoc}
     * Registers an add operation handler or a remove operation handler if one was provided to the constructor.
     */
    @Override
    public void registerOperations(ManagementResourceRegistration registration) {
        super.registerOperations(registration);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(HOST_NAME, null, new ReloadRequiredWriteAttributeHandler(HOST_NAME));
        resourceRegistration.registerReadWriteAttribute(ENVIRONMENT_URL, null, new ReloadRequiredWriteAttributeHandler(ENVIRONMENT_URL));
        resourceRegistration.registerReadWriteAttribute(DEFAULT_CONTEXT_PROPAGATION, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_CONTEXT_PROPAGATION));
        resourceRegistration.registerReadWriteAttribute(ASYNC_REGISTRATION, null, new ReloadRequiredWriteAttributeHandler(ASYNC_REGISTRATION));
        //this here just for legacy support!
        resourceRegistration.registerReadOnlyAttribute(ENVIRONMENT, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode url = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get(ModelDescriptionConstants.URL);
                context.getResult().get(ModelDescriptionConstants.URL).set(url);
            }
        });
    }
}
