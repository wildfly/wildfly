/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.domain.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class HostRegistryService implements Service<HostRegistryService>, Map<String, DomainControllerSlaveClient>{

    public static ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "host", "registry");

    private final Map<String, DomainControllerSlaveClient> hosts = new ConcurrentHashMap<String, DomainControllerSlaveClient>();

    @Override
    public HostRegistryService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext arg0) throws StartException {
    }

    @Override
    public void stop(StopContext arg0) {
    }

    public void clear() {
        hosts.clear();
    }

    public boolean containsKey(Object key) {
        return hosts.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return hosts.containsValue(value);
    }

    public Set<java.util.Map.Entry<String, DomainControllerSlaveClient>> entrySet() {
        return hosts.entrySet();
    }

    public boolean equals(Object o) {
        return hosts.equals(o);
    }

    public DomainControllerSlaveClient get(Object key) {
        return hosts.get(key);
    }

    public int hashCode() {
        return hosts.hashCode();
    }

    public boolean isEmpty() {
        return hosts.isEmpty();
    }

    public Set<String> keySet() {
        return hosts.keySet();
    }

    public DomainControllerSlaveClient put(String key, DomainControllerSlaveClient value) {
        return hosts.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends DomainControllerSlaveClient> m) {
        hosts.putAll(m);
    }

    public DomainControllerSlaveClient remove(Object key) {
        return hosts.remove(key);
    }

    public int size() {
        return hosts.size();
    }

    public Collection<DomainControllerSlaveClient> values() {
        return hosts.values();
    }
}
