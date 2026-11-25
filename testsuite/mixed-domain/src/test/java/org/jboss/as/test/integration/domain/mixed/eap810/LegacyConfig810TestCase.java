/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import org.jboss.as.test.integration.domain.mixed.LegacyConfigTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 8.1 variant of the LegacyConfigTest.
 */
@Version(Version.AsVersion.EAP_8_1_0)
public class LegacyConfig810TestCase extends LegacyConfigTest {

    @BeforeClass
    public static void beforeClass() {
        LegacyConfig810TestSuite.initializeDomain();
    }
}
