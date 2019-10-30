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

package org.jboss.as.test.integration.domain.mixed;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests slave behavior in a mixed domain when the master has booted with a legacy domain.xml.
 *
 * @author Brian Stansberry
 */
public abstract class LegacyConfigTest {

    private static final PathElement SLAVE = PathElement.pathElement("host", "slave");
    private static final PathAddress TEST_SERVER_CONFIG = PathAddress.pathAddress(SLAVE,
            PathElement.pathElement("server-config", "legacy-server"));
    private static final PathAddress TEST_SERVER = PathAddress.pathAddress(SLAVE,
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
    public void testServerLaunching() throws IOException, MgmtOperationException, InterruptedException {

        DomainClient client = support.getDomainMasterLifecycleUtil().getDomainClient();
        for (Map.Entry<String, String> entry : getProfilesToTest().entrySet()) {
            String profile = entry.getKey();
            String sbg = entry.getValue();
            try {
                installTestServer(client, profile, sbg);
                awaitServerLaunch(client, profile);
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

    private void awaitServerLaunch(ModelControllerClient client, String profile) throws InterruptedException {
        long timeout = System.currentTimeMillis() + TimeoutUtil.adjust(20000);
        ModelNode op = Util.getReadAttributeOperation(TEST_SERVER, "server-state");
        do {
            try {
                ModelNode state = DomainTestUtils.executeForResult(op, client);
                if ("running".equalsIgnoreCase(state.asString())) {
                    return;
                }
            } catch (IOException | MgmtOperationException e) {
                // ignore and try again
            }

            TimeUnit.MILLISECONDS.sleep(250L);
        } while (System.currentTimeMillis() < timeout);

        Assert.fail("Server did not start using " + profile);
    }

    private void validateServerProfile(ModelControllerClient client, String profile) throws IOException, MgmtOperationException {
        ModelNode op = Util.getReadAttributeOperation(TEST_SERVER, "profile-name");
        ModelNode result = DomainTestUtils.executeForResult(op, client);
        Assert.assertEquals(profile, result.asString());
    }

    private void verifyHttp(String profile) {
        try {
            URLConnection connection = new URL("http://" + TestSuiteEnvironment.formatPossibleIpv6Address(DomainTestSupport.slaveAddress) + ":8080").openConnection();
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
            e.printStackTrace();
        } finally {
            try {
                DomainTestUtils.executeForResult(Util.createRemoveOperation(TEST_SERVER_CONFIG), client);
            } catch (MgmtOperationException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    DomainTestUtils.executeForResult(Util.createRemoveOperation(TEST_SERVER_GROUP), client);
                } catch (MgmtOperationException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    protected Map<String, String> getProfilesToTest() {
        return new HashMap<>(STD_PROFILES);
    }
}
