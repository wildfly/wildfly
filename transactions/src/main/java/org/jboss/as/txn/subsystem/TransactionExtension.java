/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import javax.management.MBeanServer;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.txn.TransactionMessages;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.txn.TransactionLogger.ROOT_LOGGER;

/**
 * The transaction management extension.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author Emanuel Muckenhuber
 * @author Scott Stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 * @author Mike Musgrove (mmusgrov@redhat.com) (C) 2012 Red Hat Inc.
 */
public class TransactionExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "transactions";

    private static final String RESOURCE_NAME = TransactionExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ServiceName MBEAN_SERVER_SERVICE_NAME = ServiceName.JBOSS.append("mbean", "server");

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, TransactionExtension.class.getClassLoader(), true, true);
    }

    static MBeanServer getMBeanServer(OperationContext context) {
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        final ServiceController<?> serviceController = serviceRegistry.getService(MBEAN_SERVER_SERVICE_NAME);
        if(serviceController == null) {
            throw TransactionMessages.MESSAGES.jmxSubsystemNotInstalled();
        }
        return (MBeanServer) serviceController.getValue();
    }

    /** {@inheritDoc} */
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debug("Initializing Transactions Extension");
        final LogStoreResource resource = new LogStoreResource();
        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new TransactionSubsystemRootResourceDefinition(registerRuntimeOnly));
        registration.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE, GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        PathElement logStorePath = PathElement.pathElement(LogStoreConstants.LOG_STORE, LogStoreConstants.LOG_STORE);
        ManagementResourceRegistration logStoreChild = registration.registerSubModel(logStorePath, LogStoreProviders.LOG_STORE_MODEL_CHILD);
        logStoreChild.registerOperationHandler(ModelDescriptionConstants.ADD, new LogStoreAddHandler(resource), LogStoreProviders.ADD_LOG_STORE_MODEL_CHILD);

        logStoreChild.registerOperationHandler(ModelDescriptionConstants.REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, LogStoreProviders.REMOVE_LOG_STORE_MODEL_CHILD);
        logStoreChild.registerOperationHandler(LogStoreConstants.PROBE, LogStoreProbeHandler.INSTANCE, LogStoreProviders.PROBE_OPERATION);

        PathElement transactionPath = PathElement.pathElement(LogStoreConstants.TRANSACTIONS);
        ManagementResourceRegistration transactionChild = logStoreChild.registerSubModel(transactionPath, LogStoreProviders.TRANSACTION_CHILD);
        transactionChild.registerOperationHandler(LogStoreConstants.DELETE, new LogStoreTransactionDeleteHandler(resource), LogStoreProviders.DELETE_OPERATION);

        PathElement partecipantPath = PathElement.pathElement(LogStoreConstants.PARTICIPANTS);
        ManagementResourceRegistration partecipantChild = transactionChild.registerSubModel(partecipantPath, LogStoreProviders.PARTECIPANT_CHILD);

        for (final SimpleAttributeDefinition attribute : LogStoreProviders.PARTECIPANT_RW_ATTRIBUTE) {
            partecipantChild.registerReadWriteAttribute(attribute, null, new ParticipantWriteAttributeHandler(attribute));
        }

        final LogStoreParticipantRefreshHandler refreshHandler = LogStoreParticipantRefreshHandler.INSTANCE;

        partecipantChild.registerOperationHandler(LogStoreConstants.REFRESH, refreshHandler, LogStoreProviders.REFRESH_OPERATION);
        partecipantChild.registerOperationHandler(LogStoreConstants.RECOVER, new LogStoreParticipantRecoveryHandler(refreshHandler), LogStoreProviders.RECOVER_OPERATION);

        subsystem.registerXMLElementWriter(TransactionSubsystem12Parser.INSTANCE);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_0.getUriString(), TransactionSubsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_1.getUriString(), TransactionSubsystem11Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_2.getUriString(), TransactionSubsystem12Parser.INSTANCE);
    }

}
