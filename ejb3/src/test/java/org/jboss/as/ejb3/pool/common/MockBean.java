/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
