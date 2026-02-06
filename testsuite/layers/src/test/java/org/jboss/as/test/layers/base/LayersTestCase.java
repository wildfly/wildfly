/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.layers.base;

import java.util.Set;

import org.jboss.as.test.shared.LayersTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;

/**
 * Implementation of {@link LayersTestBase} meant for testing installations
 * provisioned using only the {@code wildfly-ee} feature pack.
 * <p/>
 * See the {@link LayersTestBase} javadoc for an explanation of what these tests do.
 * <p/>
 * The various WildFly installations discussed there are provisioned using {@code galleon-maven-plugin}
 * executions defined in the {@code pom.xml} for this class's maven module.
 * <p/>
 * This subclass provides implementations of methods that provide sets of JBoss Modules module names
 * that are used in {@link LayersTestBase#testUnreferencedModules()}
 * and {@link LayersTestBase#testLayersModuleUse()}. These implementations provide lists that are
 * appropriate for testing installations provisioned using the {@code wildfly-ee} feature pack.
 */
public class LayersTestCase extends LayersTestBase {

    @Override
    public void testUnreferencedModules() throws Exception {
        // For WildFly Preview testing, ignore this test. The testing in the layers-expansion
        // testsuite module covers this.
        // In this module the test-standalone-reference installation will be
        // provisioned with a lot of MP, etc modules, as that's what wildfly-preview FP does.
        // But the test-all-layers installation will not include the expansion layers as
        // testing those is out of scope for this module.
        AssumeTestGroupUtil.assumeNotWildFlyPreview();

        super.testUnreferencedModules();
    }

    @Override
    public void testLayersModuleUse() throws Exception {
        // For WildFly Preview testing, ignore this test. The testing in the layers-expansion
        // testsuite module covers this.
        // In this module the test-standalone-reference installation will be
        // provisioned with a lot of MP, etc modules, as that's what wildfly-preview FP does.
        // But the test-all-layers installation will not include the expansion layers as
        // testing those is out of scope for this module.
        AssumeTestGroupUtil.assumeNotWildFlyPreview();

        super.testLayersModuleUse();
    }

    protected Set<String> getExpectedUnreferenced() {
        return concatArrays(NO_LAYER_OR_REFERENCE_COMMON, NOT_REFERENCED_COMMON, NO_LAYER_OR_REFERENCE_WILDFLY_EE, NOT_REFERENCED_WILDFLY_EE);
    }

    protected  Set<String> getExpectedUnusedInAllLayers() {
        return concatArrays(NO_LAYER_OR_REFERENCE_COMMON, NO_LAYER_COMMON, NO_LAYER_OR_REFERENCE_WILDFLY_EE, NO_LAYER_WILDFLY_EE);
    }
}
