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
