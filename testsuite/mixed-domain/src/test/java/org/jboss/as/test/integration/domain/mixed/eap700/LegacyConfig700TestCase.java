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

import org.jboss.as.test.integration.domain.mixed.LegacyConfigTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 7.0 variant of the superclass.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_7_0_0)
public class LegacyConfig700TestCase extends LegacyConfigTest {

    @BeforeClass
    public static void beforeClass() {
        LegacyConfig700TestSuite.initializeDomain();
    }
}
