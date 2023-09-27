/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.ElytronOnlyPrimarySmokeTestCase;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * @author Martin Simka
 */
@Version(Version.AsVersion.EAP_7_4_0)
public class ElytronOnlyPrimarySmoke740TestCase extends ElytronOnlyPrimarySmokeTestCase {

    @BeforeClass
    public static void beforeClass() {
        ElytronOnlyPrimary740TestSuite.initializeDomain();
    }
}
