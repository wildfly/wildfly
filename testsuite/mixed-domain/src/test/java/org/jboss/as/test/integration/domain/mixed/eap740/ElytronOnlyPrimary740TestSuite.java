/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.ElytronOnlyPrimaryTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Martin Simka
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value= {ElytronOnlyPrimarySmoke740TestCase.class})
@Version(Version.AsVersion.EAP_7_4_0)
public class ElytronOnlyPrimary740TestSuite extends ElytronOnlyPrimaryTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        ElytronOnlyPrimaryTestSuite.getSupport(ElytronOnlyPrimary740TestSuite.class);
    }
}
