/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.layers.expansion;

import java.util.Set;
import org.jboss.as.test.shared.LayersTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;

public class LayersTestCase extends LayersTestBase {

    protected Set<String> getExpectedUnreferenced() {
        String[] extra = AssumeTestGroupUtil.isWildFlyPreview() ? NOT_REFERENCED_WILDFLY_PREVIEW : NOT_REFERENCED_WILDFLY;
        return concatArrays(NO_LAYER_OR_REFERENCE_COMMON, NOT_REFERENCED_COMMON, NOT_REFERENCED_EXPANSION, extra);
    }

    protected  Set<String> getExpectedUnusedInAllLayers() {
        String[] extra = AssumeTestGroupUtil.isWildFlyPreview() ? NO_LAYER_WILDFLY_PREVIEW : NO_LAYER_WILDFLY;
        return concatArrays(NO_LAYER_OR_REFERENCE_COMMON, NO_LAYER_COMMON, NO_LAYER_EXPANSION, extra);
    }
}
