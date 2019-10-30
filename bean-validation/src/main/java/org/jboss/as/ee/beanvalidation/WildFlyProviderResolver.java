/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ee.beanvalidation;

import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link ValidationProviderResolver} to be used within WildFly. If several BV providers are available,
 * {@code HibernateValidator} will be the first element of the returned provider list.
 * <p/>
 * The providers are loaded via the current thread's context class loader; If no providers are found, the loader of this class
 * will be tried as fallback.
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
        List<ValidationProvider<?>> providers = getValidationProviders(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());

        if (providers != null && !providers.isEmpty()) {
            return providers;
        }
        // otherwise use the loader of this class
        else {
            return getValidationProviders(WildFlySecurityManager.getClassLoaderPrivileged(WildFlyProviderResolver.class));
        }
    }

    private List<ValidationProvider<?>> getValidationProviders(final ClassLoader classLoader) {
        if(WildFlySecurityManager.isChecking()) {
            return WildFlySecurityManager.doUnchecked(new PrivilegedAction<List<ValidationProvider<?>>>() {
                @Override
                public List<ValidationProvider<?>> run() {
                    return loadProviders(classLoader);
                }
            });
        } else {
            return loadProviders(classLoader);
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
