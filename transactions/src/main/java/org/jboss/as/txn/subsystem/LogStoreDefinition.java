package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.DefaultOperationDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class LogStoreDefinition extends SimpleResourceDefinition {
    static final SimpleAttributeDefinition[] LOG_STORE_ATTRIBUTE = new SimpleAttributeDefinition[]{
            LogStoreConstants.LOG_STORE_TYPE};


    public LogStoreDefinition(final LogStoreResource resource) {
        super(TransactionExtension.LOG_STORE_PATH,
                TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE),
                new LogStoreAddHandler(resource),
                ReloadRequiredRemoveStepHandler.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        DefaultOperationDescriptionProvider probeDesc = new DefaultOperationDescriptionProvider(LogStoreConstants.PROBE, getResourceDescriptionResolver());
        resourceRegistration.registerOperationHandler(LogStoreConstants.PROBE, LogStoreProbeHandler.INSTANCE, probeDesc);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (SimpleAttributeDefinition attr : LOG_STORE_ATTRIBUTE) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

}
