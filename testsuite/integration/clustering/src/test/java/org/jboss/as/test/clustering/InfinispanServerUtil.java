/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_HOME;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PROFILE;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.core.TestSystemPropertyNames;
import org.infinispan.server.test.junit4.InfinispanServerRule;
import org.infinispan.server.test.junit4.InfinispanServerRuleBuilder;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.GlowUtil;
import org.junit.rules.TestRule;

/**
 * @author Radoslav Husar
 */
public class InfinispanServerUtil {

    public static final InfinispanServerRule INFINISPAN_SERVER_RULE = !GlowUtil.isGlowScan() ? createInfinispanServerRule() : null;

    public static InfinispanServerRule createInfinispanServerRule() {
        try {
            // Workaround for "ISPN-13107 ServerRunMode.FORKED yields InvalidPathException with relative server config paths on Windows platform" by using absolute file path which won't get mangled.
            String path = Paths.get(Objects.requireNonNull(AbstractClusteringTestCase.class.getClassLoader().getResource(INFINISPAN_SERVER_PROFILE), INFINISPAN_SERVER_PROFILE).toURI()).toFile().toString();

            return InfinispanServerRuleBuilder.config(path)
                    .property("infinispan.client.rest.auth_username", "testsuite-driver-user")
                    .property("infinispan.client.rest.auth_password", "testsuite-driver-password")
                    .property(TestSystemPropertyNames.INFINISPAN_TEST_SERVER_DIR, INFINISPAN_SERVER_HOME)
                    .numServers(1)
                    .runMode(ServerRunMode.FORKED)
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static TestRule infinispanServerTestRule() {
        return INFINISPAN_SERVER_RULE;
    }
}
