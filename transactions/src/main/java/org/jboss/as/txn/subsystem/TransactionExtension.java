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
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

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
    /**
     * The operation name to resolve the object store path
     */
    public static final String RESOLVE_OBJECT_STORE_PATH = "resolve-object-store-path";

    private static final String RESOURCE_NAME = TransactionExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(5, 0, 0);


    private static final ServiceName MBEAN_SERVER_SERVICE_NAME = ServiceName.JBOSS.append("mbean", "server");
    static final PathElement LOG_STORE_PATH = PathElement.pathElement(LogStoreConstants.LOG_STORE, LogStoreConstants.LOG_STORE);
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME);
    static final PathElement PARTICIPANT_PATH = PathElement.pathElement(LogStoreConstants.PARTICIPANTS);
    static final PathElement TRANSACTION_PATH = PathElement.pathElement(LogStoreConstants.TRANSACTIONS);


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, TransactionExtension.class.getClassLoader(), true, false);
    }

    static MBeanServer getMBeanServer(OperationContext context) {
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        final ServiceController<?> serviceController = serviceRegistry.getService(MBEAN_SERVER_SERVICE_NAME);
        if (serviceController == null) {
            throw TransactionLogger.ROOT_LOGGER.jmxSubsystemNotInstalled();
        }
        return (MBeanServer) serviceController.getValue();
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(ExtensionContext context) {
        TransactionLogger.ROOT_LOGGER.debug("Initializing Transactions Extension");
        final LogStoreResource resource = new LogStoreResource();
        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        final TransactionSubsystemRootResourceDefinition rootResourceDefinition = new TransactionSubsystemRootResourceDefinition(registerRuntimeOnly);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(rootResourceDefinition);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // Create the path resolver handlers
        if (context.getProcessType().isServer()) {
            // It's less than ideal to create a separate operation here, but this extension contains two relative-to attributes
            final ResolvePathHandler objectStorePathHandler = ResolvePathHandler.Builder.of(RESOLVE_OBJECT_STORE_PATH, context.getPathManager())
                   .setPathAttribute(TransactionSubsystemRootResourceDefinition.OBJECT_STORE_PATH)
                   .setRelativeToAttribute(TransactionSubsystemRootResourceDefinition.OBJECT_STORE_RELATIVE_TO)
                   .build();
            registration.registerOperationHandler(objectStorePathHandler.getOperationDefinition(), objectStorePathHandler);
        }


        ManagementResourceRegistration logStoreChild = registration.registerSubModel(new LogStoreDefinition(resource, registerRuntimeOnly));
        if (registerRuntimeOnly) {
            ManagementResourceRegistration transactionChild = logStoreChild.registerSubModel(new LogStoreTransactionDefinition(resource));
            transactionChild.registerSubModel(LogStoreTransactionParticipantDefinition.INSTANCE);
        }

        subsystem.registerXMLElementWriter(TransactionSubsystemXMLPersister.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_0.getUriString(), TransactionSubsystem10Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_1.getUriString(), TransactionSubsystem11Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_2.getUriString(), TransactionSubsystem12Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_3.getUriString(), TransactionSubsystem13Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_4.getUriString(), TransactionSubsystem14Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_1_5.getUriString(), TransactionSubsystem15Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_2_0.getUriString(), TransactionSubsystem20Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_3_0.getUriString(), TransactionSubsystem30Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_4_0.getUriString(), TransactionSubsystem40Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.TRANSACTIONS_5_0.getUriString(), TransactionSubsystem50Parser::new);
    }
}
