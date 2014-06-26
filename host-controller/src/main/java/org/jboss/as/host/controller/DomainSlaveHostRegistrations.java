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

package org.jboss.as.host.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.as.domain.controller.HostConnectionInfo;
import org.jboss.as.host.controller.mgmt.SlaveHostPinger;

/**
 * @author Emanuel Muckenhuber
 */
public class DomainSlaveHostRegistrations {

    // Keep entries for 1 week by default
    private static final long TTL = TimeUnit.DAYS.toMillis(7);

    private final Map<String, DomainHostConnection> registrations = new ConcurrentHashMap<>();

    protected void registerHost(final String hostName, SlaveHostPinger pinger, String address) {
        synchronized (this) {
            DomainHostConnection registration = registrations.get(hostName);
            final List<HostConnectionInfo.Event> events;
            if (registration == null) {
                events = new ArrayList<>();
            } else {
                events = registration.events;
            }
            events.add(HostConnectionInfo.Events.create(HostConnectionInfo.EventType.REGISTERED, address));
            registration = new DomainHostConnection(hostName, pinger, address, events);
            registrations.put(hostName, registration);
        }
    }

    protected boolean unregisterHost(final String hostName, HostConnectionInfo.Event event) {
        synchronized (this) {
            DomainHostConnection registration = registrations.get(hostName);
            final List<HostConnectionInfo.Event> events;
            if (registration == null) {
                return false;
            } else {
                events = registration.events;
            }
            events.add(event);
            registration = new DomainHostConnection(hostName, events);
            registrations.put(hostName, registration);
        }
        return true;
    }

    protected boolean contains(final String host) {
        return registrations.containsKey(host);
    }

    protected Set<String> getHosts() {
        return registrations.keySet();
    }

    protected DomainHostConnection getRegistration(final String hostName) {
        return registrations.get(hostName);
    }

    protected void addEvent(String hostName, HostConnectionInfo.Event event) {
        synchronized (this) {
            DomainHostConnection registration = registrations.get(hostName);
            if (registration == null) {
                registration = new DomainHostConnection(hostName);
                registrations.put(hostName, registration);
            }
            final List<HostConnectionInfo.Event> events = new ArrayList<>(registration.events);
            events.add(event);
            registration.events = events;
        }
    }

    public void pruneExpired() {
        evictEntries(EXPIRED);
    }

    public void pruneDisconnected() {
        evictEntries(DISCONNECTED);
    }

    protected void evictEntries(EvictionPolicy policy) {
        synchronized (this) {
            final Iterator<DomainHostConnection> i = registrations.values().iterator();
            while (i.hasNext()) {
                final DomainHostConnection registration = i.next();
                if (policy.evictEntry(registration)) {
                    i.remove();
                }
            }
        }
    }

    abstract static class EvictionPolicy {

        abstract boolean evictEntry(DomainHostConnection entry);

    }

    private static final EvictionPolicy EXPIRED = new EvictionPolicy() {

        @Override
        boolean evictEntry(DomainHostConnection entry) {
            final long expired = System.currentTimeMillis() - TTL;
            final List<HostConnectionInfo.Event> events = entry.events;
            final List<HostConnectionInfo.Event> newEvents = new ArrayList<>();
            for (final HostConnectionInfo.Event event : events) {
                if (event.getTimestamp() >= expired) {
                    newEvents.add(event);
                }
            }
            entry.events = newEvents;
            if (! newEvents.isEmpty()) {
                return false;
            } else {
                return ! entry.isConnected();
            }
        }
    };

    private static final EvictionPolicy DISCONNECTED = new EvictionPolicy() {

        @Override
        boolean evictEntry(DomainHostConnection entry) {
            return ! entry.isConnected();
        }
    };

    static class DomainHostConnection implements HostConnectionInfo, Comparable<HostConnectionInfo> {

        private final String hostName;

        private final String address;
        private final SlaveHostPinger pinger;
        private volatile boolean connected;
        private volatile List<Event> events;

        DomainHostConnection(String hostName) {
            this(hostName, new ArrayList<Event>());
        }

        DomainHostConnection(String hostName, List<Event> events) {
            this.hostName = hostName;
            this.connected = false;
            this.address = null;
            this.pinger = null;
            this.events = events;
        }

        DomainHostConnection(String hostName, SlaveHostPinger pinger, String address, List<Event> events) {
            this.hostName = hostName;
            this.pinger = pinger;
            this.events = events;
            this.address = address;
            this.connected = true;
        }

        @Override
        public String getHostName() {
            return hostName;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public List<Event> getEvents() {
            return events;
        }

        protected String getAddress() {
            return address;
        }

        protected Long getRemoteConnectionId() {
            if (pinger != null) {
                return pinger.getRemoteConnectionID();
            } else {
                return null;
            }
        }

        protected SlaveHostPinger getPinger() {
            return pinger;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final DomainHostConnection that = (DomainHostConnection) o;
            if (!hostName.equals(that.hostName)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return hostName.hashCode();
        }

        @Override
        public int compareTo(HostConnectionInfo o) {
            return hostName.compareTo(o.getHostName());
        }
    }
}
