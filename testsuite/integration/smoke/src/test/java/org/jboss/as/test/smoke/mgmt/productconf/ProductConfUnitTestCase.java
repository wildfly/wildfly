/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.mgmt.productconf;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_RUNTIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PRODUCT_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STABILITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
public class ProductConfUnitTestCase extends ContainerResourceMgmtTestBase {
    static String releaseName = System.getProperty("jboss.ee.dist.product.release.name");
    static String releaseVersion = System.getProperty("jboss.ee.dist.product.release.version");
    static String stability = System.getProperty("jboss.ee.dist.product.stability.level");
    static String minStability = System.getProperty("jboss.ee.dist.product.min.stability.level");
    static PathAddress CORE_SERVICE_SERVER_ENVIRONMENT = PathAddress.pathAddress(CORE_SERVICE, "server-environment");

    @BeforeAll
    public static void beforeAll() {
        if (AssumeTestGroupUtil.isWildFlyPreview()) {
            releaseName = System.getProperty("jboss.preview.dist.product.release.name");
            releaseVersion = System.getProperty("jboss.preview.dist.product.release.version");
            stability = System.getProperty("jboss.preview.dist.product.stability.level");
            minStability = System.getProperty("jboss.preview.dist.product.min.stability.level");
        } else if (AssumeTestGroupUtil.isFullDistribution()) {
            releaseName = System.getProperty("jboss.full.dist.product.release.name");
            releaseVersion = System.getProperty("jboss.full.dist.product.release.version");
            stability = System.getProperty("jboss.full.dist.product.stability.level");
            minStability = System.getProperty("jboss.full.dist.product.min.stability.level");
        }
    }

    @Test
    public void testProductConfOverride() throws Exception {
        ModelNode op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        ModelNode response = executeOperation(op);

        assertEquals(releaseName, response.get(PRODUCT_NAME).asString(), "Management model product-name does not match JBoss-Product-Release-Name");
        assertEquals(releaseVersion, response.get(PRODUCT_VERSION).asString(), "Management model product-version does not match JBoss-Product-Release-Version");

        op = Util.createEmptyOperation(READ_RESOURCE_OPERATION, CORE_SERVICE_SERVER_ENVIRONMENT);
        op.get(INCLUDE_RUNTIME).set(true);
        response = executeOperation(op);

        assertEquals(stability, response.get(STABILITY).asString(), "Management model stability level does not match JBoss-Product-Stability");
        Stability minStabilityEnum = Stability.fromString(minStability);
        List<ModelNode> enabledStabilities = response.get("permissible-stability-levels").asList();
        boolean found = false;
        for (ModelNode enabledStability: enabledStabilities) {
            Stability enabledStabilityEnum = Stability.fromString(enabledStability.asString());
            assertTrue(minStabilityEnum.enables(enabledStabilityEnum), "Management model permissible-stability-levels list does not match JBoss-Product-Minimum-Stability");
            if (!found) {
                found = enabledStabilityEnum == minStabilityEnum;
            }
        }
        if (!found) {
            fail("Management model minimum stability level is not available on the management model permissible-stability-levels");
        }
    }
}
