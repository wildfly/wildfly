/*
Copyright 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.jboss.as.test.integration.domain.mixed.eap700;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.test.integration.domain.mixed.DomainHostExcludesTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.junit.BeforeClass;

/**
 * Tests of the ability of a DC to exclude resources from visibility to an EAP 7.0.0 slave.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_7_0_0)
public class DomainHostExcludes700TestCase extends DomainHostExcludesTest {

    @BeforeClass
    public static void beforeClass() throws InterruptedException, TimeoutException, MgmtOperationException, IOException {
        LegacyConfig700TestSuite.initializeDomain();
        setup(DomainHostExcludes700TestCase.class,"EAP7.0", ModelVersion.create(4, 1));
    }
}
