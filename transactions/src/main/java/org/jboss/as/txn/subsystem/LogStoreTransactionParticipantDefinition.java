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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
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

        resourceRegistration.registerOperationHandler(new SimpleOperationDefinition(LogStoreConstants.REFRESH, getResourceDescriptionResolver()), refreshHandler);
        resourceRegistration.registerOperationHandler(new SimpleOperationDefinition(LogStoreConstants.RECOVER, getResourceDescriptionResolver()), new LogStoreParticipantRecoveryHandler(refreshHandler));
        resourceRegistration.registerOperationHandler(new SimpleOperationDefinition(LogStoreConstants.DELETE, getResourceDescriptionResolver()), new LogStoreParticipantDeleteHandler(probeHandler));
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
