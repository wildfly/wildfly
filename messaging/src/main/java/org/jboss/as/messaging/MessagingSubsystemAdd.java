/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.ALLOW_FAILBACK;
import static org.jboss.as.messaging.CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BINDINGS_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_USER;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.jboss.as.messaging.CommonAttributes.FAILBACK_DELAY;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SHUTDOWN;
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
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MEMORY_MEASURE_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.MEMORY_WARNING_THRESHOLD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.jboss.as.messaging.CommonAttributes.NAME_OPTIONAL;
import static org.jboss.as.messaging.CommonAttributes.PAGING_DIRECTORY;
import static org.jboss.as.messaging.CommonAttributes.PERF_BLAST_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PERSISTENCE_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_ID_CACHE;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.CommonAttributes.RUN_SYNC_SPEED_TEST;
import static org.jboss.as.messaging.CommonAttributes.SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_INVALIDATION_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.SERVER_DUMP_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SHARED_STORE;
import static org.jboss.as.messaging.CommonAttributes.THREAD_POOL_MAX_SIZE;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.WILD_CARD_ROUTING_ENABLED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;

import org.hornetq.api.core.DiscoveryGroupConfiguration;
import org.hornetq.api.core.SimpleString;
import org.hornetq.core.config.BroadcastGroupConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.settings.impl.AddressSettings;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
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
 * Add handler for the messaging subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class MessagingSubsystemAdd extends AbstractAddStepHandler implements DescriptionProvider {

    public static final MessagingSubsystemAdd INSTANCE = new MessagingSubsystemAdd();

    private MessagingSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        // no-op
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    @Override
    public ModelNode getModelDescription(final Locale locale) {
        return MessagingDescriptions.getSubsystemAdd(locale);
    }

}
