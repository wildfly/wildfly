/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.cache.bean;

import java.util.Map;

import org.wildfly.clustering.ee.Creator;
import org.wildfly.clustering.ee.MutatorFactory;
import org.wildfly.clustering.ee.Remover;
import org.wildfly.clustering.ee.cache.CacheProperties;
import org.wildfly.clustering.ejb.bean.BeanInstance;
import org.wildfly.clustering.marshalling.spi.MarshalledValue;
import org.wildfly.clustering.marshalling.spi.MarshalledValueFactory;

/**
 * Encapsulates the configuration of a {@link DefaultBeanGroupManager}.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 * @param <C> the marshalled value context type
 */
public interface DefaultBeanGroupManagerConfiguration<K, V extends BeanInstance<K>, C> {
    Creator<K, MarshalledValue<Map<K, V>, C>, MarshalledValue<Map<K, V>, C>> getCreator();
    Remover<K> getRemover();
    MutatorFactory<K, MarshalledValue<Map<K, V>, C>> getMutatorFactory();
    CacheProperties getCacheProperties();
    MarshalledValueFactory<C> getMarshalledValueFactory();
}
