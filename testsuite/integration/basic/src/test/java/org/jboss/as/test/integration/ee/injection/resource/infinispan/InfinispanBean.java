/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.resource.infinispan;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.junit.Assert;

/**
 * @author Paul Ferraro
 */
@ManagedBean("infinispan")
public class InfinispanBean {
    static final String CONTAINER_REF_NAME = "mycontainer";
    private static final String CONTAINER_JNDI_NAME = "java:comp/env/" + CONTAINER_REF_NAME;

    // This is gets injected via web.xml
    private CacheContainer container;
    private Cache<Integer, Object> cache;

    @PostConstruct
    public void start() {
        this.cache = this.container.getCache();
        this.cache.start();
    }

    @PreDestroy
    public void stop() {
        this.cache.stop();
    }

    public void test() {
        try {
            // Test simple value
            this.cache.put(1, "test");
            // Test custom type
            this.cache.put(2, new Bean());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            // Make sure we can also perform a vanilla java:comp/env jndi lookup of container
            Object result = new InitialContext().lookup(CONTAINER_JNDI_NAME);
            Assert.assertEquals(this.container, result);
        } catch (NamingException e) {
            Assert.fail(e.getMessage());
        }

    }

    public static class Bean implements java.io.Serializable {
        private static final long serialVersionUID = -7265704761812104791L;
    }
}