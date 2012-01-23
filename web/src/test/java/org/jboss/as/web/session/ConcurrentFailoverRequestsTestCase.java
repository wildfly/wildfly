/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009 Red Hat, Inc. and individual contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Manager;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.web.session.mocks.BasicRequestHandler;
import org.jboss.as.web.session.mocks.ConcurrentRequestHandler;
import org.jboss.as.web.session.mocks.SetAttributesRequestHandler;
import org.jboss.logging.Logger;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * JBAS-7379. Tests that multiple concurrent failover requests for the same session are handled properly.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 85945 $
 */
public class ConcurrentFailoverRequestsTestCase {
    private static final Logger log = Logger.getLogger(ConcurrentFailoverRequestsTestCase.class);

    private static long testCount = System.currentTimeMillis();

    private final JGroupsSystemPropertySupport jgSupport = new JGroupsSystemPropertySupport();
    private final EmbeddedCacheManager[] cacheContainers = new EmbeddedCacheManager[2];
    private final DistributableSessionManager<?>[] managers = new DistributableSessionManager[cacheContainers.length];

    private ExecutorService threadPool;

    @Before
    public void setUp() throws Exception {
        jgSupport.setUpProperties();
    }

    @After
    public void tearDown() throws Exception {
        jgSupport.restoreProperties();

        if (threadPool != null) {
            threadPool.shutdownNow();
        }

        for (DistributableSessionManager<?> manager : managers) {
            if (manager != null) {
                try {
                    manager.stop();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        for (CacheContainer cacheContainer : cacheContainers) {
            if (cacheContainer != null) {
                try {
                    cacheContainer.stop();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Test
    public void testConcurrentFailoverRequests() throws Exception {
        log.info("++++ Starting testConcurrentFailoverRequests ++++");

        ++testCount;

        log.info("Starting cache containers");

        String warName = "test" + testCount;
        JBossWebMetaData webMetaData = SessionTestUtil.createWebMetaData(100);
        for (int i = 0; i < cacheContainers.length; ++i) {
            cacheContainers[i] = SessionTestUtil.createCacheContainer(false, null, false, false);
            cacheContainers[i].start();
        }
        /*
         * log.info("Blocking until all views are received");
         *
         * SessionTestUtil.blockUntilViewsReceived(cacheContainers, 10000);
         *
         * log.info("All views received");
         */
        log.info("Starting managers");
        EmbeddedCacheManager[] containers = new EmbeddedCacheManager[cacheContainers.length];

        for (int i = 0; i < cacheContainers.length; ++i) {
            managers[i] = SessionTestUtil.createManager(webMetaData, warName, 30, cacheContainers[i], null);
            managers[i].start();

            containers[i] = (EmbeddedCacheManager) cacheContainers[i].getCache().getCacheManager();
        }

        log.info("Blocking until all views are received");

        SessionTestUtil.blockUntilViewsReceived(containers, 10000);

        log.info("All views received");

        Object value = "0";
        Map<String, Object> attrs = Collections.unmodifiableMap(Collections.singletonMap("count", value));
        SetAttributesRequestHandler setHandler = new SetAttributesRequestHandler(attrs, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        String id1 = setHandler.getSessionId();
        assertNotNull(id1);

        // Add a second session that we can check for replication; this is a
        // proxy
        // for checking that first session has replicated
        setHandler = new SetAttributesRequestHandler(attrs, false);
        SessionTestUtil.invokeRequest(managers[0], setHandler, null);

        String id2 = setHandler.getSessionId();
        assertNotNull(id1);

        assertFalse(id1.equals(id2));

        // Ensure replication of session 2 has occurred
        boolean found = false;
        for (int i = 0; i < 10; i++) {
            BasicRequestHandler getHandler = new BasicRequestHandler(attrs.keySet(), false);
            SessionTestUtil.invokeRequest(managers[1], getHandler, id2);
            if (getHandler.getCheckedAttributes() != null && value.equals(getHandler.getCheckedAttributes().get("count"))) {
                found = true;
                break;
            }
            Thread.sleep(50);
        }
        assertTrue("sessions replicated", found);

        managers[0].stop();

        int THREADS = 10;
        threadPool = Executors.newFixedThreadPool(THREADS);

        CountDownLatch startingGun = new CountDownLatch(THREADS + 1);
        CountDownLatch finishedSignal = new CountDownLatch(THREADS);
        ConcurrentRequestHandler concurrentHandler = new ConcurrentRequestHandler();
        Valve pipelineHead = SessionTestUtil.setupPipeline(managers[1], concurrentHandler);
        Loader[] loaders = new Loader[THREADS];

        for (int i = 0; i < loaders.length; i++) {
            loaders[i] = new Loader(pipelineHead, concurrentHandler, managers[1], id1, attrs.keySet(), startingGun, finishedSignal);
            threadPool.execute(loaders[i]);
        }

        startingGun.countDown();

        assertTrue("loaders completed on time", finishedSignal.await(45, TimeUnit.SECONDS));

        for (int i = 0; i < loaders.length; i++) {
            assertNotNull("got checked attributes for " + i, loaders[i].checkedAttributes);
            assertTrue("checked 'count' attribute for " + i, loaders[i].checkedAttributes.containsKey("count"));
            assertEquals("correct value for " + i, value, loaders[i].checkedAttributes.get("count"));
        }

        managers[1].stop();
    }

    private static class Loader implements Runnable {
        private final Valve pipelineHead;
        private final ConcurrentRequestHandler concurrentHandler;
        private final Manager manager;
        private final String sessionId;
        private final Set<String> attributeKeys;
        private final CountDownLatch startingGun;
        private final CountDownLatch finishedSignal;

        private Map<String, Object> checkedAttributes;

        private Loader(Valve pipelineHead, ConcurrentRequestHandler concurrentHandler, Manager manager, String sessionId,
                Set<String> attributeKeys, CountDownLatch startingGun, CountDownLatch finishedSignal) {
            this.pipelineHead = pipelineHead;
            this.concurrentHandler = concurrentHandler;
            this.manager = manager;
            this.sessionId = sessionId;
            this.attributeKeys = attributeKeys;
            this.startingGun = startingGun;
            this.finishedSignal = finishedSignal;
        }

        @Override
        public void run() {
            try {
                BasicRequestHandler getHandler = new BasicRequestHandler(attributeKeys, false);
                concurrentHandler.registerHandler(getHandler);
                Request request = SessionTestUtil.setupRequest(manager, sessionId);
                startingGun.countDown();
                startingGun.await();

                SessionTestUtil.invokeRequest(pipelineHead, request);
                this.checkedAttributes = getHandler.getCheckedAttributes();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                finishedSignal.countDown();
                concurrentHandler.unregisterHandler();
            }
        }
    }
}
