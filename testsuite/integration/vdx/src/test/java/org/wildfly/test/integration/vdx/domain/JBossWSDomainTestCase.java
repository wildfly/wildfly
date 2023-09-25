/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        assertContains(errorLog, "'mmodify-wsdl-address' isn't an allowed element here");
        assertContains(errorLog, "WFLYCTL0198: Unexpected element");
        assertContains(errorLog, "'{urn:jboss:domain:webservices:2.0}mmodify-wsdl-address' encountered");
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
