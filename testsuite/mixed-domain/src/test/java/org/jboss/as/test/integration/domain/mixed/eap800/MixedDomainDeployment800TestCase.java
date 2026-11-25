/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import org.jboss.as.test.integration.domain.mixed.MixedDomainDeploymentTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Version(AsVersion.EAP_8_0_0)
public class MixedDomainDeployment800TestCase extends MixedDomainDeploymentTest {
    @BeforeClass
    public static void beforeClass() {
        MixedDomain800TestSuite.initializeDomain();
    }

    @Override
    protected boolean supportManagedExplodedDeployment() {
        return true;
    }
}
