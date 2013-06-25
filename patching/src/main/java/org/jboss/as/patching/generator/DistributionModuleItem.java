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

package org.jboss.as.patching.generator;

/**
 * @author Emanuel Muckenhuber
 */
class DistributionModuleItem implements Comparable<DistributionModuleItem> {

    private final String moduleName;
    private final String slot;
    private final byte[] comparisonHash;
    private final byte[] metadataHash;

    DistributionModuleItem(String moduleName, String slot, byte[] comparisonHash, byte[] metadataHash) {
        this.moduleName = moduleName;
        this.slot = slot;
        this.metadataHash = metadataHash;
        this.comparisonHash = comparisonHash;
    }

    String getName() {
        return moduleName;
    }

    String getSlot() {
        return slot;
    }

    byte[] getMetadataHash() {
        return metadataHash;
    }

    byte[] getComparisonHash() {
        return comparisonHash;
    }

    String getFullModuleName() {
        return moduleName + ":" + slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistributionModuleItem that = (DistributionModuleItem) o;

        if (!moduleName.equals(that.moduleName)) return false;
        return slot.equals(that.slot);

    }

    @Override
    public int hashCode() {
        int result = moduleName.hashCode();
        result = 31 * result + slot.hashCode();
        return result;
    }

    @Override
    public int compareTo(DistributionModuleItem o) {
        return getFullModuleName().compareTo(o.getFullModuleName());
    }
}
