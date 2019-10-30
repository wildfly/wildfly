/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.wildfly.test.integration.vdx.standalone;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.StandaloneTests;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

/**
 * Test cases for Elytron subsystem configuration
 *
 * Created by rsvoboda on 2/4/17.
 */
@RunAsClient
@RunWith(Arquillian.class)
@Category(StandaloneTests.class)
public class ElytronTestCase extends TestBase {

    /*
     * misplaced 'plain-text' attribute for properties-realm definition
     */
    @Test
    @ServerConfig(configuration = "standalone.xml", xmlTransformationGroovy = "elytron/MisplacedAttributeForPropertiesRealm.groovy",
            subtreeName = "elytron", subsystemName = "elytron")
    public void misplacedAttributeForPropertiesRealm()throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "OPVDX001: Validation error in standalone.xml");
        assertContains(errorLog, "^^^^ 'plain-text' isn't an allowed attribute for the 'properties-realm'");
        assertContains(errorLog, "Attributes allowed here are: groups-attribute, groups-properties");
        assertContains(errorLog, "name, users-properties");
        assertContains(errorLog, "'plain-text' is allowed on elements:");
        assertContains(errorLog, "server > management > security-realms > security-realm > authentication > properties");
        assertContains(errorLog, "server > profile > {urn:wildfly:elytron");
        assertContains(errorLog, "subsystem > security-realms > properties-realm > users-properties");

    }
}
