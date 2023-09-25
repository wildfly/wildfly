/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jpa.validator;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import jakarta.validation.ValidationProviderResolver;
import jakarta.validation.spi.ValidationProvider;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link ValidationProviderResolver} to be used within WildFly. If several BV providers are available,
 * {@code HibernateValidator} will be the first element of the returned provider list.
 * <p/>
 * The providers are loaded via the current thread's context class loader; If no providers are found, the loader of this class
 * will be tried as fallback.
 * </p>
 * Note: This class is a copy of {@code org.jboss.as.ee.beanvalidation.WildFlyProviderResolver}.
 *
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class WildFlyProviderResolver implements ValidationProviderResolver {

    /**
     * Returns a list with all {@link ValidationProvider} validation providers.
     *
     * @return a list with all {@link ValidationProvider} validation providers
     */
    @Override
    public List<ValidationProvider<?>> getValidationProviders() {
        // first try the TCCL
        List<ValidationProvider<?>> providers = loadProviders(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());

        if (providers != null && !providers.isEmpty()) {
            return providers;
        }
        // otherwise use the loader of this class
        else {
            return loadProviders(WildFlySecurityManager.getClassLoaderPrivileged(WildFlyProviderResolver.class));
        }
    }

    /**
     * Retrieves the providers from the given loader, using the service loader mechanism.
     *
     * @param classLoader the class loader to use
     * @return a list with providers retrieved via the given loader. May be empty but never {@code null}
     */
    private List<ValidationProvider<?>> loadProviders(ClassLoader classLoader) {
        @SuppressWarnings("rawtypes")
        Iterator<ValidationProvider> providerIterator = ServiceLoader.load(ValidationProvider.class, classLoader).iterator();
        LinkedList<ValidationProvider<?>> providers = new LinkedList<ValidationProvider<?>>();

        while (providerIterator.hasNext()) {
            try {
                ValidationProvider<?> provider = providerIterator.next();

                // put Hibernate Validator to the beginning of the list
                if (provider.getClass().getName().equals("org.hibernate.validator.HibernateValidator")) {
                    providers.addFirst(provider);
                } else {
                    providers.add(provider);
                }
            } catch (ServiceConfigurationError e) {
                // ignore, because it can happen when multiple
                // providers are present and some of them are not class loader
                // compatible with our API.
            }
        }

        return providers;
    }
}
