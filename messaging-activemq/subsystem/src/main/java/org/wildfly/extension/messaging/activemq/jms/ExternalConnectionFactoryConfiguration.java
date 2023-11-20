/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import java.util.Arrays;
import java.util.List;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;

/**
 * Simple Configuration holder class for external connection factories.
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class ExternalConnectionFactoryConfiguration {

   private String name = null;

   private boolean persisted = false;

   private String[] bindings = null;

   private List<String> connectorNames = null;

   private String discoveryGroupName = null;

   private String clientID = null;

   private boolean ha = ActiveMQClient.DEFAULT_HA;

   private long clientFailureCheckPeriod = ActiveMQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD;

   private long connectionTTL = ActiveMQClient.DEFAULT_CONNECTION_TTL;

   private long callTimeout = ActiveMQClient.DEFAULT_CALL_TIMEOUT;

   private long callFailoverTimeout = ActiveMQClient.DEFAULT_CALL_FAILOVER_TIMEOUT;

   private boolean cacheLargeMessagesClient = ActiveMQClient.DEFAULT_CACHE_LARGE_MESSAGE_CLIENT;

   private int minLargeMessageSize = ActiveMQClient.DEFAULT_MIN_LARGE_MESSAGE_SIZE;

   private boolean compressLargeMessage = ActiveMQClient.DEFAULT_COMPRESS_LARGE_MESSAGES;

   private int compressionLevel = ActiveMQClient.DEFAULT_COMPRESSION_LEVEL;

   private int consumerWindowSize = ActiveMQClient.DEFAULT_CONSUMER_WINDOW_SIZE;

   private int consumerMaxRate = ActiveMQClient.DEFAULT_CONSUMER_MAX_RATE;

   private int confirmationWindowSize = ActiveMQClient.DEFAULT_CONFIRMATION_WINDOW_SIZE;

   private int producerWindowSize = ActiveMQClient.DEFAULT_PRODUCER_WINDOW_SIZE;

   private int producerMaxRate = ActiveMQClient.DEFAULT_PRODUCER_MAX_RATE;

   private boolean blockOnAcknowledge = ActiveMQClient.DEFAULT_BLOCK_ON_ACKNOWLEDGE;

   private boolean blockOnDurableSend = ActiveMQClient.DEFAULT_BLOCK_ON_DURABLE_SEND;

   private boolean blockOnNonDurableSend = ActiveMQClient.DEFAULT_BLOCK_ON_NON_DURABLE_SEND;

   private boolean autoGroup = ActiveMQClient.DEFAULT_AUTO_GROUP;

   private boolean preAcknowledge = ActiveMQClient.DEFAULT_PRE_ACKNOWLEDGE;

   private String loadBalancingPolicyClassName = ActiveMQClient.DEFAULT_CONNECTION_LOAD_BALANCING_POLICY_CLASS_NAME;

   private int transactionBatchSize = ActiveMQClient.DEFAULT_ACK_BATCH_SIZE;

   private int dupsOKBatchSize = ActiveMQClient.DEFAULT_ACK_BATCH_SIZE;

   private long initialWaitTimeout = ActiveMQClient.DEFAULT_DISCOVERY_INITIAL_WAIT_TIMEOUT;

   private boolean useGlobalPools = ActiveMQClient.DEFAULT_USE_GLOBAL_POOLS;

   private int scheduledThreadPoolMaxSize = ActiveMQClient.DEFAULT_SCHEDULED_THREAD_POOL_MAX_SIZE;

   private int threadPoolMaxSize = ActiveMQClient.DEFAULT_THREAD_POOL_MAX_SIZE;

   private long retryInterval = ActiveMQClient.DEFAULT_RETRY_INTERVAL;

   private double retryIntervalMultiplier = ActiveMQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER;

   private long maxRetryInterval = ActiveMQClient.DEFAULT_MAX_RETRY_INTERVAL;

   private int reconnectAttempts = ActiveMQClient.DEFAULT_RECONNECT_ATTEMPTS;

   private String groupID = null;

   private String protocolManagerFactoryStr;

   private JMSFactoryType factoryType = JMSFactoryType.CF;

   private String deserializationBlackList;

   private String deserializationWhiteList;

   private int initialMessagePacketSize = ActiveMQClient.DEFAULT_INITIAL_MESSAGE_PACKET_SIZE;

   private boolean enable1xPrefixes = ActiveMQJMSClient.DEFAULT_ENABLE_1X_PREFIXES;

   private boolean enableSharedClientID = ActiveMQClient.DEFAULT_ENABLED_SHARED_CLIENT_ID;

   private boolean useTopologyForLoadBalancing = ActiveMQClient.DEFAULT_USE_TOPOLOGY_FOR_LOADBALANCING;


   public ExternalConnectionFactoryConfiguration() {
   }

   public String[] getBindings() {
      return bindings;
   }


   public ExternalConnectionFactoryConfiguration setBindings(final String... bindings) {
      this.bindings = bindings;
      return this;
   }


   public String getName() {
      return name;
   }


   public ExternalConnectionFactoryConfiguration setName(String name) {
      this.name = name;
      return this;
   }


   public boolean isPersisted() {
      return persisted;
   }

   /**
    * @return the discoveryGroupName
    */

   public String getDiscoveryGroupName() {
      return discoveryGroupName;
   }

   /**
    * @param discoveryGroupName the discoveryGroupName to set
    */

   public ExternalConnectionFactoryConfiguration setDiscoveryGroupName(String discoveryGroupName) {
      this.discoveryGroupName = discoveryGroupName;
      return this;
   }


   public List<String> getConnectorNames() {
      return connectorNames;
   }


   public ExternalConnectionFactoryConfiguration setConnectorNames(final List<String> connectorNames) {
      this.connectorNames = connectorNames;
      return this;
   }


   public ExternalConnectionFactoryConfiguration setConnectorNames(final String... names) {
      return this.setConnectorNames(Arrays.asList(names));
   }


   public boolean isHA() {
      return ha;
   }


   public ExternalConnectionFactoryConfiguration setHA(final boolean ha) {
      this.ha = ha;
      return this;
   }


   public String getClientID() {
      return clientID;
   }


   public ExternalConnectionFactoryConfiguration setClientID(final String clientID) {
      this.clientID = clientID;
      return this;
   }


   public long getClientFailureCheckPeriod() {
      return clientFailureCheckPeriod;
   }


   public ExternalConnectionFactoryConfiguration setClientFailureCheckPeriod(final long clientFailureCheckPeriod) {
      this.clientFailureCheckPeriod = clientFailureCheckPeriod;
      return this;
   }


   public long getConnectionTTL() {
      return connectionTTL;
   }


   public ExternalConnectionFactoryConfiguration setConnectionTTL(final long connectionTTL) {
      this.connectionTTL = connectionTTL;
      return this;
   }


   public long getCallTimeout() {
      return callTimeout;
   }


   public ExternalConnectionFactoryConfiguration setCallTimeout(final long callTimeout) {
      this.callTimeout = callTimeout;
      return this;
   }


   public long getCallFailoverTimeout() {
      return callFailoverTimeout;
   }


   public ExternalConnectionFactoryConfiguration setCallFailoverTimeout(long callFailoverTimeout) {
      this.callFailoverTimeout = callFailoverTimeout;
      return this;
   }


   public boolean isCacheLargeMessagesClient() {
      return cacheLargeMessagesClient;
   }


   public ExternalConnectionFactoryConfiguration setCacheLargeMessagesClient(final boolean cacheLargeMessagesClient) {
      this.cacheLargeMessagesClient = cacheLargeMessagesClient;
      return this;
   }


   public int getMinLargeMessageSize() {
      return minLargeMessageSize;
   }


   public ExternalConnectionFactoryConfiguration setMinLargeMessageSize(final int minLargeMessageSize) {
      this.minLargeMessageSize = minLargeMessageSize;
      return this;
   }


   public int getCompressionLevel() {
      return compressionLevel;
   }


   public ExternalConnectionFactoryConfiguration setCompressionLevel(final int compressionLevel) {
      this.compressionLevel = compressionLevel;
      return this;
   }


   public int getConsumerWindowSize() {
      return consumerWindowSize;
   }


   public ExternalConnectionFactoryConfiguration setConsumerWindowSize(final int consumerWindowSize) {
      this.consumerWindowSize = consumerWindowSize;
      return this;
   }


   public int getConsumerMaxRate() {
      return consumerMaxRate;
   }


   public ExternalConnectionFactoryConfiguration setConsumerMaxRate(final int consumerMaxRate) {
      this.consumerMaxRate = consumerMaxRate;
      return this;
   }


   public int getConfirmationWindowSize() {
      return confirmationWindowSize;
   }


   public ExternalConnectionFactoryConfiguration setConfirmationWindowSize(final int confirmationWindowSize) {
      this.confirmationWindowSize = confirmationWindowSize;
      return this;
   }


   public int getProducerMaxRate() {
      return producerMaxRate;
   }


   public ExternalConnectionFactoryConfiguration setProducerMaxRate(final int producerMaxRate) {
      this.producerMaxRate = producerMaxRate;
      return this;
   }


   public int getProducerWindowSize() {
      return producerWindowSize;
   }


   public ExternalConnectionFactoryConfiguration setProducerWindowSize(final int producerWindowSize) {
      this.producerWindowSize = producerWindowSize;
      return this;
   }


   public boolean isBlockOnAcknowledge() {
      return blockOnAcknowledge;
   }


   public ExternalConnectionFactoryConfiguration setBlockOnAcknowledge(final boolean blockOnAcknowledge) {
      this.blockOnAcknowledge = blockOnAcknowledge;
      return this;
   }


   public boolean isBlockOnDurableSend() {
      return blockOnDurableSend;
   }


   public ExternalConnectionFactoryConfiguration setBlockOnDurableSend(final boolean blockOnDurableSend) {
      this.blockOnDurableSend = blockOnDurableSend;
      return this;
   }


   public boolean isBlockOnNonDurableSend() {
      return blockOnNonDurableSend;
   }


   public ExternalConnectionFactoryConfiguration setBlockOnNonDurableSend(final boolean blockOnNonDurableSend) {
      this.blockOnNonDurableSend = blockOnNonDurableSend;
      return this;
   }


   public boolean isAutoGroup() {
      return autoGroup;
   }


   public ExternalConnectionFactoryConfiguration setAutoGroup(final boolean autoGroup) {
      this.autoGroup = autoGroup;
      return this;
   }


   public boolean isPreAcknowledge() {
      return preAcknowledge;
   }


   public ExternalConnectionFactoryConfiguration setPreAcknowledge(final boolean preAcknowledge) {
      this.preAcknowledge = preAcknowledge;
      return this;
   }


   public String getLoadBalancingPolicyClassName() {
      return loadBalancingPolicyClassName;
   }


   public ExternalConnectionFactoryConfiguration setLoadBalancingPolicyClassName(final String loadBalancingPolicyClassName) {
      this.loadBalancingPolicyClassName = loadBalancingPolicyClassName;
      return this;
   }


   public int getTransactionBatchSize() {
      return transactionBatchSize;
   }


   public ExternalConnectionFactoryConfiguration setTransactionBatchSize(final int transactionBatchSize) {
      this.transactionBatchSize = transactionBatchSize;
      return this;
   }


   public int getDupsOKBatchSize() {
      return dupsOKBatchSize;
   }


   public ExternalConnectionFactoryConfiguration setDupsOKBatchSize(final int dupsOKBatchSize) {
      this.dupsOKBatchSize = dupsOKBatchSize;
      return this;
   }

   public long getInitialWaitTimeout() {
      return initialWaitTimeout;
   }

   public ExternalConnectionFactoryConfiguration setInitialWaitTimeout(final long initialWaitTimeout) {
      this.initialWaitTimeout = initialWaitTimeout;
      return this;
   }


   public boolean isUseGlobalPools() {
      return useGlobalPools;
   }


   public ExternalConnectionFactoryConfiguration setUseGlobalPools(final boolean useGlobalPools) {
      this.useGlobalPools = useGlobalPools;
      return this;
   }


   public int getScheduledThreadPoolMaxSize() {
      return scheduledThreadPoolMaxSize;
   }


   public ExternalConnectionFactoryConfiguration setScheduledThreadPoolMaxSize(final int scheduledThreadPoolMaxSize) {
      this.scheduledThreadPoolMaxSize = scheduledThreadPoolMaxSize;
      return this;
   }


   public int getThreadPoolMaxSize() {
      return threadPoolMaxSize;
   }


   public ExternalConnectionFactoryConfiguration setThreadPoolMaxSize(final int threadPoolMaxSize) {
      this.threadPoolMaxSize = threadPoolMaxSize;
      return this;
   }


   public long getRetryInterval() {
      return retryInterval;
   }


   public ExternalConnectionFactoryConfiguration setRetryInterval(final long retryInterval) {
      this.retryInterval = retryInterval;
      return this;
   }


   public double getRetryIntervalMultiplier() {
      return retryIntervalMultiplier;
   }


   public ExternalConnectionFactoryConfiguration setRetryIntervalMultiplier(final double retryIntervalMultiplier) {
      this.retryIntervalMultiplier = retryIntervalMultiplier;
      return this;
   }


   public long getMaxRetryInterval() {
      return maxRetryInterval;
   }


   public ExternalConnectionFactoryConfiguration setMaxRetryInterval(final long maxRetryInterval) {
      this.maxRetryInterval = maxRetryInterval;
      return this;
   }


   public int getReconnectAttempts() {
      return reconnectAttempts;
   }


   public ExternalConnectionFactoryConfiguration setReconnectAttempts(final int reconnectAttempts) {
      this.reconnectAttempts = reconnectAttempts;
      return this;
   }

   @Deprecated

   public boolean isFailoverOnInitialConnection() {
      return false;
   }

   @Deprecated

   public ExternalConnectionFactoryConfiguration setFailoverOnInitialConnection(final boolean failover) {
      return this;
   }


   public String getGroupID() {
      return groupID;
   }


   public ExternalConnectionFactoryConfiguration setGroupID(final String groupID) {
      this.groupID = groupID;
      return this;
   }


   public boolean isEnable1xPrefixes() {
      return enable1xPrefixes;
   }


   public ExternalConnectionFactoryConfiguration setEnable1xPrefixes(final boolean enable1xPrefixes) {
      this.enable1xPrefixes = enable1xPrefixes;
      return this;
   }


   public ExternalConnectionFactoryConfiguration setFactoryType(final JMSFactoryType factoryType) {
      this.factoryType = factoryType;
      return this;
   }


   public JMSFactoryType getFactoryType() {
      return factoryType;
   }


   public String getDeserializationBlackList() {
      return deserializationBlackList;
   }


   public void setDeserializationBlackList(String blackList) {
      this.deserializationBlackList = blackList;
   }


   public String getDeserializationWhiteList() {
      return this.deserializationWhiteList;
   }


   public void setDeserializationWhiteList(String whiteList) {
      this.deserializationWhiteList = whiteList;
   }


   public ExternalConnectionFactoryConfiguration setCompressLargeMessages(boolean compressLargeMessage) {
      this.compressLargeMessage = compressLargeMessage;
      return this;
   }


   public boolean isCompressLargeMessages() {
      return this.compressLargeMessage;
   }


   public ExternalConnectionFactoryConfiguration setProtocolManagerFactoryStr(String protocolManagerFactoryStr) {
      this.protocolManagerFactoryStr = protocolManagerFactoryStr;
      return this;
   }


   public String getProtocolManagerFactoryStr() {
      return protocolManagerFactoryStr;
   }


   public int getInitialMessagePacketSize() {
      return initialMessagePacketSize;
   }


   public ExternalConnectionFactoryConfiguration setInitialMessagePacketSize(int size) {
      this.initialMessagePacketSize = size;
      return this;
   }


   public ExternalConnectionFactoryConfiguration setEnableSharedClientID(boolean enabled) {
      this.enableSharedClientID = enabled;
      return this;
   }


   public boolean isEnableSharedClientID() {
      return enableSharedClientID;
   }


   public ExternalConnectionFactoryConfiguration setUseTopologyForLoadBalancing(boolean useTopologyForLoadBalancing) {
      this.useTopologyForLoadBalancing = useTopologyForLoadBalancing;
      return this;
   }


   public boolean getUseTopologyForLoadBalancing() {
      return useTopologyForLoadBalancing;
   }

}
