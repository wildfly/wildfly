package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public abstract class AbstractSubsystemTransformer implements SubsystemTransformer {

    private final int majorManagementVersion;
    private final int minorManagementVersion;
    private final int microManagementVersion;

    protected AbstractSubsystemTransformer(int majorManagementVersion, int minorManagementVersion, int microManagementVersion) {
        this.majorManagementVersion = majorManagementVersion;
        this.minorManagementVersion = minorManagementVersion;
        this.microManagementVersion = microManagementVersion;
    }

    @Override
    public int getMajorManagementVersion() {
        return majorManagementVersion;
    }

    @Override
    public int getMinorManagementVersion() {
        return minorManagementVersion;
    }

    @Override
    public int getMicroManagementVersion() {
        return microManagementVersion;
    }

    @Override
    public ModelNode transformOperation(TransformationContext context, PathAddress address, ModelNode operation) {
        return operation;
    }
}
