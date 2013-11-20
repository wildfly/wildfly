/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CODE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISCOVERY_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.host.controller.discovery.Constants.ACCESS_KEY;
import static org.jboss.as.host.controller.discovery.Constants.LOCATION;
import static org.jboss.as.host.controller.discovery.Constants.SECRET_ACCESS_KEY;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.exists;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of management operations involving discovery options.
 * 
 * @author Farah Juma
 */
public class DiscoveryOptionTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(DiscoveryOptionTestCase.class.getSimpleName());
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainTestSuite.stopSupport();
        testSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
    }

    @Test
    public void testAddAndRemoveS3DiscoveryOption() throws Exception {
        DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode discoveryOptionProperties = new ModelNode();
        discoveryOptionProperties.get(ACCESS_KEY).set("access_key");
        discoveryOptionProperties.get(SECRET_ACCESS_KEY).set("secret_access_key");
        discoveryOptionProperties.get(LOCATION).set("location");

        ModelNode addDiscoveryOption = getS3DiscoveryOptionAddOperation(discoveryOptionProperties);

        // (host=master),(core-service=discovery-options),(discovery-option=option-one)
        ModelNode newMasterDiscoveryOptionAddress = new ModelNode();
        newMasterDiscoveryOptionAddress.add("host", "master");
        newMasterDiscoveryOptionAddress.add("core-service", "discovery-options");
        newMasterDiscoveryOptionAddress.add("discovery-option", "option-one");
        addAndRemoveDiscoveryOptionTest(masterClient, newMasterDiscoveryOptionAddress, addDiscoveryOption);

        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();
        // (host=slave),(core-service=discovery-options),(discovery-option=option-one)
        ModelNode newSlaveDiscoveryOptionAddress = new ModelNode();
        newSlaveDiscoveryOptionAddress.add("host", "slave");
        newSlaveDiscoveryOptionAddress.add("core-service", "discovery-options");
        newSlaveDiscoveryOptionAddress.add("discovery-option", "option-one");
        addAndRemoveDiscoveryOptionTest(slaveClient, newSlaveDiscoveryOptionAddress, addDiscoveryOption);
    }

    @Test
    public void testAddAndRemoveStaticDiscoveryOption() throws Exception {
        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();
        ModelNode addDiscoveryOption = new ModelNode();
        addDiscoveryOption.get(OP).set(ADD);
        addDiscoveryOption.get(HOST).set("127.0.0.2");
        addDiscoveryOption.get(PORT).set("9999");

        // (host=slave),(core-service=discovery-options),(static-discovery=option-one)
        ModelNode newSlaveDiscoveryOptionAddress = new ModelNode();
        newSlaveDiscoveryOptionAddress.add("host", "slave");
        newSlaveDiscoveryOptionAddress.add("core-service", "discovery-options");
        newSlaveDiscoveryOptionAddress.add("static-discovery", "option-one");
        addAndRemoveDiscoveryOptionTest(slaveClient, newSlaveDiscoveryOptionAddress, addDiscoveryOption);
    }

    @Test
    public void testDiscoveryOptionsOrdering() throws Exception {
        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();
        ModelNode discoveryOptionsAddress = new ModelNode();
        discoveryOptionsAddress.add("host", "slave");
        discoveryOptionsAddress.add("core-service", "discovery-options");
        ModelNode readDiscoveryOptionsOrdering = Util.getReadAttributeOperation(PathAddress.pathAddress(discoveryOptionsAddress), DISCOVERY_OPTIONS);
        ModelNode expectedDiscoveryOptionsOrdering = new ModelNode();

        ModelNode discoveryOptionProperties = new ModelNode();
        discoveryOptionProperties.get(ACCESS_KEY).set("access_key");
        discoveryOptionProperties.get(SECRET_ACCESS_KEY).set("secret_access_key");
        discoveryOptionProperties.get(LOCATION).set("location");
        ModelNode addS3DiscoveryOption = getS3DiscoveryOptionAddOperation(discoveryOptionProperties);

        ModelNode addStaticDiscoveryOption = new ModelNode();
        addStaticDiscoveryOption.get(OP).set(ADD);
        addStaticDiscoveryOption.get(HOST).set("127.0.0.2");
        addStaticDiscoveryOption.get(PORT).set("9999");

        ModelNode result = slaveClient.execute(readDiscoveryOptionsOrdering);
        validateResponse(result);
        Assert.assertFalse(result.hasDefined(RESULT));

        // (host=slave),(core-service=discovery-options),(discovery-option=option-one)
        ModelNode discoveryOptionAddressOne = discoveryOptionsAddress.clone().add("discovery-option", "option-one");
        addDiscoveryOptionTest(slaveClient, discoveryOptionAddressOne, addS3DiscoveryOption);
        expectedDiscoveryOptionsOrdering.add("discovery-option", "option-one");

        // (host=slave),(core-service=discovery-options),(static-discovery=option-two)
        ModelNode discoveryOptionAddressTwo = discoveryOptionsAddress.clone().add("static-discovery", "option-two");
        addDiscoveryOptionTest(slaveClient, discoveryOptionAddressTwo, addStaticDiscoveryOption);
        expectedDiscoveryOptionsOrdering.add("static-discovery", "option-two");

        // (host=slave),(core-service=discovery-options),(discovery-option=option-three)
        ModelNode discoveryOptionAddressThree = discoveryOptionsAddress.clone().add("discovery-option", "option-three");
        addDiscoveryOptionTest(slaveClient, discoveryOptionAddressThree, addS3DiscoveryOption);
        expectedDiscoveryOptionsOrdering.add("discovery-option", "option-three");

        result = slaveClient.execute(readDiscoveryOptionsOrdering);
        ModelNode returnVal = validateResponse(result);
        Assert.assertEquals(expectedDiscoveryOptionsOrdering, returnVal);

        removeDiscoveryOptionTest(slaveClient, discoveryOptionAddressOne);
        removeDiscoveryOptionTest(slaveClient, discoveryOptionAddressTwo);
        removeDiscoveryOptionTest(slaveClient, discoveryOptionAddressThree);

        result = slaveClient.execute(readDiscoveryOptionsOrdering);
        validateResponse(result);
        Assert.assertFalse(result.hasDefined(RESULT));
    }

    private void addAndRemoveDiscoveryOptionTest(ModelControllerClient client, ModelNode discoveryOptionAddress, 
            ModelNode addDiscoveryOption) throws Exception {
        addDiscoveryOptionTest(client, discoveryOptionAddress, addDiscoveryOption);
        removeDiscoveryOptionTest(client, discoveryOptionAddress);
    }

    private ModelNode getS3DiscoveryOptionAddOperation(ModelNode discoveryOptionProperties) {
        ModelNode addDiscoveryOption = new ModelNode();
        addDiscoveryOption.get(OP).set(ADD);
        addDiscoveryOption.get(CODE).set("org.jboss.as.host.controller.discovery.S3Discovery");
        addDiscoveryOption.get(MODULE).set("org.jboss.as.host.controller.discovery");
        addDiscoveryOption.get(PROPERTIES).set(discoveryOptionProperties);
        return addDiscoveryOption;
    }

    private void addDiscoveryOptionTest(ModelControllerClient client, ModelNode discoveryOptionAddress, 
            ModelNode addDiscoveryOption) throws Exception {
        addDiscoveryOption.get(OP_ADDR).set(discoveryOptionAddress);

        Assert.assertFalse(exists(discoveryOptionAddress, client));
        ModelNode result = client.execute(addDiscoveryOption);
        validateResponse(result, false);
        Assert.assertTrue(exists(discoveryOptionAddress, client));
    }

    private void removeDiscoveryOptionTest(ModelControllerClient client, ModelNode discoveryOptionAddress) throws Exception {
        final ModelNode removeDiscoveryOption = new ModelNode();
        removeDiscoveryOption.get(OP).set(REMOVE);
        removeDiscoveryOption.get(OP_ADDR).set(discoveryOptionAddress);

        ModelNode result = client.execute(removeDiscoveryOption);
        validateResponse(result);
        Assert.assertFalse(exists(discoveryOptionAddress, client));
    }
}
