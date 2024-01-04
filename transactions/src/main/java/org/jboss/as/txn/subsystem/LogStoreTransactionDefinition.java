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
class LogStoreTransactionDefinition extends SimpleResourceDefinition {
    private final LogStoreResource resource;

    static final SimpleAttributeDefinition[] TRANSACTION_ATTRIBUTE = new SimpleAttributeDefinition[]{
            LogStoreConstants.JMX_NAME, LogStoreConstants.TRANSACTION_ID,
            LogStoreConstants.TRANSACTION_AGE, LogStoreConstants.RECORD_TYPE};


    public LogStoreTransactionDefinition(final LogStoreResource resource) {
        super(new Parameters(TransactionExtension.TRANSACTION_PATH,
                TransactionExtension.getResourceDescriptionResolver(LogStoreConstants.LOG_STORE, CommonAttributes.TRANSACTION))
                .setRuntime()
        );
        this.resource = resource;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(new SimpleOperationDefinitionBuilder(LogStoreConstants.DELETE, getResourceDescriptionResolver()).build(), new LogStoreTransactionDeleteHandler(resource));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (SimpleAttributeDefinition def : TRANSACTION_ATTRIBUTE) {
            resourceRegistration.registerReadOnlyAttribute(def, null);
        }
    }
}
