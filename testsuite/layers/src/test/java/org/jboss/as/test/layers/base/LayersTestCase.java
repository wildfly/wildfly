/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.layers.base;

import java.util.Set;

import org.jboss.as.test.shared.LayersTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;

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
