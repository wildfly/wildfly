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

import org.wildfly.clustering.ejb.Bean;
import org.wildfly.clustering.ejb.RemoveListener;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;

/**
 * Bean remover that removes a bean if and only if it is expired.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 * @param <T> the bean type
 */
public class ExpiredBeanRemover<I, T> implements BeanRemover<I, T> {

    private final BeanFactory<I, T> factory;

    public ExpiredBeanRemover(BeanFactory<I, T> factory) {
        this.factory = factory;
    }

    @Override
    public void remove(I id, RemoveListener<T> listener) {
        BeanEntry<I> entry = this.factory.findValue(id);
        @SuppressWarnings("resource")
        Bean<I, T> bean = (entry != null) ? this.factory.createBean(id, entry) : null;
        if (bean != null) {
            if (bean.isExpired()) {
                InfinispanEjbLogger.ROOT_LOGGER.tracef("Removing expired bean %s", id);
                this.factory.remove(id, listener);
            }
        }
    }
}
