/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector._private.Capabilities.JCA_NAMING_CAPABILITY;
import static org.jboss.as.connector.util.ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME;
import static org.jboss.as.connector.util.ConnectorServices.LOCAL_TRANSACTION_PROVIDER_CAPABILITY;
import static org.jboss.as.connector.util.ConnectorServices.TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY;
import static org.jboss.as.connector.util.ConnectorServices.TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaSubsystemRootDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, JcaExtension.SUBSYSTEM_NAME);

    static final RuntimeCapability<Void> TRANSACTION_INTEGRATION_CAPABILITY = RuntimeCapability.Builder.of(TRANSACTION_INTEGRATION_CAPABILITY_NAME, TransactionIntegration.class)
            .addRequirements(LOCAL_TRANSACTION_PROVIDER_CAPABILITY,
                    TRANSACTION_XA_RESOURCE_RECOVERY_REGISTRY_CAPABILITY,
                    TRANSACTION_SYNCHRONIZATION_REGISTRY_CAPABILITY)
            .build();

    private final boolean registerRuntimeOnly;


    private JcaSubsystemRootDefinition(final boolean registerRuntimeOnly) {
        super(new Parameters(PATH_SUBSYSTEM, JcaExtension.getResourceDescriptionResolver())
                .setAddHandler(JcaSubsystemAdd.INSTANCE)
                .setRemoveHandler(JcaSubSystemRemove.INSTANCE)
                .setCapabilities(JCA_NAMING_CAPABILITY, TRANSACTION_INTEGRATION_CAPABILITY)
        );
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    public static JcaSubsystemRootDefinition createInstance(final boolean registerRuntimeOnly) {
        return new JcaSubsystemRootDefinition(registerRuntimeOnly);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new JcaArchiveValidationDefinition());
        resourceRegistration.registerSubModel(new JcaBeanValidationDefinition());
        resourceRegistration.registerSubModel(new TracerDefinition());
        resourceRegistration.registerSubModel(new JcaCachedConnectionManagerDefinition());
        resourceRegistration.registerSubModel(JcaWorkManagerDefinition.createInstance(registerRuntimeOnly));
        resourceRegistration.registerSubModel(JcaDistributedWorkManagerDefinition.createInstance(registerRuntimeOnly));
        resourceRegistration.registerSubModel(new JcaBootstrapContextDefinition());
    }
}
