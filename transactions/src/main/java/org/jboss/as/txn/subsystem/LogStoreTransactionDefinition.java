package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LogStoreTransactionDefinition extends SimpleResourceDefinition {
    private final LogStoreResource resource;

    static final SimpleAttributeDefinition[] TRANSACTION_ATTRIBUTE = new SimpleAttributeDefinition[]{
            LogStoreConstants.JMX_NAME, LogStoreConstants.TRANSACTION_ID,
            LogStoreConstants.TRANSACTION_AGE,
            LogStoreConstants.RECORD_TYPE};


    public LogStoreTransactionDefinition(final LogStoreResource resource) {
        super(TransactionExtension.TRANSACTION_PATH,
                TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE, CommonAttributes.TRANSACTION));
        this.resource = resource;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        DefaultOperationDescriptionProvider deleteDesc = new DefaultOperationDescriptionProvider(LogStoreConstants.DELETE, getResourceDescriptionResolver());
        resourceRegistration.registerOperationHandler(LogStoreConstants.DELETE, new LogStoreTransactionDeleteHandler(resource), deleteDesc);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (SimpleAttributeDefinition def : TRANSACTION_ATTRIBUTE) {
            resourceRegistration.registerReadWriteAttribute(def, null, new ReloadRequiredWriteAttributeHandler(def));
        }
    }
}
