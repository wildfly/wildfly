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

package org.jboss.as.test.integration.domain.mixed.eap620;

import java.util.Map;

import org.jboss.as.test.integration.domain.mixed.LegacyConfigTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;

/**
 * EAP 6.2 variant of the superclass.
 *
 * @author Brian Stansberry
 */
@Version(Version.AsVersion.EAP_6_2_0)
public class LegacyConfig620TestCase extends LegacyConfigTest {

    @BeforeClass
    public static void beforeClass() {
        LegacyConfig620TestSuite.initializeDomain();
    }

    @Override
    protected Map<String, String> getProfilesToTest() {
        Map<String, String> result =  super.getProfilesToTest();

        // The following is unnecessary following addition of LegacyConfigAdjuster620.workAroundWFLY2335
        // but is just commented out in case that proves unreliable

//        // Due to WFLY-2335, EAP 6.2 full-ha servers won't launch on JDK 8,
//        // so skip testing that profile
//        result.remove("full-ha");
        return result;
    }
}
