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

package org.jboss.as.ejb3.component.stateful.cache;

/**
 * Factory for creating a stateful session bean cache for a component.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 * @param <V> the bean instance type
 */
public interface StatefulSessionBeanCacheFactory<K, V extends StatefulSessionBeanInstance<K>> {

    /**
     * Creates a stateful session bean cache for a given {@link jakarta.ejb.Stateful} EJB.
     * @param configuration configuration of a stateful bean cache
     * @return a cache for a given {@link jakarta.ejb.Stateful} EJB.
     */
    StatefulSessionBeanCache<K, V> createStatefulBeanCache(StatefulSessionBeanCacheConfiguration<K, V> configuration);
}
