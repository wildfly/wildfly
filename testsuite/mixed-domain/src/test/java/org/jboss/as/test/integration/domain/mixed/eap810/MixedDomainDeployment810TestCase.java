/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap810;

import org.jboss.as.test.integration.domain.mixed.MixedDomainDeploymentTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 * EAP 8.1 variant of the mixed domain deployment test.
 */
@Version(AsVersion.EAP_8_1_0)
public class MixedDomainDeployment810TestCase extends MixedDomainDeploymentTest {
    @BeforeClass
    public static void beforeClass() {
        MixedDomain810TestSuite.initializeDomain();
    }

    @Override
    protected boolean supportManagedExplodedDeployment() {
        return true;
    }
}
