/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_HOME;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PROFILE;
import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.INFINISPAN_SERVER_PROFILE_DEFAULT;

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

    public static final InfinispanServerRule INFINISPAN_SERVER_RULE;

    static {
        String profile = (INFINISPAN_SERVER_PROFILE == null || INFINISPAN_SERVER_PROFILE.isEmpty()) ? INFINISPAN_SERVER_PROFILE_DEFAULT : INFINISPAN_SERVER_PROFILE;
        // Workaround for "ISPN-13107 ServerRunMode.FORKED yields InvalidPathException with relative server config paths on Windows platform" by using absolute file path which won't get mangled.
        String absoluteConfigurationFile = null;
        try {
            absoluteConfigurationFile = Paths.get(Objects.requireNonNull(AbstractClusteringTestCase.class.getClassLoader().getResource(profile)).toURI()).toFile().toString();
        } catch (URISyntaxException ignore) {
        }

        InfinispanServerRuleBuilder builder = InfinispanServerRuleBuilder
                .config(absoluteConfigurationFile);
        // When WildFly Glow is instantiating the deployment outside of the test excution, INFINISPAN_SERVER_HOME is null
        if (!GlowUtil.isGlowScan()) {
            builder.property(TestSystemPropertyNames.INFINISPAN_TEST_SERVER_DIR, INFINISPAN_SERVER_HOME);
        }
        INFINISPAN_SERVER_RULE = builder.property("infinispan.client.rest.auth_username", "testsuite-driver-user")
                .property("infinispan.client.rest.auth_password", "testsuite-driver-password")
                // When WildFly Glow is instantiating the deployment, we don't want to start any server.
                .numServers(GlowUtil.isGlowScan() ? 0 : 1)
                .runMode(ServerRunMode.FORKED)
                .build();
    }

    public static TestRule infinispanServerTestRule() {
        return INFINISPAN_SERVER_RULE;
    }

}
