/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
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

package org.wildfly.test.integration.vdx.domain;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.vdx.TestBase;
import org.wildfly.test.integration.vdx.category.DomainTests;
import org.wildfly.test.integration.vdx.utils.server.ServerConfig;

/**
 *
 * Created by rsvoboda on 12/15/16.
 */

@RunAsClient
@RunWith(Arquillian.class)
@Category(DomainTests.class)
public class JBossWSDomainTestCase extends TestBase {

    /*
     * <modify-wsdl-address /> instead of <modify-wsdl-address>true</modify-wsdl-address> in domain profiles
     */
    private void startAndCheckLogsForWsdlAddressElementWithNoValue() throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "<modify-wsdl-address/>");
        assertContains(errorLog, " ^^^^ Wrong type for 'modify-wsdl-address'. Expected [BOOLEAN] but was");
        assertContains(errorLog, "STRING");
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddModifyWsdlAddressElementWithNoValue.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "default")
    public void modifyWsdlAddressElementWithNoValue()throws Exception {
        startAndCheckLogsForWsdlAddressElementWithNoValue();
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddModifyWsdlAddressElementWithNoValue.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "ha")
    public void modifyWsdlAddressElementWithNoValueHa()throws Exception {
        startAndCheckLogsForWsdlAddressElementWithNoValue();
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddModifyWsdlAddressElementWithNoValue.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "full")
    public void modifyWsdlAddressElementWithNoValueFull()throws Exception {
        startAndCheckLogsForWsdlAddressElementWithNoValue();
    }
    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddModifyWsdlAddressElementWithNoValue.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "full-ha")
    public void modifyWsdlAddressElementWithNoValueFullHa()throws Exception {
        startAndCheckLogsForWsdlAddressElementWithNoValue();
    }


    /*
     * <mmodify-wsdl-address>true</mmodify-wsdl-address> instead of <modify-wsdl-address>true</modify-wsdl-address>
     */
    private void startAndCheckLogsForIncorrectlyNamedWsdlAddressElementWithNoValue() throws Exception {
        container().tryStartAndWaitForFail();

        String errorLog = container().getErrorMessageFromServerStart();
        assertContains(errorLog, "<mmodify-wsdl-address>true</mmodify-wsdl-address>");
        assertContains(errorLog, "^^^^ 'mmodify-wsdl-address' isn't an allowed element here");
        assertContains(errorLog, " Did you mean 'modify-wsdl-address'?");
        assertContains(errorLog, "Elements allowed here are:");
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddIncorrectlyNamedModifyWsdlAddressElement.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "default")
    public void incorrectlyNamedModifyWsdlAddressElement()throws Exception {
        startAndCheckLogsForIncorrectlyNamedWsdlAddressElementWithNoValue();
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddIncorrectlyNamedModifyWsdlAddressElement.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "ha")
    public void incorrectlyNamedModifyWsdlAddressElementHa()throws Exception {
        startAndCheckLogsForIncorrectlyNamedWsdlAddressElementWithNoValue();
    }

    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddIncorrectlyNamedModifyWsdlAddressElement.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "full")
    public void incorrectlyNamedModifyWsdlAddressElementFull()throws Exception {
        startAndCheckLogsForIncorrectlyNamedWsdlAddressElementWithNoValue();
    }
    @Test
    @ServerConfig(configuration = "domain.xml", xmlTransformationGroovy = "webservices/AddIncorrectlyNamedModifyWsdlAddressElement.groovy",
            subtreeName = "webservices", subsystemName = "webservices", profileName = "full-ha")
    public void incorrectlyNamedModifyWsdlAddressElementFullHa()throws Exception {
        startAndCheckLogsForIncorrectlyNamedWsdlAddressElementWithNoValue();
    }

}