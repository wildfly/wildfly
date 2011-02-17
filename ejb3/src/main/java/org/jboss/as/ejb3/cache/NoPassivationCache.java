/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.ejb3.cache.spi.Cache;
import org.jboss.as.ejb3.cache.spi.Identifiable;
import org.jboss.as.ejb3.cache.spi.StatefulObjectFactory;

import javax.ejb.NoSuchEJBException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class NoPassivationCache<T extends Identifiable> implements Cache<T> {
    private StatefulObjectFactory<T> factory;
    private Map<Serializable, T> cacheMap = new ConcurrentHashMap<Serializable, T>();

    @Override
    public T create() {
        T instance = factory.createInstance();
        cacheMap.put(instance.getId(), instance);
        return instance;
    }

    @Override
    public void discard(Serializable key) {
        T instance = cacheMap.remove(key);
        // Note that this is not needed by spec
        if (instance != null)
            factory.destroyInstance(instance);
    }

    @Override
    public T get(Serializable key) throws NoSuchEJBException {
        T instance = cacheMap.get(key);
        if(instance == null)
            throw new NoSuchEJBException("Could not find stateful bean " + key);
        return instance;
    }

    @Override
    public void release(T obj) {
        // TODO: a lot
    }

    @Override
    public void setStatefulObjectFactory(StatefulObjectFactory<T> factory) {
        this.factory = factory;
    }
}
