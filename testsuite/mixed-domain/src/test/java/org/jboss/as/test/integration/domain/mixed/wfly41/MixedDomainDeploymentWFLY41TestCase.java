/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly41;

import org.jboss.as.test.integration.domain.mixed.MixedDomainDeploymentTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

@Version(Version.AsVersion.WFLY_41_0_0)
public class MixedDomainDeploymentWFLY41TestCase extends MixedDomainDeploymentTest {

    @BeforeClass
    public static void beforeClass() {
        MixedDomainWFLY41TestSuite.initializeDomain();
    }

}
