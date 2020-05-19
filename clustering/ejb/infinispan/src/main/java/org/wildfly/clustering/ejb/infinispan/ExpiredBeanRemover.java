/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;

/**
 * Bean remover that removes a bean if and only if it is expired.
 *
 * @author Paul Ferraro
 *
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class ExpiredBeanRemover<I, T> implements BeanRemover<I, T> {

    private final BeanFactory<I, T> factory;
    private final ExpirationConfiguration<T> expiration;

    public ExpiredBeanRemover(BeanFactory<I, T> factory, ExpirationConfiguration<T> expiration) {
        this.factory = factory;
        this.expiration = expiration;
    }

    @Override
    public boolean remove(I id, RemoveListener<T> listener) {
        BeanEntry<I> entry = this.factory.tryValue(id);
        if (entry != null) {
            if (entry.isExpired(this.expiration.getTimeout())) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Removing expired bean %s", id);
                return this.factory.remove(id, listener);
            }
            return false;
        }
        return true;
    }
}
