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
package org.jboss.as.ejb3.cache;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;

public abstract class CacheFactoryBuilderService<K, V extends Identifiable<K>> implements Value<CacheFactoryBuilder<K, V>> {

    public static final ServiceName BASE_CACHE_SERVICE_NAME = ServiceName.JBOSS.append("ejb", "cache");
    public static final ServiceName DEFAULT_CACHE_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("sfsb-default");
    public static final ServiceName DEFAULT_PASSIVATION_DISABLED_CACHE_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("sfsb-default-passivation-disabled");

    public static final ServiceName BASE_CACHE_FACTORY_SERVICE_NAME = BASE_CACHE_SERVICE_NAME.append("factory");

    public static ServiceName getServiceName(String name) {
        return BASE_CACHE_FACTORY_SERVICE_NAME.append(name);
    }

    private final String name;

    protected CacheFactoryBuilderService(String name) {
        this.name = name;
    }

    public ServiceBuilder<CacheFactoryBuilder<K, V>> build(ServiceTarget target) {
        return target.addService(getServiceName(this.name), new ValueService<>(this));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CacheFactoryBuilderService)) return false;
        return this.name.equals(((CacheFactoryBuilderService<?, ?>) object).name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
