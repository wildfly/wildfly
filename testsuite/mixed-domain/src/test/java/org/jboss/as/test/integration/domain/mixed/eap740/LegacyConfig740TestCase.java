/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.LegacyConfigTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 7.4 variant of the superclass.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_7_4_0)
public class LegacyConfig740TestCase extends LegacyConfigTest {

    @BeforeClass
    public static void beforeClass() {
        LegacyConfig740TestSuite.initializeDomain();
    }
}
