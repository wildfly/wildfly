package org.jboss.as.patching.installation;

/**
 *
 * An identity is a named set of distribution base + layered distribution(s) that is certified as a valid combination.
 *
 * Identity-on target info layout:
 *
 * <pre><code>
 *
 * ${JBOSS_HOME}
 * |-- bundles
 * |   `-- system
 * |       `-- layers
 * |           `-- base => {@link org.jboss.as.patching.DirectoryStructure#getBundleRepositoryRoot()}
 * |               `-- patches
 * |                   `-- &lt;patchId> => {@link org.jboss.as.patching.DirectoryStructure#getBundlesPatchDirectory(String)}
 * |-- modules
 * |   `-- system
 * |       `-- layers
 * |           `-- base => {@link org.jboss.as.patching.DirectoryStructure#getModuleRoot()}
 * |                `-- patches
 * |                   `-- &lt;patchId> => {@link org.jboss.as.patching.DirectoryStructure#getModulePatchDirectory(String)}
 * `-- .installation
 *     `-- identity.conf => {@link org.jboss.as.patching.DirectoryStructure#getInstallationInfo()}
 * <code>
 * </pre>
 *
 * @author Emanuel Muckenhuber
 */
public interface Identity extends PatchableTarget {

    /**
     * Get the identity version.
     *
     * @return the identity version
     */
    String getVersion();

}
