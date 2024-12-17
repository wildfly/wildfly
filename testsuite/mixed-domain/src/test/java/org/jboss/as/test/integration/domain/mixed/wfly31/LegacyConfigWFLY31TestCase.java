/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.LegacyConfigTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * WildFLy 31 variant of the superclass.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.WFLY_31_0_0)
public class LegacyConfigWFLY31TestCase extends LegacyConfigTest {

    @BeforeClass
    public static void beforeClass() {
        LegacyConfigWFLY31TestSuite.initializeDomain();
    }
}
