/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class that stores the JACC config for an EJB before being consumed by the JACC service.
 *
 * @author Stuart Douglas
 * @see EjbJaccService
 */
public class EjbJaccConfig {

    private final List<Map.Entry<String, Permission>> roles = new ArrayList<Map.Entry<String, Permission>>();
    private final List<Permission> permit = new ArrayList<Permission>();
    private final List<Permission> deny = new ArrayList<Permission>();

    public void addRole(String role, Permission permission) {
        roles.add(new Entry<>(role, permission));
    }
    public List<Map.Entry<String, Permission>> getRoles() {
        return roles;
    }

    public void addPermit(Permission permission) {
        permit.add(permission);
    }

    public List<Permission> getPermit() {
        return permit;
    }

    public void addDeny(Permission permission) {
        deny.add(permission);
    }

    public List<Permission> getDeny() {
        return deny;
    }

    private static final class Entry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;

        public Entry(final K key, final V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public V setValue(final V value) {
            return null;
        }
    }
}
