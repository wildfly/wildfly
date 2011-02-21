/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.server.deployment.module;

/**
 * Information about a deployed extension
 *
 * @author Stuart Douglas
 *
 */
public class ExtensionInfo {
    private final String name;
    private final String specVersion;
    private final String implVersion;
    private final String implVendorId;

    public ExtensionInfo(String name, String specVersion, String implVersion, String implVendorId) {
        this.name = name;
        this.specVersion = specVersion;
        this.implVersion = implVersion;
        this.implVendorId = implVendorId;
    }

    public String getName() {
        return name;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getImplVersion() {
        return implVersion;
    }

    public String getImplVendorId() {
        return implVendorId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((implVendorId == null) ? 0 : implVendorId.hashCode());
        result = prime * result + ((implVersion == null) ? 0 : implVersion.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((specVersion == null) ? 0 : specVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtensionInfo other = (ExtensionInfo) obj;
        if (implVendorId == null) {
            if (other.implVendorId != null)
                return false;
        } else if (!implVendorId.equals(other.implVendorId))
            return false;
        if (implVersion == null) {
            if (other.implVersion != null)
                return false;
        } else if (!implVersion.equals(other.implVersion))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (specVersion == null) {
            if (other.specVersion != null)
                return false;
        } else if (!specVersion.equals(other.specVersion))
            return false;
        return true;
    }

}
