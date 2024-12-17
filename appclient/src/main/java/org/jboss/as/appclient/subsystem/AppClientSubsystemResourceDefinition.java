/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.appclient.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the App Client subsystem's root management resource.
 * <p/>
 * Note that in normal circumstances the app client subsystem will never be installed into a container that
 * provides access to the management API
 *
 * @author Stuart Douglas
 */
public class AppClientSubsystemResourceDefinition extends SimpleResourceDefinition {

    public static final RuntimeCapability<Void> APPCLIENT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.appclient", Void.class)
            //.addRequirements(ModelControllerClientFactory.SERVICE_DESCRIPTOR, Capabilities.MANAGEMENT_EXECUTOR) TODO determine why this breaks domain mode provisioning
            .build();

    public static final SimpleAttributeDefinition FILE =
            new SimpleAttributeDefinitionBuilder(Constants.FILE, ModelType.STRING, false)
                    .setAllowExpression(true).build();
    public static final SimpleAttributeDefinition DEPLOYMENT =
            new SimpleAttributeDefinitionBuilder(Constants.DEPLOYMENT, ModelType.STRING, true)
                    .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition HOST_URL =
            new SimpleAttributeDefinitionBuilder(Constants.HOST_URL, ModelType.STRING, true)
                    .setAllowExpression(true).build();

    public static final SimpleAttributeDefinition CONNECTION_PROPERTIES_URL =
            new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_PROPERTIES_URL, ModelType.STRING, true)
                    .setAllowExpression(true).build();

    public static final StringListAttributeDefinition PARAMETERS = new StringListAttributeDefinition.Builder(Constants.PARAMETERS)
            .setRequired(false)
            .setAllowExpression(true)
            .build();

    AppClientSubsystemResourceDefinition() {
        super(new Parameters(AppClientExtension.SUBSYSTEM_PATH, AppClientExtension.getResourceDescriptionResolver())
                .setAddHandler(AppClientSubsystemAdd.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_ALL_SERVICES)
        );
    }

    static final AttributeDefinition[] ATTRIBUTES = {
            FILE,
            DEPLOYMENT,
            PARAMETERS,
            CONNECTION_PROPERTIES_URL,
            HOST_URL,
    };

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attr, null);
        }
    }
}
