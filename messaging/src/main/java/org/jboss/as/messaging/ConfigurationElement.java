package org.jboss.as.messaging;

import org.hornetq.api.core.Pair;
import org.hornetq.api.core.SimpleString;
import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.core.config.BridgeConfiguration;
import org.hornetq.core.config.BroadcastGroupConfiguration;
import org.hornetq.core.config.ClusterConnectionConfiguration;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.ConnectorServiceConfiguration;
import org.hornetq.core.config.CoreQueueConfiguration;
import org.hornetq.core.config.DiscoveryGroupConfiguration;
import org.hornetq.core.config.DivertConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.config.impl.FileConfiguration;
import org.hornetq.core.config.impl.Validators;
import org.hornetq.core.journal.impl.AIOSequentialFileFactory;
import org.hornetq.core.security.Role;
import org.hornetq.core.server.JournalType;
import org.hornetq.core.server.group.impl.GroupingHandlerConfiguration;
import org.hornetq.core.settings.impl.AddressSettings;
import org.hornetq.utils.XMLConfigurationUtil;
import org.jboss.as.model.AbstractModelElement;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author scott.stark@jboss.org
 * @version $Id$
 */
public class ConfigurationElement extends AbstractModelElement<ConfigurationElement> implements ServiceActivator {
   private static final long serialVersionUID = 1L;
   private static final Logger log = Logger.getLogger("org.jboss.as.messaging");

   Configuration config = new ConfigurationImpl();


   public ConfigurationElement(final XMLExtendedStreamReader reader) throws XMLStreamException {
      System.out.println("Begin " + reader.getLocation() + reader.getLocalName());
      // Handle elements
      int tag = reader.getEventType();
      String localName = null;
      do {
         tag = reader.nextTag();
         localName = reader.getLocalName();
         final org.jboss.as.messaging.Element element = org.jboss.as.messaging.Element.forName(reader.getLocalName());
         System.out.println(localName + " -> " + element + ", event=" + StaxEvent.tagToEvent(tag));
         switch (element) {
         case ACCEPTORS:
            AcceptorsElement acceptors = new AcceptorsElement(reader, config);
            break;
         case ADDRESS_SETTINGS:
            AddressSettingsElement ase = new AddressSettingsElement(reader, config);
            break;
         case ASYNC_CONNECTION_EXECUTION_ENABLED:
            break;
         case BACKUP:
            break;
         case BACKUP_CONNECTOR_REF:
            break;
         case BINDINGS_DIRECTORY: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setBindingsDirectory(text);
            }
         }
         break;
         case BROADCAST_PERIOD:
            break;
         case CLUSTERED: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setClustered(Boolean.getBoolean(text));
            }
         }
         break;
         case CLUSTER_PASSWORD:
            break;
         case CLUSTER_USER:
            break;
         case CONNECTION_TTL_OVERRIDE:
            break;
         case CONNECTORS:
            ConnectorsElement connectors = new ConnectorsElement(reader, config);
            break;
         case CONNECTOR_REF:
            break;
         case CREATE_BINDINGS_DIR:
            break;
         case CREATE_JOURNAL_DIR:
            break;
         case FILE_DEPLOYMENT_ENABLED:
            break;
         case GROUP_ADDRESS:
            break;
         case GROUP_PORT:
            break;
         case GROUPING_HANDLER:
            break;
         case ID_CACHE_SIZE:
            break;
         case JMX_DOMAIN:
            break;
         case JMX_MANAGEMENT_ENABLED:
            break;
         case JOURNAL_BUFFER_SIZE:
            break;
         case JOURNAL_BUFFER_TIMEOUT:
            break;
         case JOURNAL_COMPACT_MIN_FILES:
            break;
         case JOURNAL_COMPACT_PERCENTAGE:
            break;
         case JOURNAL_DIRECTORY: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setJournalDirectory(text);
            }
         }
         break;
         case JOURNAL_MIN_FILES: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setJournalMinFiles(Integer.valueOf(text));
            }
         }
         break;
         case JOURNAL_SYNC_NON_TRANSACTIONAL:
            break;
         case JOURNAL_SYNC_TRANSACTIONAL:
            break;
         case JOURNAL_TYPE:
            break;
         case JOURNAL_FILE_SIZE:
            break;
         case JOURNAL_MAX_IO:
            break;
         case LARGE_MESSAGES_DIRECTORY: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setLargeMessagesDirectory(text);
            }
         }
         break;
         case LOCAL_BIND_ADDRESS:
            break;
         case LOCAL_BIND_PORT:
            break;
         case LOG_JOURNAL_WRITE_RATE:
            break;
         case MANAGEMENT_ADDRESS:
            break;
         case MANAGEMENT_NOTIFICATION_ADDRESS:
            break;
         case MEMORY_MEASURE_INTERVAL:
            break;
         case MEMORY_WARNING_THRESHOLD:
            break;
         case MESSAGE_COUNTER_ENABLED:
            break;
         case MESSAGE_COUNTER_MAX_DAY_HISTORY:
            break;
         case MESSAGE_COUNTER_SAMPLE_PERIOD:
            break;
         case MESSAGE_EXPIRY_SCAN_PERIOD:
            break;
         case MESSAGE_EXPIRY_THREAD_PRIORITY:
            break;
         case PAGING_DIRECTORY: {
            String text = reader.getElementText();
            if (text != null && text.length() > 0) {
               config.setPagingDirectory(text);
            }
         }
         break;
         case PERF_BLAST_PAGES:
            break;
         case PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY:
            break;
         case PERSIST_ID_CACHE:
            break;
         case PERSISTENCE_ENABLED:
            break;
         case REFRESH_TIMEOUT:
            break;
         case REMOTING_INTERCEPTORS:
            break;
         case RUN_SYNC_SPEED_TEST:
            break;
         case SECURITY_ENABLED:
            break;
         case SECURITY_INVALIDATION_INTERVAL:
            break;
         case SECURITY_SETTINGS:
            SecuritySettingsElement sse = new SecuritySettingsElement(reader, config);
            break;
         case SERVER_DUMP_INTERVAL:
            break;
         case SHARED_STORE:
            break;
         case TRANSACTION_TIMEOUT:
            break;
         case TRANSACTION_TIMEOUT_SCAN_PERIOD:
            break;
         case WILD_CARD_ROUTING_ENABLED:
            break;
         case DEAD_LETTER_ADDRESS_NODE_NAME:
            break;
         case EXPIRY_ADDRESS_NODE_NAME:
            break;
         case REDELIVERY_DELAY_NODE_NAME:
            break;
         case MAX_DELIVERY_ATTEMPTS:
            break;
         case MAX_SIZE_BYTES_NODE_NAME:
            break;
         case ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME:
            break;
         case PAGE_SIZE_BYTES_NODE_NAME:
            break;
         case MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME:
            break;
         case LVQ_NODE_NAME:
            break;
         case REDISTRIBUTION_DELAY_NODE_NAME:
            break;
         case SEND_TO_DLA_ON_NO_ROUTE:
            break;
         case SUBSYSTEM:

            break;
         default:
            throw unexpectedElement(reader);
         }
      } while (reader.hasNext() && localName.equals("subsystem") == false);

      // Set the log delegate
      //config.setLogDelegateFactoryClassName();
      System.out.println("End messaging:subsystem, " + reader.getLocalName() + ", hasNext: " + reader.hasNext());
   }

   public Configuration getConfiguration() {
      return config;
   }

   @Override
   public long elementHash() {
      return 0;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   protected Class<ConfigurationElement> getElementClass() {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void writeContent(XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   @Override
   public void activate(ServiceActivatorContext serviceActivatorContext) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   /**
    *
    */
   private class XmlElementReader implements XMLStreamReader {
      XMLExtendedStreamReader reader;
      boolean sawEnd = false;

      XmlElementReader(XMLExtendedStreamReader reader) {
         this.reader = reader;
      }

      @Override
      public Object getProperty(String name) throws IllegalArgumentException {
         return reader.getProperty(name);
      }

      @Override
      public int next() throws XMLStreamException {
         int next = -1;
         if (sawEnd == false) {
            next = reader.next();
            if (next == XMLStreamConstants.START_ELEMENT) {
               System.out.println("start, " + reader.getLocalName());
            }
            if (next == XMLStreamConstants.END_ELEMENT) {
               System.out.println("end, " + reader.getLocalName());
               if (reader.getLocalName().equals("subsystem")) {
                  sawEnd = true;
               }
               return next;
            }
         }
         return next;
      }

      @Override
      public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
         reader.require(type, namespaceURI, localName);
      }

      @Override
      public String getElementText() throws XMLStreamException {
         return reader.getElementText();
      }

      @Override
      public int nextTag() throws XMLStreamException {
         return reader.nextTag();
      }

      @Override
      public boolean hasNext() throws XMLStreamException {
         return reader.hasNext();
      }

      @Override
      public void close() throws XMLStreamException {
         reader.close();
      }

      @Override
      public String getNamespaceURI(String prefix) {
         return reader.getNamespaceURI(prefix);
      }

      @Override
      public boolean isStartElement() {
         return reader.isStartElement();
      }

      @Override
      public boolean isEndElement() {
         return reader.isEndElement();
      }

      @Override
      public boolean isCharacters() {
         return reader.isCharacters();
      }

      @Override
      public boolean isWhiteSpace() {
         return reader.isWhiteSpace();
      }

      @Override
      public String getAttributeValue(String namespaceURI, String localName) {
         return reader.getAttributeValue(namespaceURI, localName);
      }

      @Override
      public int getAttributeCount() {
         return reader.getAttributeCount();
      }

      @Override
      public QName getAttributeName(int index) {
         return reader.getAttributeName(index);
      }

      @Override
      public String getAttributeNamespace(int index) {
         return reader.getAttributeNamespace(index);
      }

      @Override
      public String getAttributeLocalName(int index) {
         return reader.getAttributeLocalName(index);
      }

      @Override
      public String getAttributePrefix(int index) {
         return reader.getAttributePrefix(index);
      }

      @Override
      public String getAttributeType(int index) {
         return reader.getAttributeType(index);
      }

      @Override
      public String getAttributeValue(int index) {
         return reader.getAttributeValue(index);
      }

      @Override
      public boolean isAttributeSpecified(int index) {
         return reader.isAttributeSpecified(index);
      }

      @Override
      public int getNamespaceCount() {
         return reader.getNamespaceCount();
      }

      @Override
      public String getNamespacePrefix(int index) {
         return reader.getNamespacePrefix(index);
      }

      @Override
      public String getNamespaceURI(int index) {
         return reader.getNamespaceURI(index);
      }

      @Override
      public NamespaceContext getNamespaceContext() {
         return reader.getNamespaceContext();
      }

      @Override
      public int getEventType() {
         return reader.getEventType();
      }

      @Override
      public String getText() {
         return reader.getText();
      }

      @Override
      public char[] getTextCharacters() {
         return reader.getTextCharacters();
      }

      @Override
      public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
         return reader.getTextCharacters(sourceStart, target, targetStart, length);
      }

      @Override
      public int getTextStart() {
         return reader.getTextStart();
      }

      @Override
      public int getTextLength() {
         return reader.getTextLength();
      }

      @Override
      public String getEncoding() {
         return reader.getEncoding();
      }

      @Override
      public boolean hasText() {
         return reader.hasText();
      }

      @Override
      public javax.xml.stream.Location getLocation() {
         return reader.getLocation();
      }

      @Override
      public QName getName() {
         return reader.getName();
      }

      @Override
      public String getLocalName() {
         return reader.getLocalName();
      }

      @Override
      public boolean hasName() {
         return reader.hasName();
      }

      @Override
      public String getNamespaceURI() {
         return reader.getNamespaceURI();
      }

      @Override
      public String getPrefix() {
         return reader.getPrefix();
      }

      @Override
      public String getVersion() {
         return reader.getVersion();
      }

      @Override
      public boolean isStandalone() {
         return reader.isStandalone();
      }

      @Override
      public boolean standaloneSet() {
         return reader.standaloneSet();
      }

      @Override
      public String getCharacterEncodingScheme() {
         return reader.getCharacterEncodingScheme();
      }

      @Override
      public String getPITarget() {
         return reader.getPITarget();
      }

      @Override
      public String getPIData() {
         return reader.getPIData();
      }
   }

   private void parse(Element e) {


      config.setBackup(XMLConfigurationUtil.getBoolean(e, "backup", config.isBackup()));

      config.setSharedStore(XMLConfigurationUtil.getBoolean(e, "shared-store", config.isSharedStore()));

      // Defaults to true when using FileConfiguration
      config.setFileDeploymentEnabled(XMLConfigurationUtil.getBoolean(e,
         "file-deployment-enabled",
         config instanceof FileConfiguration));

      config.setPersistenceEnabled(XMLConfigurationUtil.getBoolean(e,
         "persistence-enabled",
         config.isPersistenceEnabled()));

      config.setPersistDeliveryCountBeforeDelivery(XMLConfigurationUtil.getBoolean(e,
         "persist-delivery-count-before-delivery",
         config.isPersistDeliveryCountBeforeDelivery()));

      config.setScheduledThreadPoolMaxSize(XMLConfigurationUtil.getInteger(e,
         "scheduled-thread-pool-max-size",
         config.getScheduledThreadPoolMaxSize(),
         Validators.GT_ZERO));

      config.setThreadPoolMaxSize(XMLConfigurationUtil.getInteger(e,
         "thread-pool-max-size",
         config.getThreadPoolMaxSize(),
         Validators.MINUS_ONE_OR_GT_ZERO));

      config.setSecurityEnabled(XMLConfigurationUtil.getBoolean(e, "security-enabled", config.isSecurityEnabled()));

      config.setJMXManagementEnabled(XMLConfigurationUtil.getBoolean(e,
         "jmx-management-enabled",
         config.isJMXManagementEnabled()));

      config.setJMXDomain(XMLConfigurationUtil.getString(e,
         "jmx-domain",
         config.getJMXDomain(),
         Validators.NOT_NULL_OR_EMPTY));

      config.setSecurityInvalidationInterval(XMLConfigurationUtil.getLong(e,
         "security-invalidation-interval",
         config.getSecurityInvalidationInterval(),
         Validators.GT_ZERO));

      config.setConnectionTTLOverride(XMLConfigurationUtil.getLong(e,
         "connection-ttl-override",
         config.getConnectionTTLOverride(),
         Validators.MINUS_ONE_OR_GT_ZERO));

      config.setEnabledAsyncConnectionExecution(XMLConfigurationUtil.getBoolean(e,
         "async-connection-execution-enabled",
         config.isAsyncConnectionExecutionEnabled()));

      config.setTransactionTimeout(XMLConfigurationUtil.getLong(e,
         "transaction-timeout",
         config.getTransactionTimeout(),
         Validators.GT_ZERO));

      config.setTransactionTimeoutScanPeriod(XMLConfigurationUtil.getLong(e,
         "transaction-timeout-scan-period",
         config.getTransactionTimeoutScanPeriod(),
         Validators.GT_ZERO));

      config.setMessageExpiryScanPeriod(XMLConfigurationUtil.getLong(e,
         "message-expiry-scan-period",
         config.getMessageExpiryScanPeriod(),
         Validators.MINUS_ONE_OR_GT_ZERO));

      config.setMessageExpiryThreadPriority(XMLConfigurationUtil.getInteger(e,
         "message-expiry-thread-priority",
         config.getMessageExpiryThreadPriority(),
         Validators.THREAD_PRIORITY_RANGE));

      config.setIDCacheSize(XMLConfigurationUtil.getInteger(e,
         "id-cache-size",
         config.getIDCacheSize(),
         Validators.GT_ZERO));

      config.setPersistIDCache(XMLConfigurationUtil.getBoolean(e, "persist-id-cache", config.isPersistIDCache()));

      config.setManagementAddress(new SimpleString(XMLConfigurationUtil.getString(e,
         "management-address",
         config.getManagementAddress()
            .toString(),
         Validators.NOT_NULL_OR_EMPTY)));

      config.setManagementNotificationAddress(new SimpleString(XMLConfigurationUtil.getString(e,
         "management-notification-address",
         config.getManagementNotificationAddress()
            .toString(),
         Validators.NOT_NULL_OR_EMPTY)));

      config.setClusterPassword(XMLConfigurationUtil.getString(e,
         "cluster-password",
         config.getClusterPassword(),
         Validators.NO_CHECK));

      config.setClusterUser(XMLConfigurationUtil.getString(e,
         "cluster-user",
         config.getClusterUser(),
         Validators.NO_CHECK));

      config.setLogDelegateFactoryClassName(XMLConfigurationUtil.getString(e,
         "log-delegate-factory-class-name",
         config.getLogDelegateFactoryClassName(),
         Validators.NOT_NULL_OR_EMPTY));

      NodeList interceptorNodes = e.getElementsByTagName("remoting-interceptors");

      ArrayList<String> interceptorList = new ArrayList<String>();

      if (interceptorNodes.getLength() > 0) {
         NodeList interceptors = interceptorNodes.item(0).getChildNodes();

         for (int i = 0; i < interceptors.getLength(); i++) {
            if ("class-name".equalsIgnoreCase(interceptors.item(i).getNodeName())) {
               String clazz = interceptors.item(i).getTextContent();

               interceptorList.add(clazz);
            }
         }
      }

      config.setInterceptorClassNames(interceptorList);

      NodeList backups = e.getElementsByTagName("backup-connector-ref");

      // There should be only one - this will be enforced by the DTD

      if (backups.getLength() > 0) {
         Node backupNode = backups.item(0);

         config.setBackupConnectorName(backupNode.getAttributes().getNamedItem("connector-name").getNodeValue());
      }

      NodeList connectorNodes = e.getElementsByTagName("connector");

      for (int i = 0; i < connectorNodes.getLength(); i++) {
         org.w3c.dom.Element connectorNode = (Element) connectorNodes.item(i);

         TransportConfiguration connectorConfig = parseTransportConfiguration(connectorNode);

         if (connectorConfig.getName() == null) {
            log.warn("Cannot deploy a connector with no name specified.");

            continue;
         }

         if (config.getConnectorConfigurations().containsKey(connectorConfig.getName())) {
            log.warn("There is already a connector with name " + connectorConfig.getName() +
               " deployed. This one will not be deployed.");

            continue;
         }

         config.getConnectorConfigurations().put(connectorConfig.getName(), connectorConfig);
      }

      NodeList acceptorNodes = e.getElementsByTagName("acceptor");

      for (int i = 0; i < acceptorNodes.getLength(); i++) {
         Element acceptorNode = (Element) acceptorNodes.item(i);

         TransportConfiguration acceptorConfig = parseTransportConfiguration(acceptorNode);

         config.getAcceptorConfigurations().add(acceptorConfig);
      }

      NodeList bgNodes = e.getElementsByTagName("broadcast-group");

      for (int i = 0; i < bgNodes.getLength(); i++) {
         Element bgNode = (Element) bgNodes.item(i);

         parseBroadcastGroupConfiguration(bgNode, config);
      }

      NodeList dgNodes = e.getElementsByTagName("discovery-group");

      for (int i = 0; i < dgNodes.getLength(); i++) {
         Element dgNode = (Element) dgNodes.item(i);

         parseDiscoveryGroupConfiguration(dgNode, config);
      }

      NodeList brNodes = e.getElementsByTagName("bridge");

      for (int i = 0; i < brNodes.getLength(); i++) {
         Element mfNode = (Element) brNodes.item(i);

         parseBridgeConfiguration(mfNode, config);
      }

      NodeList gaNodes = e.getElementsByTagName("grouping-handler");

      for (int i = 0; i < gaNodes.getLength(); i++) {
         Element gaNode = (Element) gaNodes.item(i);

         parseGroupingHandlerConfiguration(gaNode, config);
      }

      NodeList ccNodes = e.getElementsByTagName("cluster-connection");

      for (int i = 0; i < ccNodes.getLength(); i++) {
         Element ccNode = (Element) ccNodes.item(i);

         parseClusterConnectionConfiguration(ccNode, config);
      }

      NodeList dvNodes = e.getElementsByTagName("divert");

      for (int i = 0; i < dvNodes.getLength(); i++) {
         Element dvNode = (Element) dvNodes.item(i);

         parseDivertConfiguration(dvNode, config);
      }

      // Persistence config

      config.setLargeMessagesDirectory(XMLConfigurationUtil.getString(e,
         "large-messages-directory",
         config.getLargeMessagesDirectory(),
         Validators.NOT_NULL_OR_EMPTY));

      config.setBindingsDirectory(XMLConfigurationUtil.getString(e,
         "bindings-directory",
         config.getBindingsDirectory(),
         Validators.NOT_NULL_OR_EMPTY));

      config.setCreateBindingsDir(XMLConfigurationUtil.getBoolean(e,
         "create-bindings-dir",
         config.isCreateBindingsDir()));

      config.setJournalDirectory(XMLConfigurationUtil.getString(e,
         "journal-directory",
         config.getJournalDirectory(),
         Validators.NOT_NULL_OR_EMPTY));

      config.setPagingDirectory(XMLConfigurationUtil.getString(e,
         "paging-directory",
         config.getPagingDirectory(),
         Validators.NOT_NULL_OR_EMPTY));

      config.setCreateJournalDir(XMLConfigurationUtil.getBoolean(e, "create-journal-dir", config.isCreateJournalDir()));

      String s = XMLConfigurationUtil.getString(e,
         "journal-type",
         config.getJournalType().toString(),
         Validators.JOURNAL_TYPE);

      if (s.equals(JournalType.NIO.toString())) {
         config.setJournalType(JournalType.NIO);
      } else if (s.equals(JournalType.ASYNCIO.toString())) {
         // https://jira.jboss.org/jira/browse/HORNETQ-295
         // We do the check here to see if AIO is supported so we can use the correct defaults and/or use
         // correct settings in xml
         // If we fall back later on these settings can be ignored
         boolean supportsAIO = AIOSequentialFileFactory.isSupported();

         if (supportsAIO) {
            config.setJournalType(JournalType.ASYNCIO);
         } else {
            log.warn("AIO wasn't located on this platform, it will fall back to using pure Java NIO. If your platform is Linux, install LibAIO to enable the AIO journal");

            config.setJournalType(JournalType.NIO);
         }
      }

      config.setJournalSyncTransactional(XMLConfigurationUtil.getBoolean(e,
         "journal-sync-transactional",
         config.isJournalSyncTransactional()));

      config.setJournalSyncNonTransactional(XMLConfigurationUtil.getBoolean(e,
         "journal-sync-non-transactional",
         config.isJournalSyncNonTransactional()));

      config.setJournalFileSize(XMLConfigurationUtil.getInteger(e,
         "journal-file-size",
         config.getJournalFileSize(),
         Validators.GT_ZERO));

      int journalBufferTimeout = XMLConfigurationUtil.getInteger(e,
         "journal-buffer-timeout",
         config.getJournalType() == JournalType.ASYNCIO ? ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_TIMEOUT_AIO
            : ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_TIMEOUT_NIO,
         Validators.GT_ZERO);

      int journalBufferSize = XMLConfigurationUtil.getInteger(e,
         "journal-buffer-size",
         config.getJournalType() == JournalType.ASYNCIO ? ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_SIZE_AIO
            : ConfigurationImpl.DEFAULT_JOURNAL_BUFFER_SIZE_NIO,
         Validators.GT_ZERO);

      int journalMaxIO = XMLConfigurationUtil.getInteger(e,
         "journal-max-io",
         config.getJournalType() == JournalType.ASYNCIO ? ConfigurationImpl.DEFAULT_JOURNAL_MAX_IO_AIO
            : ConfigurationImpl.DEFAULT_JOURNAL_MAX_IO_NIO,
         Validators.GT_ZERO);

      if (config.getJournalType() == JournalType.ASYNCIO) {
         config.setJournalBufferTimeout_AIO(journalBufferTimeout);
         config.setJournalBufferSize_AIO(journalBufferSize);
         config.setJournalMaxIO_AIO(journalMaxIO);
      } else {
         config.setJournalBufferTimeout_NIO(journalBufferTimeout);
         config.setJournalBufferSize_NIO(journalBufferSize);
         config.setJournalMaxIO_NIO(journalMaxIO);
      }

      config.setJournalMinFiles(XMLConfigurationUtil.getInteger(e,
         "journal-min-files",
         config.getJournalMinFiles(),
         Validators.GT_ZERO));

      config.setJournalCompactMinFiles(XMLConfigurationUtil.getInteger(e,
         "journal-compact-min-files",
         config.getJournalCompactMinFiles(),
         Validators.GE_ZERO));

      config.setJournalCompactPercentage(XMLConfigurationUtil.getInteger(e,
         "journal-compact-percentage",
         config.getJournalCompactPercentage(),
         Validators.PERCENTAGE));

      config.setLogJournalWriteRate(XMLConfigurationUtil.getBoolean(e,
         "log-journal-write-rate",
         ConfigurationImpl.DEFAULT_JOURNAL_LOG_WRITE_RATE));

      config.setJournalPerfBlastPages(XMLConfigurationUtil.getInteger(e,
         "perf-blast-pages",
         ConfigurationImpl.DEFAULT_JOURNAL_PERF_BLAST_PAGES,
         Validators.MINUS_ONE_OR_GT_ZERO));

      config.setRunSyncSpeedTest(XMLConfigurationUtil.getBoolean(e, "run-sync-speed-test", config.isRunSyncSpeedTest()));

      config.setWildcardRoutingEnabled(XMLConfigurationUtil.getBoolean(e,
         "wild-card-routing-enabled",
         config.isWildcardRoutingEnabled()));

      config.setMessageCounterEnabled(XMLConfigurationUtil.getBoolean(e,
         "message-counter-enabled",
         config.isMessageCounterEnabled()));

      config.setMessageCounterSamplePeriod(XMLConfigurationUtil.getLong(e,
         "message-counter-sample-period",
         config.getMessageCounterSamplePeriod(),
         Validators.GT_ZERO));

      config.setMessageCounterMaxDayHistory(XMLConfigurationUtil.getInteger(e,
         "message-counter-max-day-history",
         config.getMessageCounterMaxDayHistory(),
         Validators.GT_ZERO));

      config.setServerDumpInterval(XMLConfigurationUtil.getLong(e,
         "server-dump-interval",
         config.getServerDumpInterval(),
         Validators.MINUS_ONE_OR_GT_ZERO)); // in
      // milliseconds

      config.setMemoryWarningThreshold(XMLConfigurationUtil.getInteger(e,
         "memory-warning-threshold",
         config.getMemoryWarningThreshold(),
         Validators.PERCENTAGE));

      config.setMemoryMeasureInterval(XMLConfigurationUtil.getLong(e,
         "memory-measure-interval",
         config.getMemoryMeasureInterval(),
         Validators.MINUS_ONE_OR_GT_ZERO)); // in

      parseAddressSettings(e, config);

      parseQueues(e, config);

      parseSecurity(e, config);

      NodeList connectorServiceConfigs = e.getElementsByTagName("connector-service");

      ArrayList<ConnectorServiceConfiguration> configs = new ArrayList<ConnectorServiceConfiguration>();

      for (int i = 0; i < connectorServiceConfigs.getLength(); i++) {
         Element node = (Element) connectorServiceConfigs.item(i);

         configs.add((parseConnectorService(node)));
      }

      config.setConnectorServiceConfigurations(configs);
   }

   /**
    * @param e
    * @param config
    */
   private void parseSecurity(final Element e, final Configuration config) {
      NodeList elements = e.getElementsByTagName("security-settings");

      if (elements.getLength() != 0) {
         Element node = (Element) elements.item(0);
         NodeList list = node.getElementsByTagName("security-setting");
         for (int i = 0; i < list.getLength(); i++) {
            Pair<String, Set<Role>> securityItem = parseSecurityRoles(list.item(i));
            config.getSecurityRoles().put(securityItem.a, securityItem.b);
         }
      }
   }

   /**
    * @param e
    * @param config
    */
   private void parseQueues(final Element e, final Configuration config) {
      NodeList elements = e.getElementsByTagName("queues");

      if (elements.getLength() != 0) {
         Element node = (Element) elements.item(0);
         NodeList list = node.getElementsByTagName("queue");
         for (int i = 0; i < list.getLength(); i++) {
            CoreQueueConfiguration queueConfig = parseQueueConfiguration(list.item(i));
            config.getQueueConfigurations().add(queueConfig);
         }
      }
   }

   /**
    * @param e
    * @param config
    */
   private void parseAddressSettings(final Element e, final Configuration config) {
      NodeList elements = e.getElementsByTagName("address-settings");

      if (elements.getLength() != 0) {
         Element node = (Element) elements.item(0);
         NodeList list = node.getElementsByTagName("address-setting");
         for (int i = 0; i < list.getLength(); i++) {
            Pair<String, AddressSettings> addressSettings = parseAddressSettings(list.item(i));
            config.getAddressesSettings().put(addressSettings.a, addressSettings.b);
         }
      }
   }

   /**
    * @param node
    * @return
    */
   public Pair<String, Set<Role>> parseSecurityRoles(final Node node) {
      String match = node.getAttributes().getNamedItem("match").getNodeValue();

      HashSet<Role> securityRoles = new HashSet<Role>();

      Pair<String, Set<Role>> securityMatch = new Pair<String, Set<Role>>(match, securityRoles);

      ArrayList<String> send = new ArrayList<String>();
      ArrayList<String> consume = new ArrayList<String>();
      ArrayList<String> createDurableQueue = new ArrayList<String>();
      ArrayList<String> deleteDurableQueue = new ArrayList<String>();
      ArrayList<String> createNonDurableQueue = new ArrayList<String>();
      ArrayList<String> deleteNonDurableQueue = new ArrayList<String>();
      ArrayList<String> manageRoles = new ArrayList<String>();
      ArrayList<String> allRoles = new ArrayList<String>();
      NodeList children = node.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
         Node child = children.item(i);
/*
         if (PERMISSION_ELEMENT_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            String type = child.getAttributes().getNamedItem(TYPE_ATTR_NAME).getNodeValue();
            String roleString = child.getAttributes()
                                     .getNamedItem(ROLES_ATTR_NAME)
                                     .getNodeValue();
            String[] roles = roleString.split(",");
            for (String role : roles)
            {
               if (SEND_NAME.equals(type))
               {
                  send.add(role.trim());
               }
               else if (CONSUME_NAME.equals(type))
               {
                  consume.add(role.trim());
               }
               else if (CREATEDURABLEQUEUE_NAME.equals(type))
               {
                  createDurableQueue.add(role);
               }
               else if (DELETEDURABLEQUEUE_NAME.equals(type))
               {
                  deleteDurableQueue.add(role);
               }
               else if (CREATE_NON_DURABLE_QUEUE_NAME.equals(type))
               {
                  createNonDurableQueue.add(role);
               }
               else if (DELETE_NON_DURABLE_QUEUE_NAME.equals(type))
               {
                  deleteNonDurableQueue.add(role);
               }
               else if (CREATETEMPQUEUE_NAME.equals(type))
               {
                  createNonDurableQueue.add(role);
               }
               else if (DELETETEMPQUEUE_NAME.equals(type))
               {
                  deleteNonDurableQueue.add(role);
               }
               else if (MANAGE_NAME.equals(type))
               {
                  manageRoles.add(role);
               }
               if (!allRoles.contains(role.trim()))
               {
                  allRoles.add(role.trim());
               }
            }
         }
*/
      }

      for (String role : allRoles) {
         securityRoles.add(new Role(role,
            send.contains(role),
            consume.contains(role),
            createDurableQueue.contains(role),
            deleteDurableQueue.contains(role),
            createNonDurableQueue.contains(role),
            deleteNonDurableQueue.contains(role),
            manageRoles.contains(role)));
      }

      return securityMatch;
   }

   /**
    * @param node
    * @return
    */
   public Pair<String, AddressSettings> parseAddressSettings(final Node node) {
      String match = node.getAttributes().getNamedItem("match").getNodeValue();

      NodeList children = node.getChildNodes();

      AddressSettings addressSettings = new AddressSettings();

      Pair<String, AddressSettings> setting = new Pair<String, AddressSettings>(match, addressSettings);
/*
      for (int i = 0; i < children.getLength(); i++)
      {
         Node child = children.item(i);

         if (DEAD_LETTER_ADDRESS_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            SimpleString queueName = new SimpleString(child.getTextContent());
            addressSettings.setDeadLetterAddress(queueName);
         }
         else if (EXPIRY_ADDRESS_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            SimpleString queueName = new SimpleString(child.getTextContent());
            addressSettings.setExpiryAddress(queueName);
         }
         else if (REDELIVERY_DELAY_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setRedeliveryDelay(Long.valueOf(child.getTextContent()));
         }
         else if (MAX_SIZE_BYTES_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setMaxSizeBytes(Long.valueOf(child.getTextContent()));
         }
         else if (PAGE_SIZE_BYTES_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setPageSizeBytes(Long.valueOf(child.getTextContent()));
         }
         else if (MESSAGE_COUNTER_HISTORY_DAY_LIMIT_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setMessageCounterHistoryDayLimit(Integer.valueOf(child.getTextContent()));
         }
         else if (ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            String value = child.getTextContent().trim();
            Validators.ADDRESS_FULL_MESSAGE_POLICY_TYPE.validate(ADDRESS_FULL_MESSAGE_POLICY_NODE_NAME,
                                                                 value);
            AddressFullMessagePolicy policy = null;
            if (value.equals(AddressFullMessagePolicy.BLOCK.toString()))
            {
               policy = AddressFullMessagePolicy.BLOCK;
            }
            else if (value.equals(AddressFullMessagePolicy.DROP.toString()))
            {
               policy = AddressFullMessagePolicy.DROP;
            }
            else if (value.equals(AddressFullMessagePolicy.PAGE.toString()))
            {
               policy = AddressFullMessagePolicy.PAGE;
            }
            addressSettings.setAddressFullMessagePolicy(policy);
         }
         else if (LVQ_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setLastValueQueue(Boolean.valueOf(child.getTextContent().trim()));
         }
         else if (MAX_DELIVERY_ATTEMPTS.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setMaxDeliveryAttempts(Integer.valueOf(child.getTextContent().trim()));
         }
         else if (REDISTRIBUTION_DELAY_NODE_NAME.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setRedistributionDelay(Long.valueOf(child.getTextContent().trim()));
         }
         else if (SEND_TO_DLA_ON_NO_ROUTE.equalsIgnoreCase(child.getNodeName()))
         {
            addressSettings.setSendToDLAOnNoRoute(Boolean.valueOf(child.getTextContent().trim()));
         }
      }
 */
      return setting;
   }

   public CoreQueueConfiguration parseQueueConfiguration(final Node node) {
      String name = node.getAttributes().getNamedItem("name").getNodeValue();
      String address = null;
      String filterString = null;
      boolean durable = true;

      NodeList children = node.getChildNodes();

      for (int j = 0; j < children.getLength(); j++) {
         Node child = children.item(j);

         if (child.getNodeName().equals("address")) {
            address = child.getTextContent().trim();
         } else if (child.getNodeName().equals("filter")) {
            filterString = child.getAttributes().getNamedItem("string").getNodeValue();
         } else if (child.getNodeName().equals("durable")) {
            durable = Boolean.parseBoolean(child.getTextContent().trim());
         }
      }

      return new CoreQueueConfiguration(address, name, filterString, durable);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   private TransportConfiguration parseTransportConfiguration(final Element e) {
      Node nameNode = e.getAttributes().getNamedItem("name");

      String name = nameNode != null ? nameNode.getNodeValue() : null;

      String clazz = XMLConfigurationUtil.getString(e, "factory-class", null, Validators.NOT_NULL_OR_EMPTY);

      Map<String, Object> params = new HashMap<String, Object>();

      NodeList paramsNodes = e.getElementsByTagName("param");

      for (int i = 0; i < paramsNodes.getLength(); i++) {
         Node paramNode = paramsNodes.item(i);

         NamedNodeMap attributes = paramNode.getAttributes();

         Node nkey = attributes.getNamedItem("key");

         String key = nkey.getTextContent();

         Node nValue = attributes.getNamedItem("value");

         params.put(key, nValue.getTextContent());
      }

      return new TransportConfiguration(clazz, params, name);
   }

   private void parseBroadcastGroupConfiguration(final Element e, final Configuration mainConfig) {
      String name = e.getAttribute("name");

      String localAddress = XMLConfigurationUtil.getString(e, "local-bind-address", null, Validators.NO_CHECK);

      int localBindPort = XMLConfigurationUtil.getInteger(e, "local-bind-port", -1, Validators.MINUS_ONE_OR_GT_ZERO);

      String groupAddress = XMLConfigurationUtil.getString(e, "group-address", null, Validators.NOT_NULL_OR_EMPTY);

      int groupPort = XMLConfigurationUtil.getInteger(e, "group-port", -1, Validators.GT_ZERO);

      long broadcastPeriod = XMLConfigurationUtil.getLong(e,
         "broadcast-period",
         ConfigurationImpl.DEFAULT_BROADCAST_PERIOD,
         Validators.GT_ZERO);

      NodeList children = e.getChildNodes();

      List<Pair<String, String>> connectorNames = new ArrayList<Pair<String, String>>();

      for (int j = 0; j < children.getLength(); j++) {
         Node child = children.item(j);

         if (child.getNodeName().equals("connector-ref")) {
            String connectorName = child.getAttributes().getNamedItem("connector-name").getNodeValue();

            Node backupConnectorNode = child.getAttributes().getNamedItem("backup-connector-name");

            String backupConnectorName = null;

            if (backupConnectorNode != null) {
               backupConnectorName = backupConnectorNode.getNodeValue();
            }

            Pair<String, String> connectorInfo = new Pair<String, String>(connectorName, backupConnectorName);

            connectorNames.add(connectorInfo);
         }
      }

      BroadcastGroupConfiguration config = new BroadcastGroupConfiguration(name,
         localAddress,
         localBindPort,
         groupAddress,
         groupPort,
         broadcastPeriod,
         connectorNames);

      mainConfig.getBroadcastGroupConfigurations().add(config);
   }

   private void parseDiscoveryGroupConfiguration(final Element e, final Configuration mainConfig) {
      String name = e.getAttribute("name");

      String localBindAddress = XMLConfigurationUtil.getString(e, "local-bind-address", null, Validators.NO_CHECK);

      String groupAddress = XMLConfigurationUtil.getString(e, "group-address", null, Validators.NOT_NULL_OR_EMPTY);

      int groupPort = XMLConfigurationUtil.getInteger(e, "group-port", -1, Validators.MINUS_ONE_OR_GT_ZERO);

      long refreshTimeout = XMLConfigurationUtil.getLong(e,
         "refresh-timeout",
         ConfigurationImpl.DEFAULT_BROADCAST_REFRESH_TIMEOUT,
         Validators.GT_ZERO);

      DiscoveryGroupConfiguration config = new DiscoveryGroupConfiguration(name,
         localBindAddress,
         groupAddress,
         groupPort,
         refreshTimeout);

      if (mainConfig.getDiscoveryGroupConfigurations().containsKey(name)) {
         log.warn("There is already a discovery group with name " + name +
            " deployed. This one will not be deployed.");

         return;
      } else {
         mainConfig.getDiscoveryGroupConfigurations().put(name, config);
      }
   }

   private void parseClusterConnectionConfiguration(final Element e, final Configuration mainConfig) {
      String name = e.getAttribute("name");

      String address = XMLConfigurationUtil.getString(e, "address", null, Validators.NOT_NULL_OR_EMPTY);

      boolean duplicateDetection = XMLConfigurationUtil.getBoolean(e,
         "use-duplicate-detection",
         ConfigurationImpl.DEFAULT_CLUSTER_DUPLICATE_DETECTION);

      boolean forwardWhenNoConsumers = XMLConfigurationUtil.getBoolean(e,
         "forward-when-no-consumers",
         ConfigurationImpl.DEFAULT_CLUSTER_FORWARD_WHEN_NO_CONSUMERS);

      int maxHops = XMLConfigurationUtil.getInteger(e,
         "max-hops",
         ConfigurationImpl.DEFAULT_CLUSTER_MAX_HOPS,
         Validators.GE_ZERO);

      long retryInterval = XMLConfigurationUtil.getLong(e,
         "retry-interval",
         ConfigurationImpl.DEFAULT_CLUSTER_RETRY_INTERVAL,
         Validators.GT_ZERO);

      int confirmationWindowSize = XMLConfigurationUtil.getInteger(e,
         "confirmation-window-size",
         FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE,
         Validators.GT_ZERO);

      String discoveryGroupName = null;

      List<Pair<String, String>> connectorPairs = new ArrayList<Pair<String, String>>();

      NodeList children = e.getChildNodes();

      for (int j = 0; j < children.getLength(); j++) {
         Node child = children.item(j);

         if (child.getNodeName().equals("discovery-group-ref")) {
            discoveryGroupName = child.getAttributes().getNamedItem("discovery-group-name").getNodeValue();
         } else if (child.getNodeName().equals("connector-ref")) {
            String connectorName = child.getAttributes().getNamedItem("connector-name").getNodeValue();

            Node backupNode = child.getAttributes().getNamedItem("backup-connector-name");

            String backupConnectorName = null;

            if (backupNode != null) {
               backupConnectorName = backupNode.getNodeValue();
            }

            Pair<String, String> connectorPair = new Pair<String, String>(connectorName, backupConnectorName);

            connectorPairs.add(connectorPair);
         }
      }

      ClusterConnectionConfiguration config;

      if (discoveryGroupName == null) {
         config = new ClusterConnectionConfiguration(name,
            address,
            retryInterval,
            duplicateDetection,
            forwardWhenNoConsumers,
            maxHops,
            confirmationWindowSize,
            connectorPairs);
      } else {
         config = new ClusterConnectionConfiguration(name,
            address,
            retryInterval,
            duplicateDetection,
            forwardWhenNoConsumers,
            maxHops,
            confirmationWindowSize,
            discoveryGroupName);
      }

      mainConfig.getClusterConfigurations().add(config);
   }

   private void parseGroupingHandlerConfiguration(final Element node, final Configuration mainConfiguration) {
      String name = node.getAttribute("name");
      String type = XMLConfigurationUtil.getString(node, "type", null, Validators.NOT_NULL_OR_EMPTY);
      String address = XMLConfigurationUtil.getString(node, "address", null, Validators.NOT_NULL_OR_EMPTY);
      Integer timeout = XMLConfigurationUtil.getInteger(node,
         "timeout",
         GroupingHandlerConfiguration.DEFAULT_TIMEOUT,
         Validators.GT_ZERO);
      mainConfiguration.setGroupingHandlerConfiguration(new GroupingHandlerConfiguration(new SimpleString(name),
         type.equals(GroupingHandlerConfiguration.TYPE.LOCAL.getType()) ? GroupingHandlerConfiguration.TYPE.LOCAL
            : GroupingHandlerConfiguration.TYPE.REMOTE,
         new SimpleString(address),
         timeout));
   }

   private void parseBridgeConfiguration(final Element brNode, final Configuration mainConfig) {
      String name = brNode.getAttribute("name");

      String queueName = XMLConfigurationUtil.getString(brNode, "queue-name", null, Validators.NOT_NULL_OR_EMPTY);

      String forwardingAddress = XMLConfigurationUtil.getString(brNode, "forwarding-address", null, Validators.NO_CHECK);

      String transformerClassName = XMLConfigurationUtil.getString(brNode,
         "transformer-class-name",
         null,
         Validators.NO_CHECK);

      long retryInterval = XMLConfigurationUtil.getLong(brNode,
         "retry-interval",
         HornetQClient.DEFAULT_RETRY_INTERVAL,
         Validators.GT_ZERO);

      // Default bridge conf
      int confirmationWindowSize = XMLConfigurationUtil.getInteger(brNode,
         "confirmation-window-size",
         FileConfiguration.DEFAULT_CONFIRMATION_WINDOW_SIZE,
         Validators.GT_ZERO);

      double retryIntervalMultiplier = XMLConfigurationUtil.getDouble(brNode,
         "retry-interval-multiplier",
         HornetQClient.DEFAULT_RETRY_INTERVAL_MULTIPLIER,
         Validators.GT_ZERO);

      int reconnectAttempts = XMLConfigurationUtil.getInteger(brNode,
         "reconnect-attempts",
         ConfigurationImpl.DEFAULT_BRIDGE_RECONNECT_ATTEMPTS,
         Validators.MINUS_ONE_OR_GE_ZERO);

      boolean failoverOnServerShutdown = XMLConfigurationUtil.getBoolean(brNode,
         "failover-on-server-shutdown",
         HornetQClient.DEFAULT_FAILOVER_ON_SERVER_SHUTDOWN);

      boolean useDuplicateDetection = XMLConfigurationUtil.getBoolean(brNode,
         "use-duplicate-detection",
         ConfigurationImpl.DEFAULT_BRIDGE_DUPLICATE_DETECTION);

      String user = XMLConfigurationUtil.getString(brNode,
         "user",
         ConfigurationImpl.DEFAULT_CLUSTER_USER,
         Validators.NO_CHECK);

      String password = XMLConfigurationUtil.getString(brNode,
         "password",
         ConfigurationImpl.DEFAULT_CLUSTER_PASSWORD,
         Validators.NO_CHECK);

      String filterString = null;

      Pair<String, String> connectorPair = null;

      String discoveryGroupName = null;

      NodeList children = brNode.getChildNodes();

      for (int j = 0; j < children.getLength(); j++) {
         Node child = children.item(j);

         if (child.getNodeName().equals("filter")) {
            filterString = child.getAttributes().getNamedItem("string").getNodeValue();
         } else if (child.getNodeName().equals("discovery-group-ref")) {
            discoveryGroupName = child.getAttributes().getNamedItem("discovery-group-name").getNodeValue();
         } else if (child.getNodeName().equals("connector-ref")) {
            String connectorName = child.getAttributes().getNamedItem("connector-name").getNodeValue();

            Node backupNode = child.getAttributes().getNamedItem("backup-connector-name");

            String backupConnectorName = null;

            if (backupNode != null) {
               backupConnectorName = backupNode.getNodeValue();
            }

            connectorPair = new Pair<String, String>(connectorName, backupConnectorName);
         }
      }

      BridgeConfiguration config;

      if (connectorPair != null) {
         config = new BridgeConfiguration(name,
            queueName,
            forwardingAddress,
            filterString,
            transformerClassName,
            retryInterval,
            retryIntervalMultiplier,
            reconnectAttempts,
            failoverOnServerShutdown,
            useDuplicateDetection,
            confirmationWindowSize,
            HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
            connectorPair,
            user,
            password);
      } else {
         config = new BridgeConfiguration(name,
            queueName,
            forwardingAddress,
            filterString,
            transformerClassName,
            retryInterval,
            retryIntervalMultiplier,
            reconnectAttempts,
            failoverOnServerShutdown,
            useDuplicateDetection,
            confirmationWindowSize,
            HornetQClient.DEFAULT_CLIENT_FAILURE_CHECK_PERIOD,
            discoveryGroupName,
            user,
            password);
      }

      mainConfig.getBridgeConfigurations().add(config);
   }

   private void parseDivertConfiguration(final Element e, final Configuration mainConfig) {
      String name = e.getAttribute("name");

      String routingName = XMLConfigurationUtil.getString(e, "routing-name", null, Validators.NO_CHECK);

      String address = XMLConfigurationUtil.getString(e, "address", null, Validators.NOT_NULL_OR_EMPTY);

      String forwardingAddress = XMLConfigurationUtil.getString(e,
         "forwarding-address",
         null,
         Validators.NOT_NULL_OR_EMPTY);

      boolean exclusive = XMLConfigurationUtil.getBoolean(e, "exclusive", ConfigurationImpl.DEFAULT_DIVERT_EXCLUSIVE);

      String transformerClassName = XMLConfigurationUtil.getString(e,
         "transformer-class-name",
         null,
         Validators.NO_CHECK);

      String filterString = null;

      NodeList children = e.getChildNodes();

      for (int j = 0; j < children.getLength(); j++) {
         Node child = children.item(j);

         if (child.getNodeName().equals("filter")) {
            filterString = child.getAttributes().getNamedItem("string").getNodeValue();
         }
      }

      DivertConfiguration config = new DivertConfiguration(name,
         routingName,
         address,
         forwardingAddress,
         exclusive,
         filterString,
         transformerClassName);

      mainConfig.getDivertConfigurations().add(config);
   }

   private ConnectorServiceConfiguration parseConnectorService(final Element e) {
      Node nameNode = e.getAttributes().getNamedItem("name");

      String name = nameNode != null ? nameNode.getNodeValue() : null;

      String clazz = XMLConfigurationUtil.getString(e, "factory-class", null, Validators.NOT_NULL_OR_EMPTY);

      Map<String, Object> params = new HashMap<String, Object>();

      NodeList paramsNodes = e.getElementsByTagName("param");

      for (int i = 0; i < paramsNodes.getLength(); i++) {
         Node paramNode = paramsNodes.item(i);

         NamedNodeMap attributes = paramNode.getAttributes();

         Node nkey = attributes.getNamedItem("key");

         String key = nkey.getTextContent();

         Node nValue = attributes.getNamedItem("value");

         params.put(key, nValue.getTextContent());
      }

      return new ConnectorServiceConfiguration(clazz, params, name);
   }

   private enum StaxEvent {
      START_ELEMENT(1),
      END_ELEMENT(2),
      PROCESSING_INSTRUCTION(3),
      CHARACTERS(4),
      COMMENT(5),
      SPACE(6),
      START_DOCUMENT(7),
      END_DOCUMENT(8),
      ENTITY_REFERENCE(9),
      ATTRIBUTE(10),
      DTD(11),
      CDATA(12),
      NAMESPACE(13),
      NOTATION_DECLARATION(14),
      ENTITY_DECLARATION(15);
      private static StaxEvent[] EVENTS = {
         START_ELEMENT,
         END_ELEMENT,
         PROCESSING_INSTRUCTION,
         CHARACTERS,
         COMMENT,
         SPACE,
         START_DOCUMENT,
         END_DOCUMENT,
         ENTITY_REFERENCE,
         ATTRIBUTE,
         DTD,
         CDATA,
         NAMESPACE,
         NOTATION_DECLARATION,
         ENTITY_DECLARATION
      };

      private final int tag;

      StaxEvent(int tag) {
         this.tag = tag;
      }

      static StaxEvent tagToEvent(int tag) {
         return EVENTS[tag - 1];
      }
   }
}
