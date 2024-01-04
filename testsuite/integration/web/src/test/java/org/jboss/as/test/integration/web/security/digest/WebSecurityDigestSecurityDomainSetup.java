/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.security.digest;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.wildfly.test.security.common.elytron.PropertyFileBasedDomain;
import org.wildfly.test.security.common.elytron.UndertowDomainMapper;

/**
 * Security domain setup for digest tests. This prepare either legacy security-domain or elytron configuration.
 *
 * @author olukas, jstourac
 */
public class WebSecurityDigestSecurityDomainSetup implements ServerSetupTask {

    private CLIWrapper cli;
    private PropertyFileBasedDomain ps;
    private UndertowDomainMapper domainMapper;

    protected static final String SECURITY_DOMAIN_NAME = "digestSecurityDomain";
    protected static final String GOOD_USER_NAME = "anil";
    protected static final String GOOD_USER_PASSWORD = "anil";
    private static final String GOOD_USER_ROLE = SimpleSecuredServlet.ALLOWED_ROLE;

    private static final String SUPER_USER_NAME = "marcus";
    private static final String SUPER_USER_PASSWORD = "marcus";
    private static final String SUPER_USER_ROLE = "superuser";

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        cli = new CLIWrapper(true);
        setupElytronBasedSecurityDomain();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        domainMapper.remove(cli);
        ps.remove(cli);
        cli.close();
    }

    private void setupElytronBasedSecurityDomain() throws Exception {
        ps = PropertyFileBasedDomain.builder()
                .withUser(GOOD_USER_NAME, GOOD_USER_PASSWORD, GOOD_USER_ROLE)
                .withUser(SUPER_USER_NAME, SUPER_USER_PASSWORD, SUPER_USER_ROLE)
                .withName(SECURITY_DOMAIN_NAME).build();
        ps.create(cli);
        domainMapper = UndertowDomainMapper.builder().withName(SECURITY_DOMAIN_NAME).build();
        domainMapper.create(cli);
    }
}
