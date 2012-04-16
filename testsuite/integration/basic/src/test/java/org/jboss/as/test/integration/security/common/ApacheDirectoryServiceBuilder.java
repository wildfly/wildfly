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

package org.jboss.as.test.integration.security.common;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.directory.server.config.ConfigSchemaConstants;
import org.apache.directory.server.config.ConfigurationException;
import org.apache.directory.server.config.beans.AuthenticationInterceptorBean;
import org.apache.directory.server.config.beans.AuthenticatorBean;
import org.apache.directory.server.config.beans.AuthenticatorImplBean;
import org.apache.directory.server.config.beans.ChangeLogBean;
import org.apache.directory.server.config.beans.DelegatingAuthenticatorBean;
import org.apache.directory.server.config.beans.DirectoryServiceBean;
import org.apache.directory.server.config.beans.ExtendedOpHandlerBean;
import org.apache.directory.server.config.beans.IndexBean;
import org.apache.directory.server.config.beans.InterceptorBean;
import org.apache.directory.server.config.beans.JdbmIndexBean;
import org.apache.directory.server.config.beans.JdbmPartitionBean;
import org.apache.directory.server.config.beans.JournalBean;
import org.apache.directory.server.config.beans.LdapServerBean;
import org.apache.directory.server.config.beans.PartitionBean;
import org.apache.directory.server.config.beans.PasswordPolicyBean;
import org.apache.directory.server.config.beans.ReplConsumerBean;
import org.apache.directory.server.config.beans.SaslMechHandlerBean;
import org.apache.directory.server.config.beans.TcpTransportBean;
import org.apache.directory.server.config.beans.TransportBean;
import org.apache.directory.server.config.beans.UdpTransportBean;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.journal.JournalStore;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.authn.DelegatingAuthenticator;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.apache.directory.server.core.changelog.DefaultChangeLog;
import org.apache.directory.server.core.journal.DefaultJournal;
import org.apache.directory.server.core.journal.DefaultJournalStore;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.replication.ReplicationTrustManager;
import org.apache.directory.server.ldap.replication.SyncreplConfiguration;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.ldap.replication.provider.ReplicationRequestHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.model.ldif.LdifEntry;
import org.apache.directory.shared.ldap.model.ldif.LdifReader;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.jboss.logging.Logger;

/**
 * A class used for reading the configuration present in a Partition and instantiate the necessary objects like
 * DirectoryService, Interceptors etc.
 * 
 * @author Josef Cacek
 */
public class ApacheDirectoryServiceBuilder {
    /** The logger for this class */
    private static final Logger LOG = Logger.getLogger(ApacheDirectoryServiceBuilder.class);

    /** LDIF file filter */
    private static FilenameFilter ldifFilter = new FilenameFilter() {
        public boolean accept(File file, String name) {
            if (file.isDirectory()) {
                return true;
            }

            return file.getName().toLowerCase(Locale.ENGLISH).endsWith(".ldif");
        }
    };

    /**
     * Creates the Interceptor instances from the configuration
     * 
     * @param dirServiceDN the Dn under which interceptors are configured
     * @return a list of instantiated Interceptor objects
     * @throws Exception If the instanciation failed
     */
    public static List<Interceptor> createInterceptors(List<InterceptorBean> interceptorBeans) throws LdapException {
        List<Interceptor> interceptors = new ArrayList<Interceptor>(interceptorBeans.size());

        // First order the interceptorBeans
        Set<InterceptorBean> orderedInterceptorBeans = new TreeSet<InterceptorBean>();

        for (InterceptorBean interceptorBean : interceptorBeans) {
            if (interceptorBean.isEnabled()) {
                orderedInterceptorBeans.add(interceptorBean);
            }
        }

        // Instantiate the interceptors now
        for (InterceptorBean interceptorBean : orderedInterceptorBeans) {
            try {
                LOG.debug("loading the interceptor class and instantiating: " + interceptorBean.getInterceptorClassName());
                Interceptor interceptor = (Interceptor) Class.forName(interceptorBean.getInterceptorClassName()).newInstance();

                if (interceptorBean instanceof AuthenticationInterceptorBean) {
                    // Transports
                    Authenticator[] authenticators = createAuthenticators(((AuthenticationInterceptorBean) interceptorBean)
                            .getAuthenticators());
                    ((AuthenticationInterceptor) interceptor).setAuthenticators(authenticators);

                    // password policies
                    List<PasswordPolicyBean> ppolicyBeans = ((AuthenticationInterceptorBean) interceptorBean)
                            .getPasswordPolicies();
                    PpolicyConfigContainer ppolicyContainer = new PpolicyConfigContainer();

                    for (PasswordPolicyBean ppolicyBean : ppolicyBeans) {
                        PasswordPolicyConfiguration ppolicyConfig = createPwdPolicyConfig(ppolicyBean);

                        if (ppolicyConfig != null) {
                            // the name should be strictly 'default', the default policy can't be enforced by defining a new AT
                            if (ppolicyBean.getPwdId().equalsIgnoreCase("default")) {
                                ppolicyContainer.setDefaultPolicy(ppolicyConfig);
                            } else {
                                ppolicyContainer.addPolicy(ppolicyBean.getDn(), ppolicyConfig);
                            }
                        }
                    }

                    ((AuthenticationInterceptor) interceptor).setPwdPolicies(ppolicyContainer);
                }

                interceptors.add(interceptor);
            } catch (Exception e) {
                e.printStackTrace();
                String message = "Cannot initialize the " + interceptorBean.getInterceptorClassName() + ", error : " + e;
                LOG.error(message);
                throw new ConfigurationException(message);
            }
        }

        return interceptors;
    }

    /**
     * creates the PassworddPolicyConfiguration object after reading the config entry containing pwdpolicy OC
     * 
     * @param PasswordPolicyBean The Bean containing the PasswordPolicy configuration
     * @return the {@link PasswordPolicyConfiguration} object, null if the pwdpolicy entry is not present or disabled
     */
    public static PasswordPolicyConfiguration createPwdPolicyConfig(PasswordPolicyBean passwordPolicyBean) {
        if ((passwordPolicyBean == null) || passwordPolicyBean.isDisabled()) {
            return null;
        }

        PasswordPolicyConfiguration passwordPolicy = new PasswordPolicyConfiguration();

        passwordPolicy.setPwdAllowUserChange(passwordPolicyBean.isPwdAllowUserChange());
        passwordPolicy.setPwdAttribute(passwordPolicyBean.getPwdAttribute());
        passwordPolicy.setPwdCheckQuality(passwordPolicyBean.getPwdCheckQuality());
        passwordPolicy.setPwdExpireWarning(passwordPolicyBean.getPwdExpireWarning());
        passwordPolicy.setPwdFailureCountInterval(passwordPolicyBean.getPwdFailureCountInterval());
        passwordPolicy.setPwdGraceAuthNLimit(passwordPolicyBean.getPwdGraceAuthNLimit());
        passwordPolicy.setPwdGraceExpire(passwordPolicyBean.getPwdGraceExpire());
        passwordPolicy.setPwdInHistory(passwordPolicyBean.getPwdInHistory());
        passwordPolicy.setPwdLockout(passwordPolicyBean.isPwdLockout());
        passwordPolicy.setPwdLockoutDuration(passwordPolicyBean.getPwdLockoutDuration());
        passwordPolicy.setPwdMaxAge(passwordPolicyBean.getPwdMaxAge());
        passwordPolicy.setPwdMaxDelay(passwordPolicyBean.getPwdMaxDelay());
        passwordPolicy.setPwdMaxFailure(passwordPolicyBean.getPwdMaxFailure());
        passwordPolicy.setPwdMaxIdle(passwordPolicyBean.getPwdMaxIdle());
        passwordPolicy.setPwdMaxLength(passwordPolicyBean.getPwdMaxLength());
        passwordPolicy.setPwdMinAge(passwordPolicyBean.getPwdMinAge());
        passwordPolicy.setPwdMinDelay(passwordPolicyBean.getPwdMinDelay());
        passwordPolicy.setPwdMinLength(passwordPolicyBean.getPwdMinLength());
        passwordPolicy.setPwdMustChange(passwordPolicyBean.isPwdMustChange());
        passwordPolicy.setPwdSafeModify(passwordPolicyBean.isPwdSafeModify());

        return passwordPolicy;
    }

    /**
     * Read the configuration for the ChangeLog system
     * 
     * @param changelogBean The Bean containing the ChangeLog configuration
     * @return The instantiated ChangeLog element
     */
    public static ChangeLog createChangeLog(ChangeLogBean changeLogBean) {
        if ((changeLogBean == null) || changeLogBean.isDisabled()) {
            return null;
        }

        ChangeLog changeLog = new DefaultChangeLog();

        changeLog.setEnabled(changeLogBean.isEnabled());
        changeLog.setExposed(changeLogBean.isChangeLogExposed());

        return changeLog;
    }

    /**
     * Instantiate the Journal object from the stored configuration
     * 
     * @param changelogBean The Bean containing the ChangeLog configuration
     * @return An instance of Journal
     */
    public static Journal createJournal(JournalBean journalBean) {
        if ((journalBean == null) || journalBean.isDisabled()) {
            return null;
        }

        Journal journal = new DefaultJournal();

        journal.setRotation(journalBean.getJournalRotation());
        journal.setEnabled(journalBean.isEnabled());

        JournalStore store = new DefaultJournalStore();

        store.setFileName(journalBean.getJournalFileName());
        store.setWorkingDirectory(journalBean.getJournalWorkingDir());

        journal.setJournalStore(store);

        return journal;
    }

    /**
     * Load the Test entries
     * 
     * @param entryFilePath The place on disk where the test entries are stored
     * @return A list of LdifEntry elements
     * @throws ConfigurationException If we weren't able to read the entries
     */
    public static List<LdifEntry> readTestEntries(String entryFilePath) throws ConfigurationException {
        List<LdifEntry> entries = new ArrayList<LdifEntry>();

        File file = new File(entryFilePath);

        if (!file.exists()) {
            LOG.warn("LDIF test entry file path doesn't exist: " + entryFilePath);
        } else {
            LOG.debug("parsing the LDIF file(s) present at the path: " + entryFilePath);

            try {
                loadEntries(file, entries);
            } catch (LdapLdifException e) {
                String message = "Error while parsing a LdifEntry : " + e.getMessage();
                LOG.error(message);
                throw new ConfigurationException(message);
            } catch (IOException e) {
                String message = "cannot read the Ldif entries from the " + entryFilePath + " location";
                LOG.error(message);
                throw new ConfigurationException(message);
            }
        }

        return entries;
    }

    /**
     * Load the entries from a Ldif file recursively
     * 
     * @throws LdapLdifException
     * @throws IOException
     */
    private static void loadEntries(File ldifFile, List<LdifEntry> entries) throws LdapLdifException, IOException {
        if (ldifFile.isDirectory()) {
            File[] files = ldifFile.listFiles(ldifFilter);

            for (File f : files) {
                loadEntries(f, entries);
            }
        } else {
            LdifReader reader = new LdifReader();

            try {
                entries.addAll(reader.parseLdifFile(ldifFile.getAbsolutePath()));
            } finally {
                reader.close();
            }
        }
    }

    /**
     * Loads and instantiates a MechanismHandler from the configuration entry
     * 
     * @param saslMechHandlerEntry the entry of OC type {@link ConfigSchemaConstants#ADS_LDAP_SERVER_SASL_MECH_HANDLER_OC}
     * @return an instance of the MechanismHandler type
     * @throws ConfigurationException if the SASL mechanism handler cannot be created
     */
    public static MechanismHandler createSaslMechHandler(SaslMechHandlerBean saslMechHandlerBean) throws ConfigurationException {
        if ((saslMechHandlerBean == null) || saslMechHandlerBean.isDisabled()) {
            return null;
        }

        String mechClassName = saslMechHandlerBean.getSaslMechClassName();

        Class<?> mechClass = null;

        try {
            mechClass = Class.forName(mechClassName);
        } catch (ClassNotFoundException e) {
            String message = "Cannot find the class " + mechClassName;
            LOG.error(message);
            throw new ConfigurationException(message);
        }

        MechanismHandler handler = null;

        try {
            handler = (MechanismHandler) mechClass.newInstance();
        } catch (InstantiationException e) {
            String message = "Cannot instantiate the class : " + mechClassName;
            LOG.error(message);
            throw new ConfigurationException(message);
        } catch (IllegalAccessException e) {
            String message = "Cnnot invoke the class' constructor for " + mechClassName;
            LOG.error(message);
            throw new ConfigurationException(message);
        }

        if (mechClass == NtlmMechanismHandler.class) {
            NtlmMechanismHandler ntlmHandler = (NtlmMechanismHandler) handler;
            ntlmHandler.setNtlmProviderFqcn(saslMechHandlerBean.getNtlmMechProvider());
        }

        return handler;
    }

    /**
     * Creates a Authenticator from the configuration
     * 
     * @param authenticatorBean The created instance of authenticator
     * @return An instance of authenticator if the given authenticatorBean is not disabled
     */
    public static Authenticator createAuthenticator(AuthenticatorBean authenticatorBean) throws ConfigurationException {
        if (authenticatorBean.isDisabled()) {
            return null;
        }

        Authenticator authenticator = null;

        if (authenticatorBean instanceof DelegatingAuthenticatorBean) {
            authenticator = new DelegatingAuthenticator();
            ((DelegatingAuthenticator) authenticator).setDelegateHost(((DelegatingAuthenticatorBean) authenticatorBean)
                    .getDelegateHost());
            ((DelegatingAuthenticator) authenticator).setDelegatePort(((DelegatingAuthenticatorBean) authenticatorBean)
                    .getDelegatePort());
        } else if (authenticatorBean instanceof AuthenticatorImplBean) {
            String fqcn = ((AuthenticatorImplBean) authenticatorBean).getAuthenticatorClass();

            try {
                Class<?> authnImplClass = Class.forName(fqcn);
                authenticator = (Authenticator) authnImplClass.newInstance();
            } catch (Exception e) {
                String errorMsg = "Failed to instantiate the configured authenticator "
                        + authenticatorBean.getAuthenticatorId();
                LOG.warn(errorMsg);
                throw new ConfigurationException(errorMsg, e);
            }
        }

        return authenticator;
    }

    /**
     * Creates a Transport from the configuration
     * 
     * @param transportBean The created instance of transport
     * @return An instance of transport
     */
    public static Transport createTransport(TransportBean transportBean) {
        if ((transportBean == null) || transportBean.isDisabled()) {
            return null;
        }

        Transport transport = null;

        if (transportBean instanceof TcpTransportBean) {
            transport = new TcpTransport();
        } else if (transportBean instanceof UdpTransportBean) {
            transport = new UdpTransport();
        }

        transport.setPort(transportBean.getSystemPort());
        transport.setAddress(transportBean.getTransportAddress());
        transport.setBackLog(transportBean.getTransportBackLog());
        transport.setEnableSSL(transportBean.isTransportEnableSSL());
        transport.setNbThreads(transportBean.getTransportNbThreads());

        return transport;
    }

    /**
     * Creates the array of transports read from the DIT
     * 
     * @param transportBeans The array of Transport configuration
     * @return An arry of Transport instance
     */
    public static Authenticator[] createAuthenticators(List<AuthenticatorBean> list) throws ConfigurationException {
        Authenticator[] authenticators = new Authenticator[list.size()];
        int i = 0;

        for (AuthenticatorBean authenticatorBean : list) {
            authenticators[i++] = createAuthenticator(authenticatorBean);
        }

        return authenticators;
    }

    /**
     * Creates the array of transports read from the DIT
     * 
     * @param transportBeans The array of Transport configuration
     * @return An arry of Transport instance
     */
    public static Transport[] createTransports(TransportBean[] transportBeans) {
        List<Transport> transports = new ArrayList<Transport>();

        for (TransportBean transportBean : transportBeans) {
            if (transportBean.isEnabled()) {
                transports.add(createTransport(transportBean));
            }
        }

        return transports.toArray(new Transport[transports.size()]);
    }

    /**
     * Instantiates a LdapServer based on the configuration present in the partition
     * 
     * @param ldapServerBean The LdapServerBean containing the LdapServer configuration
     * @return Instance of LdapServer
     * @throws LdapException
     */
    public static LdapServer createLdapServer(LdapServerBean ldapServerBean, DirectoryService directoryService)
            throws LdapException {
        // Fist, do nothing if the LdapServer is disabled
        if ((ldapServerBean == null) || ldapServerBean.isDisabled()) {
            return null;
        }

        LdapServer ldapServer = new LdapServer();

        ldapServer.setDirectoryService(directoryService);
        ldapServer.setEnabled(true);

        // The ID
        ldapServer.setServiceId(ldapServerBean.getServerId());

        // SearchBaseDN
        ldapServer.setSearchBaseDn(ldapServerBean.getSearchBaseDn().getName());

        // KeyStore
        ldapServer.setKeystoreFile(ldapServerBean.getLdapServerKeystoreFile());

        // Certificate password
        ldapServer.setCertificatePassword(ldapServerBean.getLdapServerCertificatePassword());

        // ConfidentialityRequired
        ldapServer.setConfidentialityRequired(ldapServerBean.isLdapServerConfidentialityRequired());

        // Max size limit
        ldapServer.setMaxSizeLimit(ldapServerBean.getLdapServerMaxSizeLimit());

        // Max time limit
        ldapServer.setMaxTimeLimit(ldapServerBean.getLdapServerMaxTimeLimit());

        // Sasl Host
        ldapServer.setSaslHost(ldapServerBean.getLdapServerSaslHost());

        // Sasl Principal
        ldapServer.setSaslPrincipal(ldapServerBean.getLdapServerSaslPrincipal());

        // Sasl realm
        ldapServer.setSaslRealms(ldapServerBean.getLdapServerSaslRealms());

        // The transports
        Transport[] transports = createTransports(ldapServerBean.getTransports());
        ldapServer.setTransports(transports);

        // SaslMechs
        for (SaslMechHandlerBean saslMechHandlerBean : ldapServerBean.getSaslMechHandlers()) {
            if (saslMechHandlerBean.isEnabled()) {
                String mechanism = saslMechHandlerBean.getSaslMechName();
                ldapServer.addSaslMechanismHandler(mechanism, createSaslMechHandler(saslMechHandlerBean));
            }
        }

        // ExtendedOpHandlers
        for (ExtendedOpHandlerBean extendedpHandlerBean : ldapServerBean.getExtendedOps()) {
            if (extendedpHandlerBean.isEnabled()) {
                try {
                    Class<?> extendedOpClass = Class.forName(extendedpHandlerBean.getExtendedOpHandlerClass());
                    @SuppressWarnings("rawtypes")
                    ExtendedOperationHandler extOpHandler = (ExtendedOperationHandler) extendedOpClass.newInstance();
                    ldapServer.addExtendedOperationHandler(extOpHandler);
                } catch (Exception e) {
                    String message = "Failed to load and instantiate ExtendedOperationHandler implementation "
                            + extendedpHandlerBean.getExtendedOpId() + ": " + e.getMessage();
                    LOG.error(message);
                    throw new ConfigurationException(message);
                }
            }
        }

        // ReplReqHandler
        String fqcn = ldapServerBean.getReplReqHandler();

        if (fqcn != null) {
            try {
                Class<?> replProvImplClz = Class.forName(fqcn);
                ReplicationRequestHandler rp = (ReplicationRequestHandler) replProvImplClz.newInstance();
                ldapServer.setReplicationReqHandler(rp);
            } catch (Exception e) {
                String message = "Failed to load and instantiate ReplicationRequestHandler implementation : " + fqcn;
                LOG.error(message);
                throw new ConfigurationException(message);
            }

        }

        ldapServer.setReplConsumers(createReplConsumers(ldapServerBean.getReplConsumers()));

        return ldapServer;
    }

    /**
     * instantiate the ReplicationConsumers based on the configuration present in ReplConsumerBeans
     * 
     * @param replConsumerBeans the list of consumers configured
     * @return a list of ReplicationConsumer instances
     * @throws ConfigurationException
     */
    public static List<ReplicationConsumer> createReplConsumers(List<ReplConsumerBean> replConsumerBeans)
            throws ConfigurationException {
        List<ReplicationConsumer> lst = new ArrayList<ReplicationConsumer>();

        if (replConsumerBeans == null) {
            return lst;
        }

        for (ReplConsumerBean replBean : replConsumerBeans) {
            String className = replBean.getReplConsumerImpl();

            ReplicationConsumer consumer = null;
            Class<?> consumerClass = null;
            SyncreplConfiguration config = null;

            try {
                if (className == null) {
                    consumerClass = ReplicationConsumerImpl.class;
                } else {
                    consumerClass = Class.forName(className);
                }

                consumer = (ReplicationConsumer) consumerClass.newInstance();

                // we don't support any other configuration impls atm, but this configuration should suffice for many needs
                config = new SyncreplConfiguration();

                config.setBaseDn(replBean.getSearchBaseDn());
                config.setRemoteHost(replBean.getReplProvHostName());
                config.setRemotePort(replBean.getReplProvPort());
                config.setAliasDerefMode(AliasDerefMode.getDerefMode(replBean.getReplAliasDerefMode()));
                config.setAttributes(replBean.getReplAttributes().toArray(new String[0]));
                config.setRefreshInterval(replBean.getReplRefreshInterval());
                config.setRefreshNPersist(replBean.isReplRefreshNPersist());

                int scope = SearchScope.getSearchScope(replBean.getReplSearchScope());
                config.setSearchScope(SearchScope.getSearchScope(scope));

                config.setFilter(replBean.getReplSearchFilter());
                config.setSearchTimeout(replBean.getReplSearchTimeOut());
                config.setReplUserDn(replBean.getReplUserDn());
                config.setReplUserPassword(replBean.getReplUserPassword());

                config.setUseTls(replBean.isReplUseTls());
                config.setStrictCertVerification(replBean.isReplStrictCertValidation());

                config.setConfigEntryDn(replBean.getDn());

                if (replBean.getReplPeerCertificate() != null) {
                    ReplicationTrustManager.addCertificate(replBean.getReplConsumerId(), replBean.getReplPeerCertificate());
                }

                consumer.setConfig(config);

                lst.add(consumer);
            } catch (Exception e) {
                throw new ConfigurationException("cannot configure the replication consumer with FQCN " + className, e);
            }
        }

        return lst;
    }

    /**
     * Create a new instance of a JdbmIndex from an instance of JdbmIndexBean
     * 
     * @param JdbmIndexBean The JdbmIndexBean to convert
     * @return An JdbmIndex instance
     * @throws Exception If the instance cannot be created
     */
    public static JdbmIndex<?, Entry> createJdbmIndex(JdbmPartition partition, JdbmIndexBean<String, Entry> jdbmIndexBean,
            DirectoryService directoryService) {
        if ((jdbmIndexBean == null) || jdbmIndexBean.isDisabled()) {
            return null;
        }

        JdbmIndex<String, Entry> index = new JdbmIndex<String, Entry>();

        index.setAttributeId(jdbmIndexBean.getIndexAttributeId());
        index.setCacheSize(jdbmIndexBean.getIndexCacheSize());
        index.setNumDupLimit(jdbmIndexBean.getIndexNumDupLimit());

        String indexFileName = jdbmIndexBean.getIndexFileName();

        if (indexFileName == null) {
            indexFileName = jdbmIndexBean.getIndexAttributeId();
        }

        // Find the OID for this index
        SchemaManager schemaManager = directoryService.getSchemaManager();

        try {
            AttributeType indexAT = schemaManager.lookupAttributeTypeRegistry(indexFileName);
            indexFileName = indexAT.getOid();
        } catch (LdapException le) {
            // Not found ? We will use the index file name
        }

        if (jdbmIndexBean.getIndexWorkingDir() != null) {
            index.setWkDirPath(new File(jdbmIndexBean.getIndexWorkingDir()).toURI());
        } else {
            // Set the Partition working dir as a default
            index.setWkDirPath(partition.getPartitionPath());
        }

        return index;
    }

    /**
     * Create the list of Index from the configuration
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Set<Index<?, Entry, Long>> createJdbmIndexes(JdbmPartition partition, List<IndexBean> indexesBeans,
            DirectoryService directoryService) //throws Exception
    {
        Set<Index<?, Entry, Long>> indexes = new HashSet<Index<?, Entry, Long>>();

        for (IndexBean indexBean : indexesBeans) {
            if (indexBean.isEnabled() && (indexBean instanceof JdbmIndexBean)) {
                indexes.add(createJdbmIndex(partition, (JdbmIndexBean) indexBean, directoryService));
            }
        }

        return indexes;
    }

    /**
     * Create a new instance of a JdbmPartition
     * 
     * @param jdbmPartitionBean the JdbmPartition bean
     * @return The instantiated JdbmPartition
     * @throws LdapInvalidDnException
     * @throws Exception If the instance cannot be created
     */
    public static JdbmPartition createJdbmPartition(DirectoryService directoryService, JdbmPartitionBean jdbmPartitionBean)
            throws ConfigurationException {
        if ((jdbmPartitionBean == null) || jdbmPartitionBean.isDisabled()) {
            return null;
        }

        JdbmPartition jdbmPartition = new JdbmPartition(directoryService.getSchemaManager());

        jdbmPartition.setCacheSize(jdbmPartitionBean.getPartitionCacheSize());
        jdbmPartition.setId(jdbmPartitionBean.getPartitionId());
        jdbmPartition.setOptimizerEnabled(jdbmPartitionBean.isJdbmPartitionOptimizerEnabled());
        File partitionPath = new File(directoryService.getInstanceLayout().getPartitionsDirectory(),
                jdbmPartitionBean.getPartitionId());
        jdbmPartition.setPartitionPath(partitionPath.toURI());

        try {
            jdbmPartition.setSuffixDn(jdbmPartitionBean.getPartitionSuffix());
        } catch (LdapInvalidDnException lide) {
            String message = "Cannot set the Dn " + jdbmPartitionBean.getPartitionSuffix() + ", " + lide.getMessage();
            LOG.error(message);
            throw new ConfigurationException(message);
        }

        jdbmPartition.setSyncOnWrite(jdbmPartitionBean.isPartitionSyncOnWrite());
        jdbmPartition.setIndexedAttributes(createJdbmIndexes(jdbmPartition, jdbmPartitionBean.getIndexes(), directoryService));

        String contextEntry = jdbmPartitionBean.getContextEntry();

        if (contextEntry != null) {
            try {
                // Replace '\n' to real LF
                String entryStr = contextEntry.replaceAll("\\\\n", "\n");

                LdifReader ldifReader = new LdifReader();

                List<LdifEntry> entries = ldifReader.parseLdif(entryStr);

                if ((entries != null) && (entries.size() > 0)) {
                    entries.get(0);
                }
            } catch (LdapLdifException lle) {
                String message = "Cannot parse the context entry : " + contextEntry + ", " + lle.getMessage();
                LOG.error(message);
                throw new ConfigurationException(message);
            }
        }

        return jdbmPartition;
    }

    /**
     * Create the a Partition instantiated from the configuration
     * 
     * @param partitionBean the Partition bean
     * @return The instantiated Partition
     * @throws ConfigurationException If we cannot process the Partition
     */
    public static Partition createPartition(DirectoryService directoryService, PartitionBean partitionBean)
            throws ConfigurationException {
        if ((partitionBean == null) || partitionBean.isDisabled()) {
            return null;
        }

        if (partitionBean instanceof JdbmPartitionBean) {
            return createJdbmPartition(directoryService, (JdbmPartitionBean) partitionBean);
        } else {
            return null;
        }
    }

    /**
     * Create the set of Partitions instantiated from the configuration
     * 
     * @param partitionBeans the list of Partition beans
     * @return A Map of all the instantiated partitions
     * @throws ConfigurationException If we cannot process some Partition
     */
    public static Map<String, Partition> createPartitions(DirectoryService directoryService, List<PartitionBean> partitionBeans)
            throws ConfigurationException {
        Map<String, Partition> partitions = new HashMap<String, Partition>(partitionBeans.size());

        for (PartitionBean partitionBean : partitionBeans) {
            if (partitionBean.isDisabled()) {
                continue;
            }

            Partition partition = createPartition(directoryService, partitionBean);

            if (partition != null) {
                partitions.put(partitionBean.getPartitionId(), partition);
            }
        }

        return partitions;
    }

    /**
     * Instantiates a DirectoryService based on the configuration present in the partition
     * 
     * @param directoryServiceBean The bean containing the configuration
     * @param baseDirectory The working path for this DirectoryService
     * @return An instance of DirectoryService
     * @throws Exception
     */
    public static DirectoryService createDirectoryService(DirectoryServiceBean directoryServiceBean,
            InstanceLayout instanceLayout, SchemaManager schemaManager) throws Exception {
        DirectoryService directoryService = new DefaultDirectoryService();

        // The schemaManager
        directoryService.setSchemaManager(schemaManager);

        // MUST attributes
        // DirectoryService ID
        directoryService.setInstanceId(directoryServiceBean.getDirectoryServiceId());

        // Replica ID
        directoryService.setReplicaId(directoryServiceBean.getDsReplicaId());

        // WorkingDirectory
        directoryService.setInstanceLayout(instanceLayout);

        // Interceptors
        List<Interceptor> interceptors = createInterceptors(directoryServiceBean.getInterceptors());
        directoryService.setInterceptors(interceptors);

        // Partitions
        Map<String, Partition> partitions = createPartitions(directoryService, directoryServiceBean.getPartitions());

        Partition systemPartition = partitions.remove("system");

        if (systemPartition == null) {
            //throw new Exception( I18n.err( I18n.ERR_505 ) );
        }

        directoryService.setSystemPartition(systemPartition);
        directoryService.setPartitions(new HashSet<Partition>(partitions.values()));

        // MAY attributes
        // AccessControlEnabled
        directoryService.setAccessControlEnabled(directoryServiceBean.isDsAccessControlEnabled());

        // AllowAnonymousAccess
        directoryService.setAllowAnonymousAccess(directoryServiceBean.isDsAllowAnonymousAccess());

        // ChangeLog
        ChangeLog cl = createChangeLog(directoryServiceBean.getChangeLog());

        if (cl != null) {
            directoryService.setChangeLog(cl);
        }

        // DenormalizedOpAttrsEnabled
        directoryService.setDenormalizeOpAttrsEnabled(directoryServiceBean.isDsDenormalizeOpAttrsEnabled());

        // Journal
        Journal journal = createJournal(directoryServiceBean.getJournal());

        if (journal != null) {
            directoryService.setJournal(journal);
        }

        // MaxPDUSize
        directoryService.setMaxPDUSize(directoryServiceBean.getDsMaxPDUSize());

        // PasswordHidden
        directoryService.setPasswordHidden(directoryServiceBean.isDsPasswordHidden());

        // SyncPeriodMillis
        directoryService.setSyncPeriodMillis(directoryServiceBean.getDsSyncPeriodMillis());

        // testEntries
        String entryFilePath = directoryServiceBean.getDsTestEntries();

        if (entryFilePath != null) {
            directoryService.setTestEntries(readTestEntries(entryFilePath));
        }

        // Enabled
        if (!directoryServiceBean.isEnabled()) {
            // will only be useful if we ever allow more than one DS to be configured and
            // switch between them
            // decide which one to use based on this flag
            // TODO
        }

        return directoryService;
    }
}