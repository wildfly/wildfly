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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_KEY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOCATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SECRET_ACCESS_KEY;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.exists;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
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
        testSupport = DomainTestSuite.createSupport(ServerManagementTestCase.class.getSimpleName());
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
        ModelNode newMasterDiscoveryOptionAddress = new ModelNode();
        // (host=master),(discovery-option=S3-discovery)
        newMasterDiscoveryOptionAddress.add("host", "master");
        newMasterDiscoveryOptionAddress.add("discovery-option", "S3-discovery");
        addAndRemoveS3DiscoveryOptionTest(masterClient, newMasterDiscoveryOptionAddress);

        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();
        ModelNode newSlaveDiscoveryOptionAddress = new ModelNode();
        // (host=slave),(discovery-option=S3-discovery)
        newSlaveDiscoveryOptionAddress.add("host", "slave");
        newSlaveDiscoveryOptionAddress.add("discovery-option", "S3-discovery");
        addAndRemoveS3DiscoveryOptionTest(slaveClient, newSlaveDiscoveryOptionAddress);
    }

    private void addAndRemoveS3DiscoveryOptionTest(ModelControllerClient client, ModelNode discoveryOptionAddress) throws Exception {
        ModelNode addS3DiscoveryOption = new ModelNode();
        addS3DiscoveryOption.get(OP).set(ADD);
        addS3DiscoveryOption.get(OP_ADDR).set(discoveryOptionAddress);
        addS3DiscoveryOption.get(ACCESS_KEY).set("access_key");
        addS3DiscoveryOption.get(SECRET_ACCESS_KEY).set("secret_access_key");
        addS3DiscoveryOption.get(LOCATION).set("location");

        Assert.assertFalse(exists(discoveryOptionAddress, client));
        ModelNode result = client.execute(addS3DiscoveryOption);
        validateResponse(result, false);
        Assert.assertTrue(exists(discoveryOptionAddress, client));

        final ModelNode removeS3DiscoveryOption = new ModelNode();
        removeS3DiscoveryOption.get(OP).set(REMOVE);
        removeS3DiscoveryOption.get(OP_ADDR).set(discoveryOptionAddress);

        result = client.execute(removeS3DiscoveryOption);
        validateResponse(result);
        Assert.assertFalse(exists(discoveryOptionAddress, client));
    }
}
