package org.jboss.as.test.integration.domain;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;

/**
 * Verifies that legacy JGroups protocols are not auto-updated by domain controller.
 *
 * (DC can be controlling older EAP version slaves, which require legacy protocols.)
 *
 * https://issues.jboss.org/browse/WFLY-12539
 */
public class LegacyJGroupsProtocolsTestCase {

    private static final PathAddress UDP_STACK_ADDR =
            PathAddress.pathAddress("profile", "default").append("subsystem", "jgroups").append("stack", "udp");

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainClient masterClient;

    @BeforeClass
    public static void setupDomain() throws Exception {
        final DomainTestSupport.Configuration configuration;
        configuration = DomainTestSupport.Configuration.create(LegacyJGroupsProtocolsTestCase.class.getSimpleName(),
                "domain-configs/domain-standard.xml", "host-configs/host-master.xml", null);
        testSupport = DomainTestSupport.create(configuration);
        testSupport.start();
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        masterClient = domainMasterLifecycleUtil.getDomainClient();
    }

    @AfterClass
    public static void tearDownDomain() {
        testSupport.stop();
        domainMasterLifecycleUtil = null;
    }

    @Test
    public void testJGroupsProtocolsNotUpdated() throws Exception {
        ModelNode readOp = Util.createEmptyOperation(ClientConstants.READ_RESOURCE_OPERATION, UDP_STACK_ADDR);
        readOp.get("recursive").set(true);
        ModelNode result = executeForResult(readOp, masterClient);

        // UNICAST2 defined in configuration should not be auto-updated to UNICAST3
        ModelNode protocols = result.get("protocol");
        Assert.assertTrue("UNICAST2 protocol is expected to be present, actual protocols: " + protocols,
                protocols.toString().contains("UNICAST2"));
    }
}
