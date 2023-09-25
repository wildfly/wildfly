/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ejb3.remote.RemotingProfileService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for remoting profiles.
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class RemotingProfileResourceDefinition extends SimpleResourceDefinition {

    public static final String REMOTING_PROFILE_CAPABILITY_NAME = "org.wildfly.ejb3.remoting-profile";
    public static final RuntimeCapability<Void> REMOTING_PROFILE_CAPABILITY =
            RuntimeCapability.Builder.of(REMOTING_PROFILE_CAPABILITY_NAME, true, RemotingProfileService.class).build();

    public static final SimpleAttributeDefinition EXCLUDE_LOCAL_RECEIVER = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.EXCLUDE_LOCAL_RECEIVER, ModelType.BOOLEAN, true).setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE).build();

    public static final SimpleAttributeDefinition LOCAL_RECEIVER_PASS_BY_VALUE = new SimpleAttributeDefinitionBuilder(
            EJB3SubsystemModel.LOCAL_RECEIVER_PASS_BY_VALUE, ModelType.BOOLEAN, true).setAllowExpression(true).build();

    private static final AttributeDefinition[] ATTRIBUTES = new AttributeDefinition[] { EXCLUDE_LOCAL_RECEIVER, LOCAL_RECEIVER_PASS_BY_VALUE, StaticEJBDiscoveryDefinition.INSTANCE };
    static final RemotingProfileAdd ADD_HANDLER = new RemotingProfileAdd(ATTRIBUTES);

    RemotingProfileResourceDefinition() {
        super(new Parameters(EJB3SubsystemModel.REMOTING_PROFILE_PATH, EJB3Extension.getResourceDescriptionResolver(EJB3SubsystemModel.REMOTING_PROFILE))
                .setAddHandler(ADD_HANDLER)
                .setRemoveHandler(new ServiceRemoveStepHandler(ADD_HANDLER))
                .setCapabilities(REMOTING_PROFILE_CAPABILITY));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration subsystemRegistration) {
        subsystemRegistration.registerSubModel(new RemotingEjbReceiverDefinition());
        subsystemRegistration.registerSubModel(new RemoteHttpConnectionDefinition());
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new RemotingProfileResourceChildWriteAttributeHandler(attr));
        }
    }
}
