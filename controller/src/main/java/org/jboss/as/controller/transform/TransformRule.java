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
