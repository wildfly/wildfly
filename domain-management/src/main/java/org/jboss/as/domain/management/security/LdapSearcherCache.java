/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.security;

import java.io.IOException;

import javax.naming.NamingException;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Interface for a LDAP searcher cache, this cache can wrap either user or group searchers.
 *
 * The big difference here is now a {@link SearchResult} is returned which allows the caller to attach to the cached result.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
interface LdapSearcherCache<R, K> {

    /**
     * Perform a search against LDAP.
     *
     * @param connectionHandler - The {@link LdapConnectionHandler} to use to access LDAP.
     * @param key - The base key to use as the search.
     * @return The search result.
     * @throws IOException - If an error occurs communicating with LDAP.
     * @throws NamingException - If an error is encountered searching LDAP.
     */
    SearchResult<R> search(final LdapConnectionHandler connectionHandler, final K key) throws IOException, NamingException;

    int getCurrentSize();

    void clearAll();

    void clear(K key);

    boolean contains(K key);

    void clear(Predicate<K> predicate);

    int count(Predicate<K> predicate);

    interface Predicate<K> {

        boolean matches(final K key);

    }

    interface SearchResult<R> {

        R getResult();

        /**
         * Retrieves an object that has been attached to this search result.
         *
         * @param key the key to the attachment.
         * @param <T> the value type of the attachment.
         *
         * @return the attachment if found otherwise {@code null}.
         */
        <T> T getAttachment(AttachmentKey<T> key);

        /**
         * Attaches an arbitrary object to this context.
         *
         * @param key they attachment key used to ensure uniqueness and used for retrieval of the value.
         * @param value the value to store.
         * @param <T> the value type of the attachment.
         *
         * @return the previous value associated with the key or {@code null} if there was no previous value.
         */
        <T> T attach(AttachmentKey<T> key, T value);

        /**
         * Detaches or removes the value from this context.
         *
         * @param key the key to the attachment.
         * @param <T> the value type of the attachment.
         *
         * @return the attachment if found otherwise {@code null}.
         */
        <T> T detach(AttachmentKey<T> key);

    }

    /**
     * An attachment key instance.
     *
     * Copied directly from {@link OperationContext.AttachmentKey}
     *
     * @param <T> the attachment value type
     */
    static final class AttachmentKey<T> {
        private final Class<T> valueClass;

        /**
         * Construct a new instance.
         *
         * @param valueClass the value type.
         */
        private AttachmentKey(final Class<T> valueClass) {
            this.valueClass = valueClass;
        }

        /**
         * Cast the value to the type of this attachment key.
         *
         * @param value the value
         *
         * @return the cast value
         */
        public T cast(final Object value) {
            return valueClass.cast(value);
        }

        /**
         * Construct a new simple attachment key.
         *
         * @param valueClass the value class
         * @param <T> the attachment type
         *
         * @return the new instance
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T> AttachmentKey<T> create(final Class<? super T> valueClass) {
            return new AttachmentKey(valueClass);
        }
    }

    public static final class ServiceUtil {

        private static final String SERVICE_SUFFIX = "ldap.cache.%s.%s";
        private static final String AUTHENTICATION = "auth";
        private static final String AUTHORIZATION = "authz";
        private static final String USER = "user";
        private static final String GROUP = "group";

        private ServiceUtil() {
        }

        /**
         * Utility method to create the ServiceName for services that provide {@code LdapSearcherCache} instances.
         *
         * @param realmName - The name of the realm the {@code LdapUserSearcher} is associated with.
         * @param forAuthentication - Is this for user loading during authentication or during authorization for user / group loading.
         * @param forUserSearch - Is this for user searching or group loading.
         * @return The constructed ServiceName.
         */
        public static ServiceName createServiceName(final boolean forAuthentication, final boolean forUserSearch, final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(String.format(SERVICE_SUFFIX, forAuthentication ? AUTHENTICATION : AUTHORIZATION, forUserSearch ? USER : GROUP));
        }

        @SuppressWarnings("unchecked")
        public static <R, K> ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Class injectorType,
                Injector<LdapSearcherCache<R, K>> injector, final boolean forAuthentication, final boolean forUserSearch,
                String realmName) {
            sb.addDependency(ServiceBuilder.DependencyType.REQUIRED,
                    createServiceName(forAuthentication, forUserSearch, realmName), injectorType, injector);

            return sb;
        }
    }


}
