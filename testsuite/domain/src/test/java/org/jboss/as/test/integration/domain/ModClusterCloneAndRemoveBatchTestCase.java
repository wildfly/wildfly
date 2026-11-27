/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.domain;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLONE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TO_PROFILE;

import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Domain integration reproducer for WFLY-20863.
 *
 * <p>Verifies that a composite operation that clones a profile containing a mod-cluster
 * filter and in the same composite removes that filter from the clone completes
 * successfully.</p>
 *
 * <p>Domain config: {@code domain-configs/domain-batch-mod-cluster-clone.xml}.</p>
 */
public class ModClusterCloneAndRemoveBatchTestCase {

    private static final String SOURCE_PROFILE = "load-balancer";
    private static final String CLONE_PROFILE  = "load-balancer-clone";

    private static final PathAddress SOURCE_PROFILE_ADDRESS = PathAddress.pathAddress(PROFILE, SOURCE_PROFILE);
    private static final PathAddress MOD_CLUSTER_IN_CLONE   = PathAddress.pathAddress(
            PathElement.pathElement(PROFILE, CLONE_PROFILE),
            PathElement.pathElement("subsystem", "undertow"),
            PathElement.pathElement("configuration", "filter"),
            PathElement.pathElement("mod-cluster", "load-balancer"));

    private static DomainTestSupport testSupport;
    private static DomainClient primaryClient;

    @BeforeClass
    public static void setUp() throws Exception {
        DomainTestSupport.Configuration configuration = DomainTestSupport.Configuration.create(
                ModClusterCloneAndRemoveBatchTestCase.class.getSimpleName(),
                "domain-configs/domain-batch-mod-cluster-clone.xml",
                "host-configs/host-primary.xml",
                null);
        testSupport = DomainTestSupport.create(configuration);
        testSupport.start();
        DomainLifecycleUtil primaryUtil = testSupport.getDomainPrimaryLifecycleUtil();
        primaryClient = primaryUtil.getDomainClient();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testSupport != null) {
            testSupport.close();
        }
    }

    /**
     * Reproduces the domain batch from WFLY-20863:
     * <pre>
     *   batch
     *   /profile=load-balancer:clone(to-profile=load-balancer-clone)
     *   /profile=load-balancer-clone/subsystem=undertow/configuration=filter/mod-cluster=load-balancer:remove()
     *   run-batch
     * </pre>
     */
    @Test
    public void testCloneProfileAndRemoveModClusterInBatch() throws Exception {
        ModelNode cloneOp = Util.createOperation(CLONE, SOURCE_PROFILE_ADDRESS);
        cloneOp.get(TO_PROFILE).set(CLONE_PROFILE);

        ModelNode removeOp = Util.createRemoveOperation(MOD_CLUSTER_IN_CLONE);

        ModelNode composite = Util.createCompositeOperation(List.of(cloneOp, removeOp));
        DomainTestUtils.executeForResult(composite, primaryClient);
    }
}
