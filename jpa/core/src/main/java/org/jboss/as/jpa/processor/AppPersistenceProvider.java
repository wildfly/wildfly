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

package org.jboss.as.jpa.processor;

import org.jboss.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

/**
 * Application provider persistence provider.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
class AppPersistenceProvider implements PersistenceProvider {
    private static final Logger log = Logger.getLogger(AppPersistenceProvider.class);
    private PersistenceProvider delegate;
    private volatile ProviderUtil providerUtil;

    AppPersistenceProvider(PersistenceProvider delegate) {
        if (delegate == null)
            throw new IllegalArgumentException("Null delegate");
        this.delegate = delegate;
    }

    /**
     * Check if we're actually making a call from this delegate's app.
     *
     * @return true if the invocation comes from this app, false otherwise
     */
    private boolean check() {
        // TODO -- better check; per module CL system
        final SecurityManager sm = System.getSecurityManager();
        if (sm == null)
            return Thread.currentThread().getContextClassLoader().equals(delegate.getClass().getClassLoader());
        else
            return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    return Thread.currentThread().getContextClassLoader().equals(delegate.getClass().getClassLoader());
                }
            });
    }

    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        if (check() == false)
            return null;

        return delegate.createEntityManagerFactory(emName, map);
    }

    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        if (check() == false)
            return null;

        return delegate.createContainerEntityManagerFactory(info, map);
    }

    public ProviderUtil getProviderUtil() {
        if (providerUtil == null) {
            synchronized (this) {
                if (providerUtil == null) {
                    try {
                        providerUtil = delegate.getProviderUtil();
                    } catch (Throwable t) {
                        log.debug("Delegate [" + delegate + "] is probably not implementing JPA2?", t);
                        providerUtil = NOOP;
                    }
                }
            }
        }
        return providerUtil;
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj instanceof AppPersistenceProvider)
            obj = AppPersistenceProvider.class.cast(obj).delegate;

        return delegate.equals(obj);
    }

    private static ProviderUtil NOOP = new ProviderUtil() {
        public LoadState isLoadedWithoutReference(Object entity, String attributeName) {
            return LoadState.UNKNOWN;
        }

        public LoadState isLoadedWithReference(Object entity, String attributeName) {
            return LoadState.UNKNOWN;
        }

        public LoadState isLoaded(Object entity) {
            return LoadState.UNKNOWN;
        }
    };
}
