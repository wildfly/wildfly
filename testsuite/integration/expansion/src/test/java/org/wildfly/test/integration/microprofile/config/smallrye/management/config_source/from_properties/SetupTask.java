/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties.TestApplication.B_OVERRIDES_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties.TestApplication.FROM_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties.TestApplication.FROM_B;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * Add a config-source with a custom class in the microprofile-config subsystem.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SetupTask extends CLIServerSetupTask {
    private static final String ADDR_A = "/subsystem=microprofile-config-smallrye/config-source=propsA";
    private static final String ADDR_B = "/subsystem=microprofile-config-smallrye/config-source=propsB";

    static final String A = "val-a";
    static final String B = "val-b";
    // In propsA this will be 'overridden-a', in propsB 'overridden-b'. Due to the relative ordinals of the config sources, propsB should win
    private static final String OVERRIDDEN_A = "overridden-a";
    static final String OVERRIDDEN_B = "overridden-b";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        NodeBuilder nb = builder.node(containerId);

        String propsA = String.format("{%s=%s, %s=%s}", FROM_A, A, B_OVERRIDES_A, OVERRIDDEN_A);
        String propsB = String.format("{%s=%s, %s=%s}", FROM_B, B, B_OVERRIDES_A, OVERRIDDEN_B);

        nb.setup(String.format("%s:add(properties=%s)", ADDR_A, propsA));
        nb.setup(String.format("%s:add(properties=%s, ordinal=300)", ADDR_B, propsB));

        nb.teardown(String.format("%s:remove", ADDR_A));
        nb.teardown(String.format("%s:remove", ADDR_B));

        super.setup(managementClient, containerId);
    }
}
