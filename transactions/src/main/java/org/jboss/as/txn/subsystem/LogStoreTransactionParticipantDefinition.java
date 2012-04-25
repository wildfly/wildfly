package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LogStoreTransactionParticipantDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition[] PARTECIPANT_RW_ATTRIBUTE = new SimpleAttributeDefinition[]{
    };
    static final SimpleAttributeDefinition[] PARTECIPANT_ATTRIBUTE = new SimpleAttributeDefinition[]{
            LogStoreConstants.JMX_NAME, LogStoreConstants.PARTICIPANT_JNDI_NAME,
            LogStoreConstants.PARTICIPANT_STATUS, LogStoreConstants.RECORD_TYPE,
            LogStoreConstants.EIS_NAME, LogStoreConstants.EIS_VERSION};

    static final LogStoreTransactionParticipantDefinition INSTANCE = new LogStoreTransactionParticipantDefinition();

    private LogStoreTransactionParticipantDefinition() {
        super(TransactionExtension.PARTECIPANT_PATH,
                TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE, CommonAttributes.TRANSACTION, CommonAttributes.PARTICIPANT));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        final LogStoreParticipantRefreshHandler refreshHandler = LogStoreParticipantRefreshHandler.INSTANCE;

        DefaultOperationDescriptionProvider refreshDesc = new DefaultOperationDescriptionProvider(LogStoreConstants.REFRESH, getResourceDescriptionResolver());
        resourceRegistration.registerOperationHandler(LogStoreConstants.REFRESH, refreshHandler, refreshDesc);
        DefaultOperationDescriptionProvider recoverDesc = new DefaultOperationDescriptionProvider(LogStoreConstants.RECOVER, getResourceDescriptionResolver());
        resourceRegistration.registerOperationHandler(LogStoreConstants.RECOVER, new LogStoreParticipantRecoveryHandler(refreshHandler), recoverDesc);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (final SimpleAttributeDefinition attribute : PARTECIPANT_RW_ATTRIBUTE) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, new ParticipantWriteAttributeHandler(attribute));
        }
        for (final SimpleAttributeDefinition attribute : PARTECIPANT_ATTRIBUTE) {
            resourceRegistration.registerReadOnlyAttribute(attribute, null);
        }

    }
}
