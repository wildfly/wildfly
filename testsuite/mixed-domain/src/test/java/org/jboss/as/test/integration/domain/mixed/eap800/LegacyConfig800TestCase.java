/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import org.jboss.as.test.integration.domain.mixed.LegacyConfigTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 8.0 variant of the superclass.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_8_0_0)
public class LegacyConfig800TestCase extends LegacyConfigTest {

    @BeforeClass
    public static void beforeClass() {
        LegacyConfig800TestSuite.initializeDomain();
    }
}
