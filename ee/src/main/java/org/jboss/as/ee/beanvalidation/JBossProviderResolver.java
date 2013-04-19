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

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.WeakHashMap;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

import org.hibernate.validator.HibernateValidator;

/**
 * A {@link ValidationProviderResolver} to be used within JBoss AS. If several BV providers are available,
 * {@code HibernateValidator} will be the first element of the returned provider list.
 * <p/>
 * The providers are loaded via the current thread's context class loader; If no providers are found, the loader of this class
 * will be tried as fallback.
 *
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 */
public class JBossProviderResolver implements ValidationProviderResolver {

    /**
     * Cache the found providers per class loader. Keep them in a weak hash map to avoid memory leaks and allow proper hot
     * re-deployment.
     */
    private static final WeakHashMap<ClassLoader, SoftReference<List<ValidationProvider<?>>>> providersPerClassLoader = new WeakHashMap<ClassLoader, SoftReference<List<ValidationProvider<?>>>>();

    /**
     * Returns a list with all {@link ValidationProvider} validation providers.
     *
     * @return a list with all {@link ValidationProvider} validation providers
     */
    @Override
    public List<ValidationProvider<?>> getValidationProviders() {
        // first try the TCCL
        List<ValidationProvider<?>> providers = getProviders(SecurityActions.getContextClassLoader());

        if (providers != null && !providers.isEmpty()) {
            return providers;
        }
        // otherwise use the loader of this class
        else {
            return getProviders(SecurityActions.getClassLoader(JBossProviderResolver.class));
        }
    }

    /**
     * Retrieves the providers from the given loader. Will be retrieved from the cache if possible, otherwise the providers are
     * loaded via the service loader.
     *
     * @param classLoader the class loader to use
     * @return a list with providers retrieved via the given loader. May be empty but never {@code null}
     */
    private List<ValidationProvider<?>> getProviders(ClassLoader classLoader) {
        List<ValidationProvider<?>> providers = getCachedValidationProviders(classLoader);
        if (providers != null) {
            return providers;
        }

        providers = loadProviders(classLoader);
        cacheValidationProviders(classLoader, providers);

        return providers;
    }

    /**
     * Loads the providers from the given loader, using the Java service loader.
     *
     * @param classLoader the class loader to use
     * @return a list with providers retrieved via the given loader. May be empty but never {@code null}
     */
    private List<ValidationProvider<?>> loadProviders(ClassLoader classLoader) {
        @SuppressWarnings("rawtypes")
        Iterator<ValidationProvider> providerIterator = ServiceLoader.load(ValidationProvider.class, classLoader).iterator();
        LinkedList<ValidationProvider<?>> providerList = new LinkedList<ValidationProvider<?>>();
        while (providerIterator.hasNext()) {
            try {
                ValidationProvider<?> provider = providerIterator.next();

                // put Hibernate Validator to the beginning of the list
                if (provider.getClass().getName().equals(HibernateValidator.class.getName())) {
                    providerList.addFirst(provider);
                } else {
                    providerList.add(provider);
                }
            } catch (ServiceConfigurationError e) {
                // ignore, because it can happen when multiple
                // providers are present and some of them are not class loader
                // compatible with our API.
            }
        }

        return providerList;
    }

    /**
     * Retrieves the providers for the given class loader.
     *
     * @param classLoader the class loader to use
     * @return A list with providers cached for the given loader, may be {@code null}
     */
    private synchronized List<ValidationProvider<?>> getCachedValidationProviders(ClassLoader classLoader) {
        SoftReference<List<ValidationProvider<?>>> ref = providersPerClassLoader.get(classLoader);
        return ref != null ? ref.get() : null;
    }

    /**
     * Caches the given providers against the given class loader.
     *
     * @param classLoader the class loader used to load the given provides
     * @param providers the providers to cache
     */
    private synchronized void cacheValidationProviders(ClassLoader classLoader, List<ValidationProvider<?>> providers) {
        providersPerClassLoader.put(classLoader, new SoftReference<List<ValidationProvider<?>>>(providers));
    }
}
