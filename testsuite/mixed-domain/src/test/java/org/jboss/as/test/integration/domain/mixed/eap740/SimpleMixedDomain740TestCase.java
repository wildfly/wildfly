/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import org.jboss.as.test.integration.domain.mixed.SimpleMixedDomainTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Version(AsVersion.EAP_7_4_0)
public class SimpleMixedDomain740TestCase extends SimpleMixedDomainTest {

    @BeforeClass
    public static void beforeClass() {
        MixedDomain740TestSuite.initializeDomain();
    }
}
