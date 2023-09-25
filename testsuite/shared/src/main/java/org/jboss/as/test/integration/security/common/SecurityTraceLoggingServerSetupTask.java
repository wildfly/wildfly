/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security.common;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.arquillian.container.ManagementClient;

/**
 * ServerSetupTask implementation which enables TRACE log-level for security related packages.
 *
 * @author Josef Cacek
 */
public class SecurityTraceLoggingServerSetupTask extends AbstractTraceLoggingServerSetupTask {

    @Override
    protected Collection<String> getCategories(ManagementClient managementClient, String containerId) {
        return Arrays.asList("org.jboss.security", "org.jboss.as.security", "org.picketbox",
                "org.apache.catalina.authenticator", "org.jboss.as.web.security", "org.jboss.as.domain.management.security",
                "org.wildfly.security", "org.wildfly.elytron");
    }
}
