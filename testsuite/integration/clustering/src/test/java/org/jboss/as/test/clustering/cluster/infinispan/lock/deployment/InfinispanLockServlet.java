/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.lock.deployment;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.infinispan.lock.EmbeddedClusteredLockManagerFactory;
import org.infinispan.lock.api.ClusteredLock;
import org.infinispan.lock.api.ClusteredLockManager;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Clustered lock servlet exposing define/is locked/lock/unlock operations for a given named lock.
 * Operates on cache manager called 'lock'.
 *
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = { InfinispanLockServlet.SERVLET_PATH })
public class InfinispanLockServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String SERVLET_NAME = "lock";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final long LOCK_TIMEOUT_MS = 5_000;
    public static final String LOCK_NAME_PARAMETER = "lock-name";
    public static final String OPERATION_PARAMETER = "operation";

    public enum LockOperation {
        DEFINE, IS_LOCKED, LOCK, UNLOCK
    }

    @Resource(lookup = "java:jboss/infinispan/container/lock")
    private EmbeddedCacheManager cm;

    private ClusteredLockManager clm;

    public static URI createURI(URL baseURL, String lockName, LockOperation operation) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(lockName).append('&').append(OPERATION_PARAMETER).append('=').append(operation.name()).toString());
    }

    private static StringBuilder buildQuery(String lockName) {
        return new StringBuilder(SERVLET_NAME).append('?').append(LOCK_NAME_PARAMETER).append('=').append(lockName);
    }

    @Override
    public void init() throws ServletException {
        clm = EmbeddedClusteredLockManagerFactory.from(cm);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String lockName = getRequiredParameter(request, LOCK_NAME_PARAMETER);
        String operationName = getRequiredParameter(request, OPERATION_PARAMETER);
        LockOperation operation = LockOperation.valueOf(operationName);

        try {
            switch (operation) {
                case DEFINE: {
                    boolean defined = clm.defineLock(lockName);
                    response.getWriter().print(defined);
                    break;
                }
                case IS_LOCKED: {
                    ClusteredLock lock = clm.get(lockName);
                    CompletableFuture<Boolean> cf = lock.isLocked();
                    Boolean locked = cf.get();
                    response.getWriter().print(locked);
                    break;
                }
                case LOCK: {
                    ClusteredLock lock = clm.get(lockName);
                    CompletableFuture<Boolean> cf = lock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    Boolean acquired = cf.get();
                    response.getWriter().print(acquired);
                    break;
                }
                case UNLOCK: {
                    ClusteredLock lock = clm.get(lockName);
                    CompletableFuture<Void> cf = lock.unlock();
                    cf.get();
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new ServletException(ex);
        }
    }

    private static String getRequiredParameter(HttpServletRequest request, String name) throws ServletException {
        String value = request.getParameter(name);
        if (value == null) {
            throw new ServletException(String.format("No %s specified", name));
        }
        return value;
    }

}
