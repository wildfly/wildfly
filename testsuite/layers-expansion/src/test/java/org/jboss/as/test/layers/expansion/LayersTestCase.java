/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.layers.expansion;

import java.util.Set;
import org.jboss.as.test.shared.LayersTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;

/**
 * Implementation of {@link LayersTestBase} meant for testing installations
 * provisioned using the {@code wildfly} feature pack or the {@code wildfly-preview} feature pack.
 * <p/>
 * See the {@link LayersTestBase} javadoc for an explanation of what these tests do.
 * <p/>
 * The various WildFly installations discussed there are provisioned using {@code galleon-maven-plugin}
 * executions defined in the {@code pom.xml} for this class's maven module.
 * <p/>
 * This subclass provides implementations of methods that provide sets of JBoss Modules module names
 * that are used in {@link LayersTestBase#testUnreferencedModules()}
 * and {@link LayersTestBase#testLayersModuleUse()}. These implementations provide lists that are
 * appropriate for testing installations provisioned using the {@code wildfly} feature pack or
 * the {@code wildfly-preview} feature pack.
 */
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
