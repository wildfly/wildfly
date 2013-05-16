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
 * of whether they are related to "applications".
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ApplicationTypeConfig {

    private final boolean core;

    private final String subsystem;
    private final String name;
    private final boolean defaultValue;
    private volatile Boolean configuredValue;

    public ApplicationTypeConfig(String subsystem, String name, boolean defaultValue) {
        assert subsystem != null : "subsystem is null";
        assert name != null : "name is null";
        this.subsystem = subsystem;
        this.name = name;
        this.defaultValue = defaultValue;
        this.core = false;
    }

    private ApplicationTypeConfig(String name, boolean defaultValue) {
        this.core = true;
        this.subsystem = null;
        this.name = name;
        this.defaultValue = defaultValue;
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

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public Boolean getConfiguredValue() {
        return configuredValue;
    }

    public boolean isApplicationType() {
        final Boolean app = configuredValue;
        return app == null ? defaultValue : app;
    }

    void setConfiguredValue(boolean configuredValue) {
        this.configuredValue = configuredValue;
    }

    Key getKey() {
        return new Key();
    }

    boolean isCompatibleWith(ApplicationTypeConfig other) {
        return !equals(other) || defaultValue == other.defaultValue;
    }

    class Key {

        private final ApplicationTypeConfig typeConfig = ApplicationTypeConfig.this;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key thatKey = (Key) o;
            ApplicationTypeConfig that = thatKey.typeConfig;

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
