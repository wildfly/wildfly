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

package org.jboss.as.messaging.jms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.hornetq.api.core.Pair;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.hornetq.jms.server.config.impl.ConnectionFactoryConfigurationImpl;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * ConnectionFactory configuration element.
 *
 * @author Emanuel Muckenhuber
 */
public class ConnectionFactoryElement extends AbstractModelElement<ConnectionFactoryElement> {

    private static final long serialVersionUID = 7928155650456224886L;
    private final String name;
    private Set<String> bindings;
    private String discoveryGroupName;

    private String localBindAddress;
    private String discoveryAddress;
    private Integer discoveryPort;
    private String clientID = null;
    private Long discoveryRefreshTimeout;
    private Long clientFailureCheckPeriod;
    private Long connectionTTL;
    private Long callTimeout;
    private Boolean cacheLargeMessagesClient;
    private Integer minLargeMessageSize;
    private Integer consumerWindowSize;
    private Integer consumerMaxRate;
    private Integer confirmationWindowSize;
    private Integer producerWindowSize;
    private Integer producerMaxRate;
    private Boolean blockOnAcknowledge;
    private Boolean blockOnDurableSend;
    private Boolean blockOnNonDurableSend;
    private Boolean autoGroup;
    private Boolean preAcknowledge;
    private String loadBalancingPolicyClassName;
    private Integer transactionBatchSize;
    private Integer dupsOKBatchSize;
    private Long initialWaitTimeout;
    private Boolean useGlobalPools;
    private Integer scheduledThreadPoolMaxSize;
    private Integer threadPoolMaxSize;
    private Long retryInterval;
    private Double retryIntervalMultiplier;
    private Long maxRetryInterval;
    private Integer reconnectAttempts;
    private Boolean failoverOnInitialConnection;
    private Boolean failoverOnServerShutdown;
    private String groupID = null;
    private List<ConnectionFactoryConnectorRef> connectorRef;

    public ConnectionFactoryElement(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Set<String> getBindings() {
        return bindings;
    }

    public void setBindings(Set<String> bindings) {
        this.bindings = bindings;
    }

    public String getDiscoveryGroupName() {
        return discoveryGroupName;
    }

    public void setDiscoveryGroupName(String discoveryGroupName) {
        this.discoveryGroupName = discoveryGroupName;
    }

    public String getLocalBindAddress() {
        return localBindAddress;
    }

    public void setLocalBindAddress(String localBindAddress) {
        this.localBindAddress = localBindAddress;
    }

    public String getDiscoveryAddress() {
        return discoveryAddress;
    }

    public void setDiscoveryAddress(String discoveryAddress) {
        this.discoveryAddress = discoveryAddress;
    }

    public Integer getDiscoveryPort() {
        return discoveryPort;
    }

    public void setDiscoveryPort(Integer discoveryPort) {
        this.discoveryPort = discoveryPort;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public Long getDiscoveryRefreshTimeout() {
        return discoveryRefreshTimeout;
    }

    public void setDiscoveryRefreshTimeout(Long discoveryRefreshTimeout) {
        this.discoveryRefreshTimeout = discoveryRefreshTimeout;
    }

    public Long getClientFailureCheckPeriod() {
        return clientFailureCheckPeriod;
    }

    public void setClientFailureCheckPeriod(Long clientFailureCheckPeriod) {
        this.clientFailureCheckPeriod = clientFailureCheckPeriod;
    }

    public Long getConnectionTTL() {
        return connectionTTL;
    }

    public void setConnectionTTL(Long connectionTTL) {
        this.connectionTTL = connectionTTL;
    }

    public Long getCallTimeout() {
        return callTimeout;
    }

    public void setCallTimeout(Long callTimeout) {
        this.callTimeout = callTimeout;
    }

    public Boolean getCacheLargeMessagesClient() {
        return cacheLargeMessagesClient;
    }

    public void setCacheLargeMessagesClient(Boolean cacheLargeMessagesClient) {
        this.cacheLargeMessagesClient = cacheLargeMessagesClient;
    }

    public Integer getMinLargeMessageSize() {
        return minLargeMessageSize;
    }

    public void setMinLargeMessageSize(Integer minLargeMessageSize) {
        this.minLargeMessageSize = minLargeMessageSize;
    }

    public Integer getConsumerWindowSize() {
        return consumerWindowSize;
    }

    public void setConsumerWindowSize(Integer consumerWindowSize) {
        this.consumerWindowSize = consumerWindowSize;
    }

    public Integer getConsumerMaxRate() {
        return consumerMaxRate;
    }

    public void setConsumerMaxRate(Integer consumerMaxRate) {
        this.consumerMaxRate = consumerMaxRate;
    }

    public Integer getConfirmationWindowSize() {
        return confirmationWindowSize;
    }

    public void setConfirmationWindowSize(Integer confirmationWindowSize) {
        this.confirmationWindowSize = confirmationWindowSize;
    }

    public Integer getProducerWindowSize() {
        return producerWindowSize;
    }

    public void setProducerWindowSize(Integer producerWindowSize) {
        this.producerWindowSize = producerWindowSize;
    }

    public Integer getProducerMaxRate() {
        return producerMaxRate;
    }

    public void setProducerMaxRate(Integer producerMaxRate) {
        this.producerMaxRate = producerMaxRate;
    }

    public Boolean getBlockOnAcknowledge() {
        return blockOnAcknowledge;
    }

    public void setBlockOnAcknowledge(Boolean blockOnAcknowledge) {
        this.blockOnAcknowledge = blockOnAcknowledge;
    }

    public Boolean getBlockOnDurableSend() {
        return blockOnDurableSend;
    }

    public void setBlockOnDurableSend(Boolean blockOnDurableSend) {
        this.blockOnDurableSend = blockOnDurableSend;
    }

    public Boolean getBlockOnNonDurableSend() {
        return blockOnNonDurableSend;
    }

    public void setBlockOnNonDurableSend(Boolean blockOnNonDurableSend) {
        this.blockOnNonDurableSend = blockOnNonDurableSend;
    }

    public Boolean getAutoGroup() {
        return autoGroup;
    }

    public void setAutoGroup(Boolean autoGroup) {
        this.autoGroup = autoGroup;
    }

    public Boolean getPreAcknowledge() {
        return preAcknowledge;
    }

    public void setPreAcknowledge(Boolean preAcknowledge) {
        this.preAcknowledge = preAcknowledge;
    }

    public String getLoadBalancingPolicyClassName() {
        return loadBalancingPolicyClassName;
    }

    public void setLoadBalancingPolicyClassName(String loadBalancingPolicyClassName) {
        this.loadBalancingPolicyClassName = loadBalancingPolicyClassName;
    }

    public Integer getTransactionBatchSize() {
        return transactionBatchSize;
    }

    public void setTransactionBatchSize(Integer transactionBatchSize) {
        this.transactionBatchSize = transactionBatchSize;
    }

    public Integer getDupsOKBatchSize() {
        return dupsOKBatchSize;
    }

    public void setDupsOKBatchSize(Integer dupsOKBatchSize) {
        this.dupsOKBatchSize = dupsOKBatchSize;
    }

    public Long getInitialWaitTimeout() {
        return initialWaitTimeout;
    }

    public void setInitialWaitTimeout(Long initialWaitTimeout) {
        this.initialWaitTimeout = initialWaitTimeout;
    }

    public Boolean getUseGlobalPools() {
        return useGlobalPools;
    }

    public void setUseGlobalPools(Boolean useGlobalPools) {
        this.useGlobalPools = useGlobalPools;
    }

    public Integer getScheduledThreadPoolMaxSize() {
        return scheduledThreadPoolMaxSize;
    }

    public void setScheduledThreadPoolMaxSize(Integer scheduledThreadPoolMaxSize) {
        this.scheduledThreadPoolMaxSize = scheduledThreadPoolMaxSize;
    }

    public Integer getThreadPoolMaxSize() {
        return threadPoolMaxSize;
    }

    public void setThreadPoolMaxSize(Integer threadPoolMaxSize) {
        this.threadPoolMaxSize = threadPoolMaxSize;
    }

    public Long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Double getRetryIntervalMultiplier() {
        return retryIntervalMultiplier;
    }

    public void setRetryIntervalMultiplier(Double retryIntervalMultiplier) {
        this.retryIntervalMultiplier = retryIntervalMultiplier;
    }

    public Long getMaxRetryInterval() {
        return maxRetryInterval;
    }

    public void setMaxRetryInterval(Long maxRetryInterval) {
        this.maxRetryInterval = maxRetryInterval;
    }

    public Integer getReconnectAttempts() {
        return reconnectAttempts;
    }

    public void setReconnectAttempts(Integer reconnectAttempts) {
        this.reconnectAttempts = reconnectAttempts;
    }

    public Boolean getFailoverOnInitialConnection() {
        return failoverOnInitialConnection;
    }

    public void setFailoverOnInitialConnection(Boolean failoverOnInitialConnection) {
        this.failoverOnInitialConnection = failoverOnInitialConnection;
    }

    public Boolean getFailoverOnServerShutdown() {
        return failoverOnServerShutdown;
    }

    public void setFailoverOnServerShutdown(Boolean failoverOnServerShutdown) {
        this.failoverOnServerShutdown = failoverOnServerShutdown;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public List<ConnectionFactoryConnectorRef> getConnectorRef() {
        return connectorRef;
    }

    public void setConnectorRef(List<ConnectionFactoryConnectorRef> connectorRef) {
        this.connectorRef = connectorRef;
    }

    /** {@inheritDoc} */
    protected Class<ConnectionFactoryElement> getElementClass() {
        return ConnectionFactoryElement.class;
    }

    /** {@inheritDoc} */
    public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.NAME.getLocalName(), name);

        if(discoveryGroupName != null) {
            streamWriter.writeEmptyElement(Element.DISCOVERY_GROUP_REF.getLocalName());
            streamWriter.writeAttribute(Attribute.DISCOVERY_GROUP_NAME.getLocalName(), discoveryGroupName);
        }
        if(initialWaitTimeout != null) {
            writeSimpleElement(Element.DISCOVERY_INITIAL_WAIT_TIMEOUT, initialWaitTimeout.toString(), streamWriter);
        }
        if(connectorRef != null && ! connectorRef.isEmpty()) {
            streamWriter.writeStartElement(Element.CONNECTORS.getLocalName());
            for(final ConnectionFactoryConnectorRef ref : connectorRef) {
                streamWriter.writeEmptyElement(Element.CONNECTOR_REF.getLocalName());
                streamWriter.writeAttribute(Attribute.CONNECTOR_NAME.getLocalName(), ref.getConnectorName());
                if(ref.getBackupName() != null) {
                    streamWriter.writeAttribute(Attribute.CONNECTOR_BACKUP_NAME.getLocalName(), ref.getBackupName());
                }
            }
            streamWriter.writeEndElement();
        }
        if(bindings != null && bindings.size() > 0) {
            streamWriter.writeStartElement(Element.ENTRIES.getLocalName());
            for(final String binding : bindings) {
                streamWriter.writeEmptyElement(Element.ENTRY.getLocalName());
                streamWriter.writeAttribute(Attribute.NAME.getLocalName(), binding);
            }
            streamWriter.writeEndElement();
        }
        if(clientFailureCheckPeriod != null) {
            writeSimpleElement(Element.CLIENT_FAILURE_CHECK_PERIOD, clientFailureCheckPeriod.toString(), streamWriter);
        }
        if(connectionTTL != null) {
            writeSimpleElement(Element.CONNECTION_TTL, connectionTTL.toString(), streamWriter);
        }
        if(callTimeout != null) {
            writeSimpleElement(Element.CALL_TIMEOUT, callTimeout.toString(), streamWriter);
        }
        if(consumerWindowSize != null) {
            writeSimpleElement(Element.CONSUMER_WINDOW_SIZE, consumerWindowSize.toString(), streamWriter);
        }
        if(consumerMaxRate != null) {
            writeSimpleElement(Element.CONSUMER_MAX_RATE, consumerMaxRate.toString(), streamWriter);
        }
        if(confirmationWindowSize != null) {
            writeSimpleElement(Element.CONFIRMATION_WINDOW_SIZE, confirmationWindowSize.toString(), streamWriter);
        }
        if(producerWindowSize != null) {
            writeSimpleElement(Element.PRODUCER_WINDOW_SIZE, producerWindowSize.toString(), streamWriter);
        }
        if(producerMaxRate != null) {
            writeSimpleElement(Element.PRODUCER_MAX_RATE, producerMaxRate.toString(), streamWriter);
        }
        if(cacheLargeMessagesClient != null) {
            writeSimpleElement(Element.CACHE_LARGE_MESSAGE_CLIENT, cacheLargeMessagesClient.toString(), streamWriter);
        }
        if(minLargeMessageSize != null) {
            writeSimpleElement(Element.MIN_LARGE_MESSAGE_SIZE, minLargeMessageSize.toString(), streamWriter);
        }
        if(clientID != null) {
            writeSimpleElement(Element.CLIENT_ID, clientID, streamWriter);
        }
        if(dupsOKBatchSize != null) {
            writeSimpleElement(Element.DUPS_OK_BATCH_SIZE, dupsOKBatchSize.toString(), streamWriter);
        }
        if(transactionBatchSize != null) {
            writeSimpleElement(Element.TRANSACTION_BATH_SIZE, transactionBatchSize.toString(), streamWriter);
        }
        if(blockOnAcknowledge != null) {
            writeSimpleElement(Element.BLOCK_ON_ACK, blockOnAcknowledge.toString(), streamWriter);
        }
        if(blockOnNonDurableSend != null) {
            writeSimpleElement(Element.BLOCK_ON_NON_DURABLE_SEND, blockOnNonDurableSend.toString(), streamWriter);
        }
        if(blockOnDurableSend != null) {
            writeSimpleElement(Element.BLOCK_ON_DURABLE_SEND, blockOnDurableSend.toString(), streamWriter);
        }
        if(autoGroup != null) {
            writeSimpleElement(Element.AUTO_GROUP, autoGroup.toString(), streamWriter);
        }
        if(preAcknowledge != null) {
            writeSimpleElement(Element.PRE_ACK, preAcknowledge.toString(), streamWriter);
        }
        if(retryInterval != null) {
            writeSimpleElement(Element.RETRY_INTERVAL, retryInterval.toString(), streamWriter);
        }
        if(retryIntervalMultiplier != null) {
            writeSimpleElement(Element.RETRY_INTERVAL_MULTIPLIER, retryIntervalMultiplier.toString(), streamWriter);
        }
        if(maxRetryInterval != null) {
            writeSimpleElement(Element.MAX_RETRY_INTERVAL, maxRetryInterval.toString(), streamWriter);
        }
        if(reconnectAttempts != null) {
            writeSimpleElement(Element.RECONNECT_ATTEMPTS, reconnectAttempts.toString(), streamWriter);
        }
        if(failoverOnInitialConnection != null) {
            writeSimpleElement(Element.FAILOVER_ON_INITIAL_CONNECTION, failoverOnInitialConnection.toString(), streamWriter);
        }
        if(failoverOnServerShutdown != null) {
            writeSimpleElement(Element.FAILOVER_ON_SERVER_SHUTDOWN, failoverOnServerShutdown.toString(), streamWriter);
        }
        if(loadBalancingPolicyClassName != null) {
            writeSimpleElement(Element.LOAD_BALANCING_CLASS_NAME, loadBalancingPolicyClassName, streamWriter);
        }
        if(useGlobalPools != null) {
            writeSimpleElement(Element.USE_GLOBAL_POOLS, useGlobalPools.toString(), streamWriter);
        }
        if(scheduledThreadPoolMaxSize != null) {
            writeSimpleElement(Element.SCHEDULED_THREAD_POOL_MAX_SIZE, scheduledThreadPoolMaxSize.toString(), streamWriter);
        }
        if(threadPoolMaxSize != null) {
            writeSimpleElement(Element.THREAD_POOL_MAX_SIZE, threadPoolMaxSize.toString(), streamWriter);
        }
        if(groupID != null) {
            writeSimpleElement(Element.GROUP_ID, groupID, streamWriter);
        }
        streamWriter.writeEndElement();
    }

    /**
     * Transform the {@code ConnectionFactoryElement} to a HornetQ {@code ConnectionFactoryConfiguration}.
     *
     * @return the transformed metadata.
     */
    ConnectionFactoryConfiguration transform() {
        final ConnectionFactoryConfiguration configuration = new ConnectionFactoryConfigurationImpl(name, jndiBindings());

        if (discoveryGroupName != null) {
            if(initialWaitTimeout != null) configuration.setInitialWaitTimeout(initialWaitTimeout);
            if(discoveryGroupName != null) configuration.setDiscoveryGroupName(discoveryGroupName);
        } else {
            if(this.connectorRef != null && ! this.connectorRef.isEmpty()) {
                List<Pair<String, String>> connectorNames = new ArrayList<Pair<String,String>>();
                for(final ConnectionFactoryConnectorRef ref : this.connectorRef) {
                    connectorNames.add( new Pair<String, String>(ref.getConnectorName(), ref.getBackupName()));
                }
                configuration.setConnectorNames(connectorNames);
            }
        }
        if(clientID != null) configuration.setClientID(clientID);
        if(clientFailureCheckPeriod != null) configuration.setClientFailureCheckPeriod(clientFailureCheckPeriod);
        if(connectionTTL != null) configuration.setConnectionTTL(connectionTTL);
        if(callTimeout != null) configuration.setCallTimeout(callTimeout);
        if(cacheLargeMessagesClient != null) configuration.setCacheLargeMessagesClient(cacheLargeMessagesClient);
        if(minLargeMessageSize != null) configuration.setMinLargeMessageSize(minLargeMessageSize);
        if(consumerWindowSize != null) configuration.setConsumerWindowSize(consumerWindowSize);
        if(consumerMaxRate != null) configuration.setConsumerMaxRate(consumerMaxRate);
        if(confirmationWindowSize != null) configuration.setConfirmationWindowSize(confirmationWindowSize);
        if(producerWindowSize != null) configuration.setProducerWindowSize(producerWindowSize);
        if(producerMaxRate != null) configuration.setProducerMaxRate(producerMaxRate);
        if(blockOnAcknowledge != null) configuration.setBlockOnAcknowledge(blockOnAcknowledge);
        if(blockOnDurableSend != null) configuration.setBlockOnDurableSend(blockOnDurableSend);
        if(blockOnNonDurableSend != null) configuration.setBlockOnNonDurableSend(blockOnNonDurableSend);
        if(autoGroup != null) configuration.setAutoGroup(autoGroup);
        if(preAcknowledge != null) configuration.setPreAcknowledge(preAcknowledge);
        if(loadBalancingPolicyClassName != null) configuration.setLoadBalancingPolicyClassName(loadBalancingPolicyClassName);
        if(transactionBatchSize != null) configuration.setTransactionBatchSize(transactionBatchSize);
        if(dupsOKBatchSize != null) configuration.setDupsOKBatchSize(dupsOKBatchSize);
        if(useGlobalPools != null) configuration.setUseGlobalPools(useGlobalPools);
        if(scheduledThreadPoolMaxSize != null) configuration.setScheduledThreadPoolMaxSize(scheduledThreadPoolMaxSize);
        if(threadPoolMaxSize != null) configuration.setThreadPoolMaxSize(threadPoolMaxSize);
        if(retryInterval != null) configuration.setRetryInterval(retryInterval);
        if(retryIntervalMultiplier != null) configuration.setRetryIntervalMultiplier(retryIntervalMultiplier);
        if(maxRetryInterval != null) configuration.setMaxRetryInterval(maxRetryInterval);
        if(reconnectAttempts != null) configuration.setReconnectAttempts(reconnectAttempts);
        if(failoverOnServerShutdown != null) configuration.setFailoverOnServerShutdown(failoverOnServerShutdown);
        if(failoverOnInitialConnection != null) configuration.setFailoverOnInitialConnection(failoverOnInitialConnection);
        if(groupID != null) configuration.setGroupID(groupID);
        return configuration;
    }

    private String[] jndiBindings() {
        if(bindings != null && ! bindings.isEmpty()) {
            return bindings.toArray(new String[bindings.size()]);
        }
        return new String[0];
    }

    static void writeSimpleElement(final Element element, String content, XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        if(content != null) {
            streamWriter.writeStartElement(element.getLocalName());
            streamWriter.writeCharacters(content);
            streamWriter.writeEndElement();
        }
    }

}
