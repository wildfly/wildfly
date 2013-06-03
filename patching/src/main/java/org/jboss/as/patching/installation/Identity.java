package org.jboss.as.patching.installation;

import org.jboss.as.patching.DirectoryStructure;

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
 * |           `-- base => {@link DirectoryStructure#getBundleRepositoryRoot()}
 * |               `-- patches
 * |                   `-- &lt;patchId> => {@link DirectoryStructure#getBundlesPatchDirectory(String)}
 * |-- modules
 * |   `-- system
 * |       `-- layers
 * |           `-- base => {@link DirectoryStructure#getModuleRoot()}
 * |                `-- patches
 * |                   `-- &lt;patchId> => {@link DirectoryStructure#getModulePatchDirectory(String)}
 * `-- .installation
 *     |-- cumulative
 *     |-- references
 *     |   `-- patch-identity-1
 *     `-- patches
 *         `-- layers
 *             `-- base
 *                 |-- cumulative => {@link DirectoryStructure#getCumulativeLink()}
 *                 `-- references
 *                     `-- &lt;patchId> => {@link DirectoryStructure#getCumulativeRefs(String)}
 * <code>
 * </pre>
 *
 * @author Emanuel Muckenhuber
 */
public interface Identity extends PatchableTarget {

    /**
     * Get the identity name.
     *
     * @return the identity name
     */
    String getName();

    /**
     * Get the identity version.
     *
     * @return the identity version
     */
    String getVersion();

}
