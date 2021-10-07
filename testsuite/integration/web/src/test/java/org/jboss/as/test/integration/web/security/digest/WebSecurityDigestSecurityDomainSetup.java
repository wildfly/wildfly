/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
