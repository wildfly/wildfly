/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
