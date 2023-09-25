/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.layers.base;


import org.jboss.as.test.shared.LayersTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;

public class LayersTestCase extends LayersTestBase {

    @Override
    public void test() throws Exception {
        // For WildFly Preview testing, ignore this test. The testing in the layers-expansion
        // testsuite module covers this.
        // In this module the test-standalone-reference installation will be
        // provisioned with a lot of MP, etc modules, as that's what wildfly-preview FP does.
        // But the test-all-layers installation will not include the expansion layers as
        // testing those is out of scope for this module.
        AssumeTestGroupUtil.assumeNotWildFlyPreview();

        super.test();
    }
}
