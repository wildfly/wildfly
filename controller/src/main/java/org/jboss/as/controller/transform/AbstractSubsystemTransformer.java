package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public abstract class AbstractSubsystemTransformer implements SubsystemTransformer {
    private int majorManagementVersion;
    private int minorManagementVersion;

    protected AbstractSubsystemTransformer(int majorManagementVersion, int minorManagementVersion) {
        this.majorManagementVersion = majorManagementVersion;
        this.minorManagementVersion = minorManagementVersion;
    }

    public int getMajorManagementVersion() {
        return majorManagementVersion;
    }

    public int getMinorManagementVersion() {
        return minorManagementVersion;
    }

    @Override
    public ModelNode transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
        return operation;
    }
}
