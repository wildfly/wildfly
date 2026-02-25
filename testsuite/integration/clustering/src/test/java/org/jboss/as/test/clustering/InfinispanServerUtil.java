/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.*;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Objects;

import org.infinispan.server.test.core.ServerRunMode;
import org.infinispan.server.test.core.TestSystemPropertyNames;
import org.infinispan.server.test.junit5.InfinispanServerExtension;
import org.infinispan.server.test.junit5.InfinispanServerExtensionBuilder;
import org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase;
import org.jboss.as.test.shared.GlowUtil;

/**
 * @author Radoslav Husar
 */
public class InfinispanServerUtil {

    public static final InfinispanServerExtension INFINISPAN_SERVER_EXTENSION = !GlowUtil.isGlowScan() ? createInfinispanServerExtension() : null;

    public static InfinispanServerExtension createInfinispanServerExtension() {
        // Workaround for "ISPN-13107 ServerRunMode.FORKED yields InvalidPathException with relative server config paths on Windows platform" by using absolute file path which won't get mangled.
        String path = Paths.get(URI.create(Objects.requireNonNull(AbstractClusteringTestCase.class.getClassLoader().getResource(INFINISPAN_SERVER_PROFILE), INFINISPAN_SERVER_PROFILE).toString())).toFile().toString();

        return InfinispanServerExtensionBuilder.config(path)
                .property("infinispan.client.rest.auth_username", "testsuite-driver-user")
                .property("infinispan.client.rest.auth_password", "testsuite-driver-password")
                .property(TestSystemPropertyNames.INFINISPAN_TEST_SERVER_DIR, INFINISPAN_SERVER_HOME)
                .numServers(1)
                .runMode(ServerRunMode.FORKED)
                .build();
    }

    public static InfinispanServerExtension infinispanServerExtension() {
        return INFINISPAN_SERVER_EXTENSION;
    }
}
