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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
class TransformRule {
    private PathAddress sourceAddress;
    private PathAddress targetAddress;
    private String sourceName;
    private String targetName;

    public TransformRule() {
    }

    public TransformRule(String sourceName, String targetName) {
        this.sourceName = sourceName;
        this.targetName = targetName;
    }

    public TransformRule(PathAddress sourceAddress, PathAddress targetAddress) {
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
    }

    public TransformRule(PathAddress sourceAddress, PathAddress targetAddress, String sourceName, String targetName) {
        this.sourceAddress = sourceAddress;
        this.targetAddress = targetAddress;
        this.sourceName = sourceName;
        this.targetName = targetName;
    }

    public PathAddress getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(PathAddress sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public PathAddress getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(PathAddress targetAddress) {
        this.targetAddress = targetAddress;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public boolean isMappingSame() {
        return sourceAddress.equals(targetAddress) && sourceName.equals(targetName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        TransformRule that = (TransformRule) o;

        if (sourceAddress != null ? !sourceAddress.equals(that.sourceAddress) : that.sourceAddress != null) { return false; }
        if (sourceName != null ? !sourceName.equals(that.sourceName) : that.sourceName != null) { return false; }
        if (targetAddress != null ? !targetAddress.equals(that.targetAddress) : that.targetAddress != null) { return false; }
        if (targetName != null ? !targetName.equals(that.targetName) : that.targetName != null) { return false; }

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourceAddress != null ? sourceAddress.hashCode() : 0;
        result = 31 * result + (targetAddress != null ? targetAddress.hashCode() : 0);
        result = 31 * result + (sourceName != null ? sourceName.hashCode() : 0);
        result = 31 * result + (targetName != null ? targetName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "path: " + sourceAddress + " => " + targetAddress + ", name: " + sourceName + " => " + targetName;

    }
}
