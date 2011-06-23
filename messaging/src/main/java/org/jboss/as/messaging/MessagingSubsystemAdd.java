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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_FULL_MESSAGE_POLICY;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BINDINGS_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_USER;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONSUME_NAME;
import static org.jboss.as.messaging.CommonAttributes.CREATEDURABLEQUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_NON_DURABLE_QUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DELETEDURABLEQUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.DELETE_NON_DURABLE_QUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.FACTORY_CLASS;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.JMS_TOPIC;
import static org.jboss.as.messaging.CommonAttributes.JMX_DOMAIN;
import static org.jboss.as.messaging.CommonAttributes.JMX_MANAGEMENT_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_PERCENTAGE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_FILE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MAX_IO;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_TYPE;
import static org.jboss.as.messaging.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.LIVE_CONNECTOR_REF;
import static org.jboss.as.messaging.CommonAttributes.LOG_JOURNAL_WRITE_RATE;
import static org.jboss.as.messaging.CommonAttributes.LVQ;
import static org.jboss.as.messaging.CommonAttributes.MANAGE_NAME;
import static org.jboss.as.messaging.CommonAttributes.MAX_DELIVERY_ATTEMPTS;
import static org.jboss.as.messaging.CommonAttributes.MAX_SIZE_BYTES_NODE_NAME;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_HISTORY_DAY_LIMIT;
import static org.jboss.as.messaging.CommonAttributes.PAGE_SIZE_BYTES_NODE_NAME;
import static org.jboss.as.messaging.CommonAttributes.PAGING_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.PERF_BLAST_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PERSISTENCE_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_ID_CACHE;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.CommonAttributes.REDELIVERY_DELAY;
import static org.jboss.as.messaging.CommonAttributes.REDISTRIBUTION_DELAY;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.SEND_NAME;
import static org.jboss.as.messaging.CommonAttributes.SEND_TO_DLA_ON_NO_ROUTE;
import static org.jboss.as.messaging.CommonAttributes.SERVER_ID;
import static org.jboss.as.messaging.CommonAttributes.SOCKET_BINDING;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyAcceptorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressFullMessagePolicy;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.messaging.MessagingServices.TransportConfigType;
import org.jboss.as.messaging.jms.JMSService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
class MessagingSubsystemAdd extends AbstractAddStepHandler {

    private static final String DEFAULT_PATH = "messaging";
    private static final String DEFAULT_RELATIVE_TO = "jboss.server.data.dir";
    private static final ServiceName PATH_BASE = MessagingServices.JBOSS_MESSAGING.append("paths");

    static final String DEFAULT_BINDINGS_DIR = "bindings";
    static final String DEFAULT_JOURNAL_DIR = "journal";
    static final String DEFAULT_LARGE_MESSSAGE_DIR = "largemessages";
    static final String DEFAULT_PAGING_DIR = "paging";

    static final MessagingSubsystemAdd INSTANCE = new MessagingSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
        for (final String attribute : MessagingSubsystemProviders.MESSAGING_ROOT_ATTRIBUTES) {
            if (operation.hasDefined(attribute)) {
                model.get(attribute).set(operation.get(attribute));
            }
        }
        model.get(QUEUE);
        model.get(CONNECTION_FACTORY).setEmptyObject();
        model.get(JMS_QUEUE).setEmptyObject();
        model.get(JMS_TOPIC).setEmptyObject();
        model.get(POOLED_CONNECTION_FACTORY).setEmptyObject();
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final ServiceTarget serviceTarget = context.getServiceTarget();
        // Create the HornetQ Service
        final HornetQService hqService = new HornetQService();
        // Transform the configuration
        final Configuration configuration = transformConfig(operation);

        // Add the HornetQ Service
        final ServiceBuilder<HornetQServer> serviceBuilder = serviceTarget.addService(MessagingServices.JBOSS_MESSAGING, hqService)
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, hqService.getMBeanServer());
        // Create path services
        serviceBuilder.addDependency(createDirectoryService(DEFAULT_BINDINGS_DIR, operation.get(BINDINGS_DIRECTORY), serviceTarget),
                String.class, hqService.getPathInjector(DEFAULT_BINDINGS_DIR));
        serviceBuilder.addDependency(createDirectoryService(DEFAULT_JOURNAL_DIR, operation.get(JOURNAL_DIRECTORY), serviceTarget),
                String.class, hqService.getPathInjector(DEFAULT_JOURNAL_DIR));
        serviceBuilder.addDependency(createDirectoryService(DEFAULT_LARGE_MESSSAGE_DIR, operation.get(LARGE_MESSAGES_DIRECTORY), serviceTarget),
                String.class, hqService.getPathInjector(DEFAULT_LARGE_MESSSAGE_DIR));
        serviceBuilder.addDependency(createDirectoryService(DEFAULT_PAGING_DIR, operation.get(PAGING_DIRECTORY), serviceTarget),
                String.class, hqService.getPathInjector(DEFAULT_PAGING_DIR));

        // Proccess acceptors and connectors
        final Set<String> socketBindings = new HashSet<String>();
        processAcceptors(configuration, operation, socketBindings);
        processConnectors(configuration, operation, socketBindings);
        for (final String socketBinding : socketBindings) {
            final ServiceName socketName = SocketBinding.JBOSS_BINDING_NAME.append(socketBinding);
            serviceBuilder.addDependency(socketName, SocketBinding.class, hqService.getSocketBindingInjector(socketBinding));
        }
        hqService.setConfiguration(configuration);

        serviceBuilder.addListener(verificationHandler);

        // Install the HornetQ Service
        newControllers.add(serviceBuilder.install());

        newControllers.add(JMSService.addService(serviceTarget, verificationHandler));
    }

    /**
     * Transform the detyped operation parameters into the hornetQ configuration.
     *
     * @param params the detyped operation parameters
     * @return the hornetQ configuration
     */
    static Configuration transformConfig(final ModelNode params) {
        final Configuration configuration = new ConfigurationImpl();
        // --
        configuration.setBackup(params.get(BACKUP).asBoolean(ConfigurationImpl.DEFAULT_BACKUP));
        if(params.has(LIVE_CONNECTOR_REF)) {
            configuration.setLiveConnectorName(params.get(LIVE_CONNECTOR_REF).asString());
        }
        configuration.setClustered(params.get(CLUSTERED).asBoolean(ConfigurationImpl.DEFAULT_CLUSTERED));
        configuration.setClusterPassword(params.get(CLUSTER_PASSWORD).asString());
        configuration.setClusterUser(params.get(CLUSTER_USER).asString());
        configuration.setConnectionTTLOverride(params.get(CONNECTION_TTL_OVERRIDE).asInt((int) ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE));
        configuration.setCreateBindingsDir(params.get(CREATE_BINDINGS_DIR).asBoolean(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR));
        configuration.setCreateJournalDir(params.get(CREATE_JOURNAL_DIR).asBoolean(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR));
        configuration.setEnabledAsyncConnectionExecution(params.get(ASYNC_CONNECTION_EXECUTION_ENABLED).asBoolean(ConfigurationImpl.DEFAULT_ASYNC_CONNECTION_EXECUTION_ENABLED));
        configuration.setIDCacheSize(params.get(ID_CACHE_SIZE).asInt(ConfigurationImpl.DEFAULT_ID_CACHE_SIZE));
        // TODO do we want to allow the jmx configuration ?
        if (params.hasDefined(JMX_DOMAIN)) configuration.setJMXDomain(params.get(JMX_DOMAIN).asString());
        configuration.setJMXManagementEnabled(params.get(JMX_MANAGEMENT_ENABLED).asBoolean(ConfigurationImpl.DEFAULT_JMX_MANAGEMENT_ENABLED));
        // Journal
        final JournalType journalType = params.hasDefined(JOURNAL_TYPE) ? JournalType.valueOf(params.get(JOURNAL_TYPE).asString()) : ConfigurationImpl.DEFAULT_JOURNAL_TYPE;
        configuration.setJournalType(journalType);
        // AIO Journal
        configuration.setJournalBufferSize_AIO(params.get(JOURNAL_BUFFER_SIZE).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_SIZE_AIO));
        configuration.setJournalBufferTimeout_AIO(params.get(JOURNAL_BUFFER_TIMEOUT).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_TIMEOUT_AIO));
        configuration.setJournalMaxIO_AIO(params.get(JOURNAL_MAX_IO).asInt(ConfigurationImpl.DEFAULT_JOURNAL_MAX_IO_AIO));
        // NIO Journal
        configuration.setJournalBufferSize_NIO(params.get(JOURNAL_BUFFER_SIZE).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_SIZE_NIO));
        configuration.setJournalBufferTimeout_NIO(params.get(JOURNAL_BUFFER_TIMEOUT).asInt(ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_TIMEOUT_NIO));
        configuration.setJournalMaxIO_NIO(params.get(JOURNAL_MAX_IO).asInt(ConfigurationImpl.DEFAULT_JOURNAL_MAX_IO_NIO));
        //
        configuration.setJournalCompactMinFiles(params.get(JOURNAL_COMPACT_MIN_FILES).asInt(ConfigurationImpl.DEFAULT_JOURNAL_MIN_FILES));
        configuration.setJournalCompactPercentage(params.get(JOURNAL_COMPACT_PERCENTAGE).asInt(ConfigurationImpl.DEFAULT_JOURNAL_COMPACT_PERCENTAGE));
        configuration.setJournalFileSize(params.get(JOURNAL_FILE_SIZE).asInt(ConfigurationImpl.DEFAULT_JOURNAL_FILE_SIZE));
        configuration.setJournalMinFiles(params.get(JOURNAL_MIN_FILES).asInt(ConfigurationImpl.DEFAULT_JOURNAL_MIN_FILES));
        configuration.setJournalPerfBlastPages(params.get(PERF_BLAST_PAGES).asInt(ConfigurationImpl.DEFAULT_JOURNAL_PERF_BLAST_PAGES));
        configuration.setJournalSyncNonTransactional(params.get(JOURNAL_SYNC_NON_TRANSACTIONAL).asBoolean(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_NON_TRANSACTIONAL));
        configuration.setJournalSyncTransactional(params.get(JOURNAL_SYNC_TRANSACTIONAL).asBoolean(ConfigurationImpl.DEFAULT_JOURNAL_SYNC_TRANSACTIONAL));
        configuration.setLogJournalWriteRate(params.get(LOG_JOURNAL_WRITE_RATE).asBoolean(ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE));

        // configuration.setManagementAddress(address)
        // configuration.setManagementNotificationAddress(address)
        // TODO more

        configuration.setPersistDeliveryCountBeforeDelivery(params.get(PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY).asBoolean(ConfigurationImpl.DEFAULT_PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY));
        configuration.setPersistenceEnabled(params.get(PERSISTENCE_ENABLED).asBoolean(ConfigurationImpl.DEFAULT_PERSISTENCE_ENABLED));
        configuration.setPersistIDCache(params.get(PERSIST_ID_CACHE).asBoolean(ConfigurationImpl.DEFAULT_PERSIST_ID_CACHE));

        // --
        processAddressSettings(configuration, params);
        processCoreQueues(configuration, params);
        processSecuritySettings(configuration, params);
        return configuration;
    }

    /**
     * Process the acceptor information.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     * @param bindings      the referenced socket bindings
     */
    static void processAcceptors(final Configuration configuration, final ModelNode params, final Set<String> bindings) {
        if (params.hasDefined(ACCEPTOR)) {
            final Map<String, TransportConfiguration> acceptors = new HashMap<String, TransportConfiguration>();
            for (final Property property : params.get(ACCEPTOR).asPropertyList()) {
                final String acceptorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = new HashMap<String, Object>();
                if (config.get(PARAM).isDefined()) {
                    for (final Property parameter : config.get(PARAM).asPropertyList()) {
                        parameters.put(parameter.getName(), parameter.getValue().asString());
                    }
                }
                final TransportConfigType type = TransportConfigType.valueOf(config.get(TYPE).asString());
                final String clazz;
                switch (type) {
                    case Remote: {
                        clazz = NettyAcceptorFactory.class.getName();
                        final String binding = config.get(SOCKET_BINDING).asString();
                        parameters.put(SOCKET_BINDING, binding);
                        bindings.add(binding);
                        break;
                    }
                    case InVM: {
                        clazz = InVMAcceptorFactory.class.getName();
                        parameters.put(SERVER_ID, config.get(SERVER_ID).asInt());
                        break;
                    }
                    case Generic: {
                        clazz = config.get(FACTORY_CLASS).asString();
                        break;
                    }
                    default: {
                        clazz = null;
                        break;
                    }
                }
                acceptors.put(acceptorName, new TransportConfiguration(clazz, parameters, acceptorName));
            }
            configuration.setAcceptorConfigurations(new HashSet<TransportConfiguration>(acceptors.values()));
        }
    }

    /**
     * Process the connector information.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     * @param bindings      the referenced socket bindings
     */
    static void processConnectors(final Configuration configuration, final ModelNode params, final Set<String> bindings) {
        if (params.hasDefined(CONNECTOR)) {
            final Map<String, TransportConfiguration> connectors = new HashMap<String, TransportConfiguration>();
            for (final Property property : params.get(CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
                final Map<String, Object> parameters = new HashMap<String, Object>();
                if (config.get(PARAM).isDefined()) {
                    for (final Property parameter : config.get(PARAM).asPropertyList()) {
                        parameters.put(parameter.getName(), parameter.getValue().asString());
                    }
                }
                final TransportConfigType type = TransportConfigType.valueOf(config.get(TYPE).asString());
                final String clazz;
                switch (type) {
                    case Remote: {
                        clazz = NettyConnectorFactory.class.getName();
                        final String binding = config.get(SOCKET_BINDING).asString();
                        parameters.put(SOCKET_BINDING, binding);
                        bindings.add(binding);
                        break;
                    }
                    case InVM: {
                        clazz = InVMConnectorFactory.class.getName();
                        parameters.put(SERVER_ID, config.get(SERVER_ID).asInt());
                        break;
                    }
                    case Generic: {
                        clazz = config.get(FACTORY_CLASS).asString();
                        break;
                    }
                    default: {
                        clazz = null;
                        break;
                    }
                }
                connectors.put(connectorName, new TransportConfiguration(clazz, parameters, connectorName));
            }
            configuration.setConnectorConfigurations(connectors);
        }
    }

    /**
     * Process the HornetQ core queues.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     */
    static void processCoreQueues(final Configuration configuration, final ModelNode params) {
        if (params.get(QUEUE).isDefined()) {
            final List<CoreQueueConfiguration> queues = new ArrayList<CoreQueueConfiguration>();
            for (final Property property : params.get(QUEUE).asPropertyList()) {
                final String queueName = property.getName();
                final ModelNode config = property.getValue();
               boolean durable = config.get(DURABLE).isDefined()?config.get(DURABLE).asBoolean():true;
               final CoreQueueConfiguration queue = new CoreQueueConfiguration(config.get(CommonAttributes.ADDRESS).asString(), queueName,
                        config.get(FILTER).asString(), durable);
                queues.add(queue);
            }
            configuration.setQueueConfigurations(queues);
        }
    }

    /**
     * Process the address settings.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     */
    static void processAddressSettings(final Configuration configuration, final ModelNode params) {
        if (params.get(ADDRESS_SETTING).isDefined()) {
            for (final Property property : params.get(ADDRESS_SETTING).asPropertyList()) {
                final String match = property.getName();
                final ModelNode config = property.getValue();

                final AddressSettings settings = new AddressSettings();
                final AddressFullMessagePolicy addressPolicy = config.hasDefined(ADDRESS_FULL_MESSAGE_POLICY) ?
                        AddressFullMessagePolicy.valueOf(config.get(ADDRESS_FULL_MESSAGE_POLICY).asString()) : AddressSettings.DEFAULT_ADDRESS_FULL_MESSAGE_POLICY;
                settings.setAddressFullMessagePolicy(addressPolicy);
                settings.setDeadLetterAddress(asSimpleString(config.get(DEAD_LETTER_ADDRESS), null));
                settings.setLastValueQueue(config.get(LVQ).asBoolean(AddressSettings.DEFAULT_LAST_VALUE_QUEUE));
                settings.setMaxDeliveryAttempts(config.get(MAX_DELIVERY_ATTEMPTS).asInt(AddressSettings.DEFAULT_MAX_DELIVERY_ATTEMPTS));
                settings.setMaxSizeBytes(config.get(MAX_SIZE_BYTES_NODE_NAME).asInt((int) AddressSettings.DEFAULT_MAX_SIZE_BYTES));
                settings.setMessageCounterHistoryDayLimit(config.get(MESSAGE_COUNTER_HISTORY_DAY_LIMIT).asInt(AddressSettings.DEFAULT_MESSAGE_COUNTER_HISTORY_DAY_LIMIT));
                settings.setExpiryAddress(asSimpleString(config.get(EXPIRY_ADDRESS), null));
                settings.setRedeliveryDelay(config.get(REDELIVERY_DELAY).asInt((int) AddressSettings.DEFAULT_REDELIVER_DELAY));
                settings.setRedistributionDelay(config.get(REDISTRIBUTION_DELAY).asInt((int) AddressSettings.DEFAULT_REDISTRIBUTION_DELAY));
                settings.setPageSizeBytes(config.get(PAGE_SIZE_BYTES_NODE_NAME).asInt((int) AddressSettings.DEFAULT_PAGE_SIZE));
                settings.setSendToDLAOnNoRoute(config.get(SEND_TO_DLA_ON_NO_ROUTE).asBoolean(AddressSettings.DEFAULT_SEND_TO_DLA_ON_NO_ROUTE));

                configuration.getAddressesSettings().put(match, settings);
            }
        }
    }

    /**
     * Process the security settings.
     *
     * @param configuration the hornetQ configuration
     * @param params        the detyped operation parameters
     */
    static void processSecuritySettings(final Configuration configuration, final ModelNode params) {
        if (params.get(SECURITY_SETTING).isDefined()) {
            for (final Property property : params.get(SECURITY_SETTING).asPropertyList()) {
                final String match = property.getName();
                final ModelNode config = property.getValue();
                if (config.getType() != ModelType.UNDEFINED) {
                    final Set<Role> roles = new HashSet<Role>();
                    for (final Property role : config.asPropertyList()) {
                        final String name = role.getName();
                        final ModelNode value = role.getValue();
                        roles.add(new Role(name, value.get(SEND_NAME).asBoolean(false),
                                value.get(CONSUME_NAME).asBoolean(false), value.get(CREATEDURABLEQUEUE_NAME).asBoolean(false),
                                value.get(DELETEDURABLEQUEUE_NAME).asBoolean(false), value.get(CREATE_NON_DURABLE_QUEUE_NAME).asBoolean(false),
                                value.get(DELETE_NON_DURABLE_QUEUE_NAME).asBoolean(false), value.get(MANAGE_NAME).asBoolean(false)));
                    }
                    configuration.getSecurityRoles().put(match, roles);
                }
            }
        }
    }

    /**
     * Create a path service for a given target.
     *
     * @param name          the path service name
     * @param path          the detyped path element
     * @param serviceTarget the service target
     * @return the created service name
     */
    static ServiceName createDirectoryService(final String name, final ModelNode path, final ServiceTarget serviceTarget) {
        final ServiceName serviceName = PATH_BASE.append(name);
        final String relativeTo = path.hasDefined(RELATIVE_TO) ? path.get(RELATIVE_TO).asString() : DEFAULT_RELATIVE_TO;
        final String pathName = path.hasDefined(PATH) ? path.get(PATH).asString() : DEFAULT_PATH + name;
        RelativePathService.addService(serviceName, pathName, relativeTo, serviceTarget);
        return serviceName;
    }

    static SimpleString asSimpleString(final ModelNode node, final String defVal) {
        return SimpleString.toSimpleString(node.getType() != ModelType.UNDEFINED ? node.asString() : defVal);
    }

}
