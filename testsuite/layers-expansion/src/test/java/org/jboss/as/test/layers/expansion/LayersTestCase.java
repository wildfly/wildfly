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
        String[] extra;
        if (AssumeTestGroupUtil.isWildFlyPreview()) {
            extra = NOT_REFERENCED_WILDFLY_PREVIEW;
        } else {
            String[] commonEEUnRef = AssumeTestGroupUtil.isLegacyEEDistribution() ?
                    NOT_REFERENCED_STD_EE_LEGACY :
                    NOT_REFERENCED_STD_EE_LATEST;
            String[] commonEEBoth = AssumeTestGroupUtil.isLegacyEEDistribution() ?
                    NO_LAYER_OR_REFERENCE_COMMON_EE_LEGACY :
                    NO_LAYER_OR_REFERENCE_COMMON_EE_LATEST;
            extra = concatArrays(commonEEUnRef, commonEEBoth, NOT_REFERENCED_WILDFLY).toArray(new String[0]);
        }
        return concatArrays(NO_LAYER_OR_REFERENCE_COMMON, NOT_REFERENCED_COMMON, NOT_REFERENCED_EXPANSION, extra);
    }

    protected  Set<String> getExpectedUnusedInAllLayers() {
        String[] extra;
        if (AssumeTestGroupUtil.isWildFlyPreview()) {
            extra = NO_LAYER_WILDFLY_PREVIEW;
        } else {
            String[] commonEENoLayer = AssumeTestGroupUtil.isLegacyEEDistribution() ?
                    NO_LAYER_STD_EE_LEGACY :
                    NO_LAYER_STD_EE_LATEST;
            String[] commonEEBoth = AssumeTestGroupUtil.isLegacyEEDistribution() ?
                    NO_LAYER_OR_REFERENCE_COMMON_EE_LEGACY :
                    NO_LAYER_OR_REFERENCE_COMMON_EE_LATEST;
            extra = concatArrays(commonEENoLayer, commonEEBoth, NO_LAYER_WILDFLY).toArray(new String[0]);
        }
        return concatArrays(NO_LAYER_OR_REFERENCE_COMMON, NO_LAYER_COMMON, NO_LAYER_EXPANSION, extra);
    }
}
