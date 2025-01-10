/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.WildcardReadsTestCase;
import org.junit.BeforeClass;

/**
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_8_0_0)
public class WildcardReads800TestCase extends WildcardReadsTestCase {

    @BeforeClass
    public static void beforeClass() {
        KernelBehavior800TestSuite.initializeDomain();
    }

}
