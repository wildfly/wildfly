/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.eap740;

import static org.jboss.as.test.integration.domain.mixed.Version.AsVersion.EAP_7_4_0;

import java.io.IOException;

import org.jboss.as.test.integration.domain.mixed.PatchRemoteHostTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

@Version(EAP_7_4_0)
public class PatchRemoteHost740TestCase extends PatchRemoteHostTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        MixedDomain740TestSuite.initializeDomain();
        PatchRemoteHostTest.setup(PatchRemoteHost740TestCase.class);
    }
}
