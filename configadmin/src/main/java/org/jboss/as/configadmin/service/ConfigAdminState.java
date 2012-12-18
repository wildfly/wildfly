/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.configadmin.service;

import static org.jboss.as.configadmin.ConfigAdminLogger.LOGGER;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.osgi.spi.util.UnmodifiableDictionary;

/**
 * Maintains a set of {@link Dictionary}s keyed be persistent ID (PID).
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Oct-2012
 */
class ConfigAdminState {

    static final String TRANSIENT_PROPERTY_SERIAL_ID = ".transient.serial.id";

    private final Map<String, Dictionary<String, String>> configurations = new LinkedHashMap<String, Dictionary<String, String>>();
    private final Map<String, AtomicLong> serialids = new HashMap<String, AtomicLong>();

    synchronized Set<String> keySet() {
        return configurations.keySet();
    }

    synchronized Dictionary<String, String> get(String pid) {
        return configurations.get(pid);
    }

    synchronized boolean remove(String pid) {
        AtomicLong serialId = getSerialId(pid);

        Dictionary<String, String> previous = configurations.get(pid);
        if (previous == null) {
            LOGGER.debugf("Config removed: %s", pid);
            return true;
        }

        serialId.getAndIncrement();

        LOGGER.debugf("Config remove: %s", pid);
        configurations.remove(pid);
        return true;
    }

    synchronized boolean put(String pid, Dictionary<String, String> source, boolean rollback) {
        AtomicLong serialId = getSerialId(pid);

        // Asign a serialid to the source if we don't already have one
        String sourceid = source.get(TRANSIENT_PROPERTY_SERIAL_ID);
        if (sourceid == null) {
            sourceid = new Long(serialId.incrementAndGet()).toString();
            source.put(TRANSIENT_PROPERTY_SERIAL_ID, sourceid);
        }

        // Ignore the serialid on rollback
        if (rollback) {
            LOGGER.debugf("Config rollback: %s => %s", pid, source);
            serialId.set(Long.parseLong(sourceid));
            configurations.put(pid, source);
            return true;
        }

        // Update if there is no previous config for this pid
        Dictionary<String, String> previous = configurations.get(pid);
        if (previous == null) {
            if (Long.parseLong(sourceid) == serialId.get()) {
                LOGGER.debugf("Config put: %s => %s", pid, source);
                configurations.put(pid, new UnmodifiableDictionary<String, String>(source));
                return true;
            } else {
                LOGGER.debugf("Config skip put: %s => %s", pid, source);
                return false;
            }
        }

        // Do not allow an older configuration to update a newer one
        String previd = previous.get(TRANSIENT_PROPERTY_SERIAL_ID);
        if (Long.parseLong(previd) <= Long.parseLong(sourceid)) {
            LOGGER.debugf("Config put: %s => %s", pid, source);
            configurations.put(pid, new UnmodifiableDictionary<String, String>(source));
            return true;
        } else {
            LOGGER.debugf("Config skip put: %s => %s", pid, source);
            return false;
        }
    }

    private AtomicLong getSerialId(String pid) {
        AtomicLong serialid = serialids.get(pid);
        if (serialid == null) {
            serialids.put(pid, serialid = new AtomicLong());
        }
        return serialid;
    }
}