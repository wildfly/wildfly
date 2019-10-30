/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ldap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.codec.api.LdapApiService;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.Csn;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.LdapComparator;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.NormalizingComparator;
import org.apache.directory.api.ldap.model.schema.registries.ComparatorRegistry;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.loader.JarLdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.ldap.util.tree.DnNode;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.AttributeTypeProvider;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.ObjectClassProvider;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.server.core.api.OperationManager;
import org.apache.directory.server.core.api.ReferralManager;
import org.apache.directory.server.core.api.administrative.AccessControlAdministrativePoint;
import org.apache.directory.server.core.api.administrative.CollectiveAttributeAdministrativePoint;
import org.apache.directory.server.core.api.administrative.SubschemaAdministrativePoint;
import org.apache.directory.server.core.api.administrative.TriggerExecutionAdministrativePoint;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.api.event.EventService;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.journal.Journal;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.api.subtree.SubentryCache;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.server.core.factory.AvlPartitionFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.core.shared.DefaultDnFactory;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for a fast (mostly in-memory-only) ApacheDS DirectoryService. Use only for tests!!
 *
 * @author Josef Cacek
 */
public class InMemoryDirectoryServiceFactory implements DirectoryServiceFactory {

    private static Logger LOG = LoggerFactory.getLogger(InMemoryDirectoryServiceFactory.class);

    private static volatile int counter = 1;

    private final DirectoryService directoryService;
    private final PartitionFactory partitionFactory;
    private CacheManager cacheManager;

    /**
     * Default constructor which creates {@link DefaultDirectoryService} instance and configures {@link AvlPartitionFactory} as
     * the {@link PartitionFactory} implementation.
     */
    public InMemoryDirectoryServiceFactory() {
        try {
            directoryService = new DefaultDirectoryService();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        directoryService.setShutdownHookEnabled(false);
        partitionFactory = new AvlPartitionFactory();
    }

    /**
     * Constructor which uses provided {@link DirectoryService} and {@link PartitionFactory} implementations.
     *
     * @param directoryService must be not-<code>null</code>
     * @param partitionFactory must be not-<code>null</code>
     */
    public InMemoryDirectoryServiceFactory(DirectoryService directoryService, PartitionFactory partitionFactory) {
        this.directoryService = directoryService;
        this.partitionFactory = partitionFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(String name) throws Exception {
        if ((directoryService == null) || directoryService.isStarted()) {
            return;
        }

        int id = counter++;

        directoryService.setInstanceId(name + id);

        // instance layout
        InstanceLayout instanceLayout = new InstanceLayout(System.getProperty("java.io.tmpdir") + "/server-work-" + directoryService.getInstanceId());
        if (instanceLayout.getInstanceDirectory().exists()) {
            try {
                FileUtils.deleteDirectory(instanceLayout.getInstanceDirectory());
            } catch (IOException e) {
                LOG.warn("couldn't delete the instance directory before initializing the DirectoryService", e);
            }
        }
        directoryService.setInstanceLayout(instanceLayout);

        // EhCache in disabled-like-mode
        String cacheName = "ApacheDSTestCache-" +  id;
        Configuration ehCacheConfig = new Configuration();
        ehCacheConfig.setName(cacheName);
        CacheConfiguration defaultCache = new CacheConfiguration(cacheName, 1).eternal(false).timeToIdleSeconds(30)
                .timeToLiveSeconds(30).overflowToDisk(false);
        ehCacheConfig.addDefaultCache(defaultCache);
        cacheManager = new CacheManager(ehCacheConfig);
        CacheService cacheService = new CacheService(cacheManager);

        directoryService.setCacheService(cacheService);

        // Init the schema
        // SchemaLoader loader = new SingleLdifSchemaLoader();
        SchemaLoader loader = new JarLdifSchemaLoader();
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        schemaManager.loadAllEnabled();
        ComparatorRegistry comparatorRegistry = schemaManager.getComparatorRegistry();
        for (LdapComparator<?> comparator : comparatorRegistry) {
            if (comparator instanceof NormalizingComparator) {
                ((NormalizingComparator) comparator).setOnServer();
            }
        }
        directoryService.setSchemaManager(schemaManager);
        InMemorySchemaPartition inMemorySchemaPartition = new InMemorySchemaPartition(schemaManager);

        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(inMemorySchemaPartition);
        directoryService.setSchemaPartition(schemaPartition);
        List<Throwable> errors = schemaManager.getErrors();
        if (errors.size() != 0) {
            throw new Exception(I18n.err(I18n.ERR_317, Exceptions.printErrors(errors)));
        }

        DnFactory dnFactory = new DefaultDnFactory( schemaManager, cacheService.getCache( "dnCache" ) );
        // Init system partition
        Partition systemPartition = partitionFactory.createPartition(directoryService.getSchemaManager(), dnFactory, "system",
                ServerDNConstants.SYSTEM_DN, 500, new File(directoryService.getInstanceLayout().getPartitionsDirectory(),
                        "system"));
        systemPartition.setSchemaManager(directoryService.getSchemaManager());
        partitionFactory.addIndex(systemPartition, SchemaConstants.OBJECT_CLASS_AT, 100);
        directoryService.setSystemPartition(systemPartition);

        directoryService.startup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryService getDirectoryService() throws Exception {
        return cacheManager != null ? new WrapperDirectoryService(directoryService, cacheManager) : directoryService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionFactory getPartitionFactory() throws Exception {
        return partitionFactory;
    }

    /**
     * Delegating DirectoryService which ensure cacheManager shutdown on DirectoryService shutdown.
     */
    private class WrapperDirectoryService implements DirectoryService {

        private final DirectoryService wrapped;
        private final CacheManager cacheManager;

        private WrapperDirectoryService(DirectoryService wrapped, CacheManager cacheManager) {
            this.wrapped = wrapped;
            this.cacheManager = cacheManager;
        }

        @Override
        public Entry newEntry(Dn dn) throws LdapException {
            return wrapped.newEntry(dn);
        }

        @Override
        public long revert(long revision) throws LdapException {
            return wrapped.revert(revision);
        }

        @Override
        public long revert() throws LdapException {
            return wrapped.revert();
        }

        @Override
        public PartitionNexus getPartitionNexus() {
            return wrapped.getPartitionNexus();
        }

        @Override
        public void addPartition(Partition partition) throws Exception {
            wrapped.addPartition(partition);
        }

        @Override
        public void removePartition(Partition partition) throws Exception {
            wrapped.removePartition(partition);
        }

        @Override
        public SchemaManager getSchemaManager() {
            return wrapped.getSchemaManager();
        }

        @Override
        public LdapApiService getLdapCodecService() {
            return wrapped.getLdapCodecService();
        }

        @Override
        public ReferralManager getReferralManager() {
            return wrapped.getReferralManager();
        }

        @Override
        public void setReferralManager(ReferralManager referralManager) {
            wrapped.setReferralManager(referralManager);
        }

        @Override
        public SchemaPartition getSchemaPartition() {
            return wrapped.getSchemaPartition();
        }

        @Override
        public void setSchemaPartition(SchemaPartition schemaPartition) {
            wrapped.setSchemaPartition(schemaPartition);
        }

        @Override
        public EventService getEventService() {
            return wrapped.getEventService();
        }

        @Override
        public void setEventService(EventService eventService) {
            wrapped.setEventService(eventService);
        }

        @Override
        public void startup() throws Exception {
            wrapped.startup();
        }

        @Override
        public void shutdown() throws Exception {
            wrapped.shutdown();
            cacheManager.shutdown();
        }

        @Override
        public void sync() throws Exception {
            wrapped.sync();
        }

        @Override
        public boolean isStarted() {
            return wrapped.isStarted();
        }

        @Override
        public CoreSession getAdminSession() {
            return wrapped.getAdminSession();
        }

        @Override
        public SubentryCache getSubentryCache() {
            return wrapped.getSubentryCache();
        }

        @Override
        public SubtreeEvaluator getEvaluator() {
            return wrapped.getEvaluator();
        }

        @Override
        public CoreSession getSession() throws Exception {
            return wrapped.getSession();
        }

        @Override
        public CoreSession getSession(LdapPrincipal principal) throws Exception {
            return wrapped.getSession(principal);
        }

        @Override
        public CoreSession getSession(Dn principalDn, byte[] credentials) throws LdapException {
            return wrapped.getSession(principalDn, credentials);
        }

        @Override
        public CoreSession getSession(Dn principalDn, byte[] credentials, String saslMechanism, String saslAuthId) throws Exception {
            return wrapped.getSession(principalDn, credentials, saslMechanism, saslAuthId);
        }

        @Override
        public void setInstanceId(String instanceId) {
            wrapped.setInstanceId(instanceId);
        }

        @Override
        public String getInstanceId() {
            return wrapped.getInstanceId();
        }

        @Override
        public Set<? extends Partition> getPartitions() {
            return wrapped.getPartitions();
        }

        @Override
        public void setPartitions(Set<? extends Partition> partitions) {
            wrapped.setPartitions(partitions);
        }

        @Override
        public boolean isAccessControlEnabled() {
            return wrapped.isAccessControlEnabled();
        }

        @Override
        public void setAccessControlEnabled(boolean accessControlEnabled) {
            wrapped.setAccessControlEnabled(accessControlEnabled);
        }

        @Override
        public boolean isAllowAnonymousAccess() {
            return wrapped.isAllowAnonymousAccess();
        }

        @Override
        public boolean isPasswordHidden() {
            return wrapped.isPasswordHidden();
        }

        @Override
        public void setPasswordHidden(boolean passwordHidden) {
            wrapped.setPasswordHidden(passwordHidden);
        }

        @Override
        public void setAllowAnonymousAccess(boolean enableAnonymousAccess) {
            wrapped.setAllowAnonymousAccess(enableAnonymousAccess);
        }

        @Override
        public List<Interceptor> getInterceptors() {
            return wrapped.getInterceptors();
        }

        @Override
        public List<String> getInterceptors(OperationEnum operation) {
            return wrapped.getInterceptors(operation);
        }

        @Override
        public void setInterceptors(List<Interceptor> interceptors) {
            wrapped.setInterceptors(interceptors);
        }

        @Override
        public void addFirst(Interceptor interceptor) throws LdapException {
            wrapped.addFirst(interceptor);
        }

        @Override
        public void addLast(Interceptor interceptor) throws LdapException {
            wrapped.addLast(interceptor);
        }

        @Override
        public void addAfter(String interceptorName, Interceptor interceptor) {
            wrapped.addAfter(interceptorName, interceptor);
        }

        @Override
        public void remove(String interceptorName) {
            wrapped.remove(interceptorName);
        }

        @Override
        public void setJournal(Journal journal) {
            wrapped.setJournal(journal);
        }

        @Override
        public List<LdifEntry> getTestEntries() {
            return wrapped.getTestEntries();
        }

        @Override
        public void setTestEntries(List<? extends LdifEntry> testEntries) {
            wrapped.setTestEntries(testEntries);
        }

        @Override
        public InstanceLayout getInstanceLayout() {
            return wrapped.getInstanceLayout();
        }

        @Override
        public void setInstanceLayout(InstanceLayout instanceLayout) throws IOException {
            wrapped.setInstanceLayout(instanceLayout);
        }

        @Override
        public void setShutdownHookEnabled(boolean shutdownHookEnabled) {
            wrapped.setShutdownHookEnabled(shutdownHookEnabled);
        }

        @Override
        public boolean isShutdownHookEnabled() {
            return wrapped.isShutdownHookEnabled();
        }

        @Override
        public void setExitVmOnShutdown(boolean exitVmOnShutdown) {
            wrapped.setExitVmOnShutdown(exitVmOnShutdown);
        }

        @Override
        public boolean isExitVmOnShutdown() {
            return wrapped.isExitVmOnShutdown();
        }


        @Override
        public void setSystemPartition(Partition systemPartition) {
            wrapped.setSystemPartition(systemPartition);
        }

        @Override
        public Partition getSystemPartition() {
            return wrapped.getSystemPartition();
        }

        @Override
        public boolean isDenormalizeOpAttrsEnabled() {
            return wrapped.isDenormalizeOpAttrsEnabled();
        }

        @Override
        public void setDenormalizeOpAttrsEnabled(boolean denormalizeOpAttrsEnabled) {
            wrapped.setDenormalizeOpAttrsEnabled(denormalizeOpAttrsEnabled);
        }

        @Override
        public ChangeLog getChangeLog() {
            return wrapped.getChangeLog();
        }

        @Override
        public Journal getJournal() {
            return wrapped.getJournal();
        }

        @Override
        public void setChangeLog(ChangeLog changeLog) {
            wrapped.setChangeLog(changeLog);
        }

        @Override
        public Entry newEntry(String ldif, String dn) {
            return wrapped.newEntry(ldif, dn);
        }

        @Override
        public OperationManager getOperationManager() {
            return wrapped.getOperationManager();
        }

        @Override
        public int getMaxPDUSize() {
            return wrapped.getMaxPDUSize();
        }

        @Override
        public void setMaxPDUSize(int maxPDUSize) {
            wrapped.setMaxPDUSize(maxPDUSize);
        }

        @Override
        public Interceptor getInterceptor(String interceptorName) {
            return wrapped.getInterceptor(interceptorName);
        }

        @Override
        public Csn getCSN() {
            return wrapped.getCSN();
        }

        @Override
        public int getReplicaId() {
            return wrapped.getReplicaId();
        }

        @Override
        public void setReplicaId(int replicaId) {
            wrapped.setReplicaId(replicaId);
        }

        @Override
        public void setSchemaManager(SchemaManager schemaManager) {
            wrapped.setSchemaManager(schemaManager);
        }

        @Override
        public void setSyncPeriodMillis(long syncPeriodMillis) {
            wrapped.setSyncPeriodMillis(syncPeriodMillis);
        }

        @Override
        public long getSyncPeriodMillis() {
            return wrapped.getSyncPeriodMillis();
        }

        @Override
        public CacheService getCacheService() {
            return wrapped.getCacheService();
        }

        @Override
        public DnNode<AccessControlAdministrativePoint> getAccessControlAPCache() {
            return wrapped.getAccessControlAPCache();
        }

        @Override
        public DnNode<CollectiveAttributeAdministrativePoint> getCollectiveAttributeAPCache() {
            return wrapped.getCollectiveAttributeAPCache();
        }

        @Override
        public DnNode<SubschemaAdministrativePoint> getSubschemaAPCache() {
            return wrapped.getSubschemaAPCache();
        }

        @Override
        public DnNode<TriggerExecutionAdministrativePoint> getTriggerExecutionAPCache() {
            return wrapped.getTriggerExecutionAPCache();
        }

        @Override
        public boolean isPwdPolicyEnabled() {
            return wrapped.isPwdPolicyEnabled();
        }

        @Override
        public DnFactory getDnFactory() {
            return wrapped.getDnFactory();
        }

        @Override
        public void setCacheService(CacheService cacheService) {
            wrapped.setCacheService(cacheService);
        }

        @Override
        public AttributeTypeProvider getAtProvider() {
            return wrapped.getAtProvider();
        }

        @Override
        public ObjectClassProvider getOcProvider() {
            return wrapped.getOcProvider();
        }

        @Override
        public void setDnFactory(DnFactory dnFactory) {
            wrapped.setDnFactory(dnFactory);
        }

    }

}
