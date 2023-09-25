/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.pool.common;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

/**
 * Comment
 *
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
public class MockBean {
    private static final Logger log = Logger.getLogger(MockBean.class);
    static final AtomicInteger finalized = new AtomicInteger(0);
    static final AtomicInteger preDestroys = new AtomicInteger(0);
    static final AtomicInteger postConstructs= new AtomicInteger(0);

    @Override
    protected void finalize() throws Throwable {
        log.info("finalize");

        finalized.incrementAndGet();

        super.finalize();
    }

    public static int getFinalized() {
        return finalized.get();
    }

    public static int getPreDestroys() {
        return preDestroys.get();
    }

    public static int getPostConstructs() {
        return postConstructs.get();
    }

    public void postConstruct() {
        log.info("postConstruct");
        postConstructs.incrementAndGet();
    }

    public void preDestroy() {
        log.info("preDestroy");
        preDestroys.incrementAndGet();
    }

    public static void reset() {
        finalized.set(0);
        preDestroys.set(0);
        postConstructs.set(0);
    }
}
