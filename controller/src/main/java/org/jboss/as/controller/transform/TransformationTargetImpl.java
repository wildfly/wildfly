package org.jboss.as.controller.transform;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.extension.SubsystemInformation;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class TransformationTargetImpl implements TransformationTarget {
    private static final Logger log = Logger.getLogger(TransformationTarget.class);
    private final int major;
    private final int minor;
    private final ExtensionRegistry extensionRegistry;
    private final TransformerRegistry transformerRegistry;
    private final Map<String, String> subsystemVersions = Collections.synchronizedMap(new HashMap<String, String>());

    private TransformationTargetImpl(final int majorManagementVersion, final int minorManagementVersion, final ModelNode subsystemVersions) {
        this.major = majorManagementVersion;
        this.minor = minorManagementVersion;
        this.transformerRegistry = TransformerRegistry.getInstance();
        this.extensionRegistry = transformerRegistry.getExtensionRegistry();
        for (Property p : subsystemVersions.asPropertyList()) {
            this.subsystemVersions.put(p.getName(), p.getValue().asString());
        }
    }

    public static TransformationTargetImpl create(final int majorManagementVersion, final int minorManagementVersion, final ModelNode subsystemVersions) {
        return new TransformationTargetImpl(majorManagementVersion, minorManagementVersion, subsystemVersions);
    }

    @Override
    public String getManagementVersion() {
        return major + "." + minor;
    }

    @Override
    public int getMajorManagementVersion() {
        return major;
    }

    @Override
    public int getMinorManagementVersion() {
        return minor;
    }

    @Override
    public String getSubsystemVersion(String subsystemName) {
        return subsystemVersions.get(subsystemName);
    }

    @Override
    public SubsystemInformation getSubsystemInformation(String subsystemName) {
        return extensionRegistry.getSubsystemInfo(subsystemName);
    }

    @Override
    public OperationTransformer resolveTransformer(PathAddress address, String operationName) {
        return null;
    }

    @Override
    public SubsystemTransformer getSubsystemTransformer(String subsystemName) {
        if (!subsystemVersions.containsKey(subsystemName)) {
            return null;
        }
        SubsystemInformation info = getSubsystemInformation(subsystemName);

        String[] version = getSubsystemVersion(subsystemName).split("\\.");
        int major = Integer.parseInt(version[0]);
        int minor = Integer.parseInt(version[1]);

        if (info.getManagementInterfaceMajorVersion() == major && info.getManagementInterfaceMinorVersion() == minor) {
            return null; //no need to transform
        }
        SubsystemTransformer t = transformerRegistry.getSubsystemTransformer(subsystemName, major, minor);
        if (t == null) {
            log.warn("We have no transformer for subsystem: " + subsystemName + "-" + major + "." + minor + " model transfer can break!");
            //return defaultSubsystemTransformer?
        }
        return t;
    }

    public boolean isTransformationNeeded() {
        //return  !(major==org.jboss.as.version.Version.MANAGEMENT_MAJOR_VERSION&& minor == org.jboss.as.version.Version.MANAGEMENT_MINOR_VERSION); //todo dependencies issue
        return !(major == 1 && minor == 2);
    }

    @Override
    public boolean addSubsystemVersion(String subsystemName, int majorVersion, int minorVersion) {
        StringBuilder sb = new StringBuilder(String.valueOf(majorVersion)).append('.').append(minorVersion);
        return this.subsystemVersions.put(subsystemName, sb.toString()) == null;
    }
}
