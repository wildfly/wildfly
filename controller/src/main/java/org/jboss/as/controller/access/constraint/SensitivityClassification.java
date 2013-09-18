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

package org.jboss.as.controller.access.constraint;


/**
 * Classification to apply to resources, attributes or operation to allow configuration
 * of whether access, reads or writes are sensitive.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class SensitivityClassification extends AbstractSensitivity {

    public static final SensitivityClassification ACCESS_CONTROL = new SensitivityClassification("access-control", true, true, true);
    public static final SensitivityClassification CREDENTIAL = new SensitivityClassification("credential", false, true, true);
    public static final SensitivityClassification DOMAIN_CONTROLLER = new SensitivityClassification("domain-controller", false, false, true);
    public static final SensitivityClassification DOMAIN_NAMES = new SensitivityClassification("domain-names", false, false, true);
    public static final SensitivityClassification EXTENSIONS = new SensitivityClassification("extensions", false, false, true);
    public static final SensitivityClassification JVM = new SensitivityClassification("jvm", false, false, true);
    public static final SensitivityClassification MANAGEMENT_INTERFACES = new SensitivityClassification("management-interfaces", false, false, true);
    public static final SensitivityClassification MODULE_LOADING = new SensitivityClassification("module-loading", false, false, true);
    public static final SensitivityClassification PATCHING = new SensitivityClassification("patching", false, false, true);
    public static final SensitivityClassification READ_WHOLE_CONFIG = new SensitivityClassification("read-whole-config", false, true, true);
    public static final SensitivityClassification SECURITY_REALM = new SensitivityClassification("security-realm", true, true, true);
    public static final SensitivityClassification SECURITY_REALM_REF = new SensitivityClassification("security-realm-ref", true, true, true);
    public static final SensitivityClassification SECURITY_DOMAIN = new SensitivityClassification("security-domain", true, true, true);
    public static final SensitivityClassification SECURITY_DOMAIN_REF = new SensitivityClassification("security-domain-ref", true, true, true);
    public static final SensitivityClassification SECURITY_VAULT = new SensitivityClassification("security-vault", false, true, true);
    public static final SensitivityClassification SERVICE_CONTAINER = new SensitivityClassification("service-container", false, false, true);
    public static final SensitivityClassification SOCKET_BINDING_REF = new SensitivityClassification("socket-binding-ref", false, false, false);
    public static final SensitivityClassification SOCKET_CONFIG = new SensitivityClassification("socket-config", false, false, true);
    public static final SensitivityClassification SNAPSHOTS = new SensitivityClassification("snapshots", false, false, false);
    public static final SensitivityClassification SYSTEM_PROPERTY = new SensitivityClassification("system-property", false, false, true);

    private final boolean core;
    private final String subsystem;
    private final String name;

    private SensitivityClassification(String name, boolean accessDefault, boolean readDefault, boolean writeDefault) {
        super(accessDefault, readDefault, writeDefault);
        this.core = true;
        this.subsystem = null;
        this.name = name;
    }

    public SensitivityClassification(String subsystem, String name, boolean accessDefault, boolean readDefault, boolean writeDefault) {
        super(accessDefault, readDefault, writeDefault);
        assert subsystem != null : "subsystem is null";
        assert name != null : "name is null";
        this.core = false;
        this.subsystem = subsystem;
        this.name = name;
    }

    public boolean isCore() {
        return core;
    }

    public String getSubsystem() {
        return subsystem;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SensitivityClassification that = (SensitivityClassification) o;

        return core == that.core && name.equals(that.name)
                && !(subsystem != null ? !subsystem.equals(that.subsystem) : that.subsystem != null);

    }

    @Override
    public int hashCode() {
        int result = (core ? 1 : 0);
        result = 31 * result + (subsystem != null ? subsystem.hashCode() : 0);
        result = 31 * result + name.hashCode();
        return result;
    }

    Key getKey() {
        return new Key();
    }

    class Key {

        private final SensitivityClassification sensitivity = SensitivityClassification.this;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key thatKey = (Key) o;
            SensitivityClassification that = thatKey.sensitivity;

            return core == that.core && name.equals(that.name)
                    && !(subsystem != null ? !subsystem.equals(that.subsystem) : that.subsystem != null);

        }

        @Override
        public int hashCode() {
            int result = (core ? 1 : 0);
            result = 31 * result + (subsystem != null ? subsystem.hashCode() : 0);
            result = 31 * result + name.hashCode();
            return result;
        }

    }
}
