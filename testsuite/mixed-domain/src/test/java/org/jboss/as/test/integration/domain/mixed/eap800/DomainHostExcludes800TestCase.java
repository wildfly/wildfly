/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap800;

import static org.jboss.as.test.integration.domain.mixed.Version.AsVersion.EAP_8_0_0;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.as.test.integration.domain.mixed.DomainHostExcludesTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.BeforeClass;

/**
 * Tests of the ability of a DC to exclude resources from visibility to an EAP 7.4.0 secondary.
 *
 * @author Brian Stansberry
 */
@Version(EAP_8_0_0)
public class DomainHostExcludes800TestCase extends DomainHostExcludesTest {

    @BeforeClass
    public static void beforeClass() throws InterruptedException, TimeoutException, MgmtOperationException, IOException {
        LegacyConfig800TestSuite.initializeDomain();
        setup(DomainHostExcludes800TestCase.class, EAP_8_0_0.getHostExclude(), EAP_8_0_0.getModelVersion());
    }
}
