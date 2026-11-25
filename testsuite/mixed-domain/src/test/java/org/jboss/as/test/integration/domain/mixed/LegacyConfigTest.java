/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests secondary behavior in a mixed domain when the primary has booted with a legacy domain.xml.
 *
 * @author Brian Stansberry
 */
public abstract class LegacyConfigTest {
    private static final Logger LOGGER = Logger.getLogger(LegacyConfigTest.class);
    private static final PathElement SECONDARY = PathElement.pathElement("host", "secondary");
    private static final PathAddress TEST_SERVER_CONFIG = PathAddress.pathAddress(SECONDARY,
            PathElement.pathElement("server-config", "legacy-server"));
    private static final PathAddress TEST_SERVER = PathAddress.pathAddress(SECONDARY,
            PathElement.pathElement("server", "legacy-server"));
    private static final PathAddress TEST_SERVER_GROUP = PathAddress.pathAddress("server-group", "legacy-group");

    private static final Map<String, String> STD_PROFILES;

    static {
        final Map<String, String> stdProfiles = new HashMap<>();
        stdProfiles.put("default", "standard-sockets");
        stdProfiles.put("ha", "ha-sockets");
        stdProfiles.put("full", "full-sockets");
        stdProfiles.put("full-ha", "full-ha-sockets");
        STD_PROFILES = Collections.unmodifiableMap(stdProfiles);
    }

    private static DomainTestSupport support;

    @Before
    public void init() throws Exception {
        support = MixedDomainTestSuite.getSupport(this.getClass());
    }

    @Test
    public void testServerLaunching() throws IOException, MgmtOperationException {

        DomainClient client = support.getDomainPrimaryLifecycleUtil().getDomainClient();
        for (Map.Entry<String, String> entry : getProfilesToTest().entrySet()) {
            String profile = entry.getKey();
            String sbg = entry.getValue();
            try {
                installTestServer(client, profile, sbg);
                awaitServerLaunch(client);
                validateServerProfile(client, profile);
                verifyHttp(profile);
            } finally {
                cleanTestServer(client);
            }
        }
    }

    private void installTestServer(ModelControllerClient client, String profile, String sbg) throws IOException, MgmtOperationException {
        ModelNode op = Util.createAddOperation(TEST_SERVER_GROUP);
        op.get("profile").set(profile);
        op.get("socket-binding-group").set(sbg);
        DomainTestUtils.executeForResult(op, client);

        op = Util.createAddOperation(TEST_SERVER_CONFIG);
        op.get("group").set("legacy-group");
        DomainTestUtils.executeForResult(op, client);

        DomainTestUtils.executeForResult(Util.createEmptyOperation("start", TEST_SERVER_CONFIG), client);
    }

    private void awaitServerLaunch(ModelControllerClient client) throws IOException, MgmtOperationException {
        DomainTestUtils.waitUntilState(client, TEST_SERVER_CONFIG, "STARTED");
        MixedDomainTestSupport.assertNoBootErrors(client, TEST_SERVER);
    }

    private void validateServerProfile(ModelControllerClient client, String profile) throws IOException, MgmtOperationException {
        ModelNode op = Util.getReadAttributeOperation(TEST_SERVER, "profile-name");
        ModelNode result = DomainTestUtils.executeForResult(op, client);
        Assert.assertEquals(profile, result.asString());
    }

    private void verifyHttp(String profile) {
        try {
            URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(DomainTestSupport.secondaryAddress) + ":8080").openConnection();
            connection.connect();
        } catch (IOException e) {
            Assert.fail("Cannot connect to profile " + profile + " " + e.toString());
        }
    }

    private void cleanTestServer(ModelControllerClient client) {
        try {
            ModelNode op = Util.createEmptyOperation("stop", TEST_SERVER_CONFIG);
            op.get("blocking").set(true);
            DomainTestUtils.executeForResult(op, client);
        } catch (MgmtOperationException | IOException e) {
            LOGGER.error("Failed to stop legacy server on secondary host", e);
        } finally {
            try {
                DomainTestUtils.executeForResult(Util.createRemoveOperation(TEST_SERVER_CONFIG), client);
            } catch (MgmtOperationException | IOException e) {
                LOGGER.error("Failed to remove legacy-server resource on secondary host", e);
            } finally {
                try {
                    DomainTestUtils.executeForResult(Util.createRemoveOperation(TEST_SERVER_GROUP), client);
                } catch (MgmtOperationException | IOException e) {
                    LOGGER.error("Failed to remove legacy-group resource on secondary host", e);
                }
            }
        }
    }


    protected Map<String, String> getProfilesToTest() {
        return new HashMap<>(STD_PROFILES);
    }
}
