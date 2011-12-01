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
        roles.add(new Entry(role, permission));
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
