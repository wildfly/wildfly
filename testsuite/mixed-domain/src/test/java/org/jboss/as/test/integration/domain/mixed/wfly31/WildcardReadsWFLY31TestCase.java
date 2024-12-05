/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.WildcardReadsTestCase;
import org.junit.BeforeClass;

/**
 * Tests for cross-process wildcard reads in a mixed domain. See https://issues.jboss.org/browse/WFCORE-621.
 * This test is specific to WildFly 31.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.WFLY_31_0_0)
public class WildcardReadsWFLY31TestCase extends WildcardReadsTestCase {

    @BeforeClass
    public static void beforeClass() {
        KernelBehaviorWFLY31TestSuite.initializeDomain();
    }

}
