/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LogStoreTransactionParticipantDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition[] PARTICIPANT_ATTRIBUTES = new SimpleAttributeDefinition[]{
            LogStoreConstants.JMX_NAME, LogStoreConstants.PARTICIPANT_JNDI_NAME,
            LogStoreConstants.PARTICIPANT_STATUS, LogStoreConstants.RECORD_TYPE,
            LogStoreConstants.EIS_NAME, LogStoreConstants.EIS_VERSION};

    LogStoreTransactionParticipantDefinition() {
        super(new Parameters(TransactionExtension.PARTICIPANT_PATH,
                TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE, CommonAttributes.TRANSACTION, CommonAttributes.PARTICIPANT))
                .setRuntime()
        );
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final LogStoreParticipantRefreshHandler refreshHandler = LogStoreParticipantRefreshHandler.INSTANCE;
        final LogStoreProbeHandler probeHandler = LogStoreProbeHandler.INSTANCE;

        resourceRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder(LogStoreConstants.REFRESH, getResourceDescriptionResolver()).build(), refreshHandler);
        resourceRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder(LogStoreConstants.RECOVER, getResourceDescriptionResolver()).build(), new LogStoreParticipantRecoveryHandler(refreshHandler));
        resourceRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder(LogStoreConstants.DELETE, getResourceDescriptionResolver()).build(), new LogStoreParticipantDeleteHandler(probeHandler));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final SimpleAttributeDefinition attribute : PARTICIPANT_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute, null);
        }

    }
}
