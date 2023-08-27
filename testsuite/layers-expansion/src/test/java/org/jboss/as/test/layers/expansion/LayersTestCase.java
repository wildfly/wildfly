/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.layers.expansion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.as.test.shared.LayersTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;

public class LayersTestCase extends LayersTestBase {

    protected Set<String> getExpectedUnreferenced() {
        String[] extra = AssumeTestGroupUtil.isWildFlyPreview() ? NOT_REFERENCED_WILDFLY_PREVIEW : NOT_REFERENCED_WILDFLY;
        return new HashSet<>(List.of(concatArrays(NOT_REFERENCED_COMMON, NOT_REFERENCED_EXPANSION, extra)));
    }

    protected  Set<String> getExpectedUnusedInAllLayers() {
        String[] extra = AssumeTestGroupUtil.isWildFlyPreview() ? NO_LAYER_WILDFLY_PREVIEW : NO_LAYER_WILDFLY;
        return new HashSet<>(List.of(concatArrays(NO_LAYER_COMMON, NO_LAYER_EXPANSION, extra)));
    }

    private static String[] concatArrays(String[] common, String[] expansion, String[] pack) {
        String[] result = Arrays.copyOf(common, common.length + expansion.length + pack.length);
        System.arraycopy(expansion, 0, result, common.length, expansion.length);
        System.arraycopy(pack, 0, result, common.length + expansion.length, pack.length);
        return result;
    }
}
