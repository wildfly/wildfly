/*
 *
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.jboss.as.test.integration.domain.mixed.eap640;

import org.jboss.as.test.integration.domain.mixed.ElytronOnlyMasterTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Martin Simka
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(value = {ElytronOnlyMasterSmoke640TestCase.class})
@Version(Version.AsVersion.EAP_6_4_0)
@Ignore("Ignore until WFCORE-2882 is integrated")
public class ElytronOnlyMaster640TestSuite extends ElytronOnlyMasterTestSuite {

    @BeforeClass
    public static void initializeDomain() {
        ElytronOnlyMasterTestSuite.getSupport(ElytronOnlyMaster640TestSuite.class);
    }
}
