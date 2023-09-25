/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.counter.deployment;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.infinispan.counter.EmbeddedCounterManagerFactory;
import org.infinispan.counter.api.CounterConfiguration;
import org.infinispan.counter.api.CounterManager;
import org.infinispan.counter.api.CounterType;
import org.infinispan.counter.api.Storage;
import org.infinispan.counter.api.StrongCounter;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

/**
 * @author Radoslav Husar
 */
@WebServlet(urlPatterns = { InfinispanCounterServlet.SERVLET_PATH })
public class InfinispanCounterServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(InfinispanCounterServlet.class);

    private static final long serialVersionUID = 1L;
    private static final String SERVLET_NAME = "counter";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;
    public static final String COUNTER_NAME_PARAMETER = "counter-name";
    public static final String STORAGE_PARAMETER = "define";

    @Resource(lookup = "java:jboss/infinispan/container/counter")
    private EmbeddedCacheManager ecm;

    private CounterManager cm;

    public static URI createURI(URL baseURL, String counterName) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(counterName).toString());
    }

    public static URI createURI(URL baseURL, String counterName, String storage) throws URISyntaxException {
        return baseURL.toURI().resolve(buildQuery(counterName).append('&').append(STORAGE_PARAMETER).append('=').append(storage).toString());
    }

    private static StringBuilder buildQuery(String counterName) {
        return new StringBuilder(SERVLET_NAME).append('?').append(COUNTER_NAME_PARAMETER).append('=').append(counterName);
    }

    @Override
    public void init() throws ServletException {
        cm = EmbeddedCounterManagerFactory.asCounterManager(ecm);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String counterName = getRequiredParameter(request, COUNTER_NAME_PARAMETER);
        String storage = request.getParameter(STORAGE_PARAMETER);

        if (storage != null) {
            CounterConfiguration cc = CounterConfiguration.builder(CounterType.UNBOUNDED_STRONG)
                    .storage(Storage.valueOf(storage))
                    .build();

            cm.defineCounter(counterName, cc);
        }

        StrongCounter strongCounter = cm.getStrongCounter(counterName);

        CompletableFuture<Long> longCompletableFuture = strongCounter.addAndGet(1);
        try {
            Long count = longCompletableFuture.get();
            log.infof("Counter %s returned %s", counterName, count);
            response.getWriter().print(count);
        } catch (InterruptedException | ExecutionException e) {
            throw new ServletException(e);
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
