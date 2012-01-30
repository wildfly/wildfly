/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.FileCacheStoreConfigurationBuilder.FsyncMode;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.remoting.transport.jgroups.JGroupsChannelLookup;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.subsystem.CacheAdd;
import org.jboss.as.clustering.jgroups.MuxChannel;
import org.jboss.as.clustering.lock.SharedLocalYieldingClusterLockManager;
import org.jboss.as.clustering.registry.Registry;
import org.jboss.as.clustering.registry.RegistryService;
import org.jboss.as.clustering.web.ClusteringNotSupportedException;
import org.jboss.as.clustering.web.OutgoingDistributableSessionData;
import org.jboss.as.clustering.web.infinispan.DistributedCacheManagerFactory;
import org.jboss.as.web.session.mocks.BasicRequestHandler;
import org.jboss.as.web.session.mocks.MockEngine;
import org.jboss.as.web.session.mocks.MockHost;
import org.jboss.as.web.session.mocks.MockRequest;
import org.jboss.as.web.session.mocks.MockValve;
import org.jboss.as.web.session.mocks.RequestHandler;
import org.jboss.as.web.session.mocks.RequestHandlerValve;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.jboss.PassivationConfig;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.metadata.web.jboss.ReplicationGranularity;
import org.jboss.metadata.web.jboss.ReplicationTrigger;
import org.jboss.metadata.web.jboss.SnapshotMode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jgroups.Channel;
import org.jgroups.conf.XmlConfigurator;

/**
 * Utilities for session testing.
 *
 * @author <a href="mailto://brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 109938 $
 */
public class SessionTestUtil {
    public static final String CACHE_CONFIG_PROP = "jbosstest.cluster.web.cache.config";

    private static final Logger log = Logger.getLogger(SessionTestUtil.class);

    public static class ExtendedCacheManager extends DefaultEmbeddedCacheManager {
        private final CoreGroupCommunicationService service;
        private final SharedLocalYieldingClusterLockManager lockManager;

        ExtendedCacheManager(EmbeddedCacheManager container) {
            super(container, CacheContainer.DEFAULT_CACHE_NAME);
            Transport transport = container.getCache().getCacheManager().getTransport();
            if (transport != null) {
                Channel channel = ((org.infinispan.remoting.transport.jgroups.JGroupsTransport) transport).getChannel();
                this.service = new CoreGroupCommunicationService(Integer.valueOf(0).shortValue());
                service.setChannel(channel);
                this.lockManager = new SharedLocalYieldingClusterLockManager("lock", service, service);
            } else {
                this.service = null;
                this.lockManager = null;
            }
        }
        
        SharedLocalYieldingClusterLockManager getLockManager() {
            return this.lockManager;
        }
        
        @Override
        public void start() {
            super.start();
            if (this.lockManager != null) {
                try {
                    this.service.start();
                    this.lockManager.start();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        @Override
        public void stop() {
            if (this.lockManager != null) {
                try {
                    this.lockManager.stop();
                    this.service.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException(e);
                }
            }
            super.stop();
        }
    }

    public static class ChannelLookup implements JGroupsChannelLookup {
        @Override
        public Channel getJGroupsChannel(Properties properties) {
            try {
                Channel channel = new MuxChannel(XmlConfigurator.getInstance(Thread.currentThread().getContextClassLoader().getResource("jgroups-udp.xml")));
                return channel;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean shouldStartAndConnect() {
            return true;
        }

        @Override
        public boolean shouldStopAndDisconnect() {
            return true;
        }
    }

    public static DistributableSessionManager<?> createManager(JBossWebMetaData metaData, String warName, int maxInactiveInterval, final EmbeddedCacheManager cacheContainer, final String jvmRoute) {
        final Cache<?, ?> jvmRouteCache = cacheContainer.getCache();
        Registry.RegistryEntryProvider<String, Void> provider = new Registry.RegistryEntryProvider<String, Void>() {
            @Override
            public String getKey() {
                return jvmRoute;
            }
            @Override
            public Void getValue() {
                return null;
            }
        };
        final RegistryService<String, Void> registry = new RegistryService<String, Void>(provider);
        registry.getCacheInjector().inject(jvmRouteCache);
        DistributedCacheManagerFactory factory = new DistributedCacheManagerFactory();
        factory.getCacheContainerInjector().inject(cacheContainer);
        factory.getCacheConfigurationInjector().inject(jvmRouteCache.getCacheConfiguration());
        factory.getRegistryInjector().inject(registry);
        factory.getLockManagerInjector().inject(((ExtendedCacheManager) cacheContainer).getLockManager());
        
        Engine engine = new MockEngine();
        engine.setName("jboss.web");
        engine.setJvmRoute(jvmRoute);
        Host host = new MockHost();
        host.setName("localhost");
        engine.addChild(host);
        StandardContext context = new StandardContext();
        context.setName(warName);
        context.setDomain(jvmRoute);
        host.addChild(context);

        try {
            DistributableSessionManager<OutgoingDistributableSessionData> manager = new DistributableSessionManager<OutgoingDistributableSessionData>(factory, context, metaData) {
                @Override
                public void start() throws LifecycleException {
                    try {
                        jvmRouteCache.start();
                        registry.start(mock(StartContext.class));
                        super.start();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void stop() throws LifecycleException {
                    try {
                        super.stop();
                        if (jvmRouteCache.getStatus().allowInvocations()) {
                            registry.stop(mock(StopContext.class));
                            jvmRouteCache.stop();
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

            context.setManager(manager);

            // Do this after assigning the manager to the container, or else
            // the container's setting will override ours
            // Can't just set the container as their config is per minute not per second
            manager.setMaxInactiveInterval(maxInactiveInterval);
            return manager;
        } catch (ClusteringNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static volatile int containerIndex = 1;

    public static EmbeddedCacheManager createCacheContainer(boolean local, String passivationDir, boolean totalReplication, boolean purgeCacheLoader) throws Exception {
        CacheMode mode = local ? CacheMode.LOCAL : (totalReplication ? CacheMode.REPL_SYNC : CacheMode.DIST_SYNC);
        GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder();
        String name = "container" + containerIndex++;
        globalBuilder.transport()
                .transport(local ? null : new JGroupsTransport())
                .addProperty(JGroupsTransport.CHANNEL_LOOKUP, ChannelLookup.class.getName())
                .distributedSyncTimeout(60000)
                .clusterName("test")
                .globalJmxStatistics().enable().cacheManagerName(name).allowDuplicateDomains(true)
        ;
        ConfigurationBuilder builder = new ConfigurationBuilder().read(CacheAdd.getDefaultConfiguration(mode));
        builder.transaction()
                .syncCommitPhase(true)
                .syncRollbackPhase(true)
                .transactionMode(TransactionMode.TRANSACTIONAL)
                .transactionManagerLookup(new TransactionManagerProvider(BatchModeTransactionManager.getInstance()))
                .invocationBatching().enable()
                .jmxStatistics().enable()
        ;
        if (passivationDir != null) {
            builder.loaders()
                    .passivation(true)
                    .preload(false)
//                    .preload(!purgeCacheLoader)
                    .addFileCacheStore()
                            .location(passivationDir)
                            .fsyncMode(FsyncMode.PER_WRITE)
                            .fetchPersistentState(mode.isReplicated())
                            .purgeOnStartup(purgeCacheLoader)
                            .purgeSynchronously(true)
            ;
        }

        try {
            return new ExtendedCacheManager(new DefaultEmbeddedCacheManager(globalBuilder.build(), builder.build(), CacheContainer.DEFAULT_CACHE_NAME));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static JBossWebMetaData createWebMetaData(int maxSessions) {
        return createWebMetaData(ReplicationGranularity.SESSION, ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET, maxSessions, false, -1, -1, false, 0);
    }

    public static JBossWebMetaData createWebMetaData(int maxSessions, boolean passivation, int maxIdle, int minIdle) {
        return createWebMetaData(ReplicationGranularity.SESSION, ReplicationTrigger.SET_AND_NON_PRIMITIVE_GET, maxSessions, passivation, maxIdle, minIdle, false, 60);
    }

    public static JBossWebMetaData createWebMetaData(ReplicationGranularity granularity, ReplicationTrigger trigger, boolean batchMode, int maxUnreplicated) {
        return createWebMetaData(granularity, trigger, -1, false, -1, -1, batchMode, maxUnreplicated);
    }

    public static JBossWebMetaData createWebMetaData(ReplicationGranularity granularity, ReplicationTrigger trigger, int maxSessions, boolean passivation, int maxIdle, int minIdle, boolean batchMode, int maxUnreplicated) {
        JBossWebMetaData webMetaData = new JBossWebMetaData();
        webMetaData.setDistributable(new EmptyMetaData());
        webMetaData.setMaxActiveSessions(new Integer(maxSessions));
        PassivationConfig pcfg = new PassivationConfig();
        pcfg.setUseSessionPassivation(Boolean.valueOf(passivation));
        pcfg.setPassivationMaxIdleTime(new Integer(maxIdle));
        pcfg.setPassivationMinIdleTime(new Integer(minIdle));
        webMetaData.setPassivationConfig(pcfg);
        ReplicationConfig repCfg = new ReplicationConfig();
        repCfg.setReplicationGranularity(granularity);
        repCfg.setReplicationTrigger(trigger);
        repCfg.setReplicationFieldBatchMode(Boolean.valueOf(batchMode));
        repCfg.setMaxUnreplicatedInterval(Integer.valueOf(maxUnreplicated));
        repCfg.setSnapshotMode(SnapshotMode.INSTANT);
        webMetaData.setReplicationConfig(repCfg);

        return webMetaData;
    }

    public static void invokeRequest(Manager manager, RequestHandler handler, String sessionId) throws ServletException, IOException {
        Valve valve = setupPipeline(manager, handler);
        Request request = setupRequest(manager, sessionId);
        invokeRequest(valve, request);
    }

    public static void invokeRequest(Valve pipelineHead, Request request) throws ServletException, IOException {
        pipelineHead.invoke(request, request.getResponse());
        // StandardHostValve calls request.getSession(false) on way out, so we will too
        request.getSession(false);
        request.recycle();
    }

    public static Valve setupPipeline(Manager manager, RequestHandler requestHandler) {
        Pipeline pipeline = manager.getContainer().getPipeline();

        // Clean out any existing request handler
        Valve[] valves = pipeline.getValves();
        RequestHandlerValve mockValve = null;
        for (Valve valve: valves) {
            if (valve instanceof RequestHandlerValve) {
                mockValve = (RequestHandlerValve) valve;
                break;
            }
        }

        if (mockValve == null) {
            mockValve = new RequestHandlerValve(requestHandler);
            pipeline.addValve(mockValve);
        } else {
            mockValve.setRequestHandler(requestHandler);
        }
        return pipeline.getFirst();
    }

    public static Request setupRequest(Manager manager, String sessionId) {
        MockRequest request = new MockRequest(manager);
        request.setRequestedSessionId(sessionId);
        Response response = new Response();
        try {
            response.setConnector(new Connector("http"));
            response.setCoyoteResponse(new org.apache.coyote.Response());
//            org.apache.coyote.Response coyoteResponse = new org.apache.coyote.Response();
//            coyoteResponse.setOutputBuffer(new InternalOutputBuffer(coyoteResponse));
//            response.setCoyoteResponse(coyoteResponse);
//            response.setConnector(new Connector("http"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        request.setResponse(response);
        return request;
    }

    public static void cleanupPipeline(Manager manager) {
        Pipeline pipeline = manager.getContainer().getPipeline();

        Valve[] valves = pipeline.getValves();
        for (Valve valve : valves) {
            if (valve instanceof MockValve) {
                ((MockValve) valve).clear();
            }
        }
    }

    /**
     * Loops, continually calling {@link #areCacheViewsComplete(TestDistributedCacheManagerFactory[])} until it either returns
     * true or <code>timeout</code> ms have elapsed.
     *
     * @param dcmFactories caches which must all have consistent views
     * @param timeout max number of ms to loop
     * @throws RuntimeException if <code>timeout</code> ms have elapse without all caches having the same number of members.
     */
    public static void blockUntilViewsReceived(EmbeddedCacheManager[] cacheContainers, long timeout) {
        long failTime = System.currentTimeMillis() + timeout;

        while (System.currentTimeMillis() < failTime) {
            sleepThread(100);
            if (areCacheViewsComplete(cacheContainers)) {
                return;
            }
        }

        throw new RuntimeException("timed out before caches had complete views");
    }

    /**
     * Checks each cache to see if the number of elements in the array returned by
     * {@link TestDistributedCacheFactory#getMembers()} matches the size of the <code>dcmFactories</code> parameter.
     *
     * @param dcmFactories factory whose caches should form a View
     * @return <code>true</code> if all caches have <code>factories.length</code> members; false otherwise
     * @throws IllegalStateException if any of the caches have MORE view members than factories.length
     */
    public static boolean areCacheViewsComplete(EmbeddedCacheManager[] cacheContainers) {
        return areCacheViewsComplete(cacheContainers, true);
    }

    public static boolean areCacheViewsComplete(EmbeddedCacheManager[] cacheContainers, boolean barfIfTooManyMembers) {
        for (EmbeddedCacheManager cacheContainer : cacheContainers) {
            log.info("Cache container " + cacheContainer.getClusterName() + " members: " + cacheContainer.getMembers());
            if (!isCacheViewComplete(cacheContainer, cacheContainers.length, barfIfTooManyMembers)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isCacheViewComplete(EmbeddedCacheManager cacheContainer, int memberCount, boolean barfIfTooManyMembers) {
        List<Address> members = cacheContainer.getMembers();
        if (members == null || memberCount > members.size()) {
            return false;
        } else if (memberCount < members.size()) {
            if (barfIfTooManyMembers) {
                // This is an exceptional condition
                StringBuffer sb = new StringBuffer("Cache at address ");
                sb.append(cacheContainer.getAddress());
                sb.append(" had ");
                sb.append(members.size());
                sb.append(" members; expecting ");
                sb.append(memberCount);
                sb.append(". Members were (");
                for (int j = 0; j < members.size(); j++) {
                    if (j > 0) {
                        sb.append(", ");
                    }
                    sb.append(members.get(j));
                }
                sb.append(')');

                throw new IllegalStateException(sb.toString());
            }

            return false;
        }

        return true;
    }

    /*
     * public static Cookie getSessionCookie(HttpClient client) { // Get the state for the JSESSIONID HttpState state =
     * client.getState(); // Get the JSESSIONID so we can reset the host Cookie[] cookies = state.getCookies(); Cookie sessionID
     * = null; for (int c = 0; c < cookies.length; c++) { Cookie k = cookies[c]; if (k.getName().equalsIgnoreCase("JSESSIONID"))
     * { sessionID = k; } } return sessionID; }
     *
     * public static void setCookieDomainToThisServer(HttpClient client, String server) { // Get the session cookie Cookie
     * sessionID = getSessionCookie(client); if (sessionID == null) { throw new
     * IllegalStateException("No session cookie found on " + client); } // Reset the domain so that the cookie will be sent to
     * server1 sessionID.setDomain(server); client.getState().addCookie(sessionID); }
     */
    public static void validateExpectedAttributes(Map<String, Object> expected, BasicRequestHandler handler) {
        assertFalse(handler.isNewSession());

        if (handler.isCheckAttributeNames()) {
            assertEquals(expected.size(), handler.getAttributeNames().size());
        }
        Map<String, Object> checked = handler.getCheckedAttributes();
        assertEquals(expected.size(), checked.size());
        for (Map.Entry<String, Object> entry : checked.entrySet()) {
            assertEquals(entry.getKey(), expected.get(entry.getKey()), entry.getValue());
        }
    }

    public static void validateNewSession(BasicRequestHandler handler) {
        assertTrue(handler.isNewSession());
        assertEquals(handler.getCreationTime(), handler.getLastAccessedTime());
        if (handler.isCheckAttributeNames()) {
            assertEquals(0, handler.getAttributeNames().size());
        }
        Map<String, Object> checked = handler.getCheckedAttributes();
        for (Map.Entry<String, Object> entry : checked.entrySet()) {
            assertNull(entry.getKey(), entry.getValue());
        }
    }

    public static String getPassivationDir(String rootDir, long testCount, int cacheCount) {
        File dir = new File(rootDir);
        dir = new File(dir, String.valueOf(testCount));
        dir.mkdirs();
        dir.deleteOnExit();
        dir = new File(dir, String.valueOf(cacheCount));
        dir.mkdirs();
        dir.deleteOnExit();
        return dir.getAbsolutePath();
    }

    public static void cleanFilesystem(String path) {
        if (path != null) {
            File f = new File(path);
            if (f.exists()) {
                if (f.isDirectory()) {
                    File[] children = f.listFiles();
                    for (File child : children) {
                        cleanFilesystem(child.getAbsolutePath());
                    }
                }
                f.delete();
            }
        }
    }

    /**
     * Puts the current thread to sleep for the desired number of ms, suppressing any exceptions.
     *
     * @param sleeptime number of ms to sleep
     */
    public static void sleepThread(long sleeptime) {
        try {
            Thread.sleep(sleeptime);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public static Object getAttributeValue(int value) {
        return Integer.valueOf(value);
    }

    private SessionTestUtil() {
    }
}
