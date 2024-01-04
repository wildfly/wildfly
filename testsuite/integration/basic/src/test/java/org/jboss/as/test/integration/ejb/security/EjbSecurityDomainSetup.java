/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import java.io.File;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.wildfly.test.security.common.elytron.EjbElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ElytronDomainSetup;
import org.wildfly.test.security.common.elytron.ServletElytronDomainSetup;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EjbSecurityDomainSetup extends AbstractSecurityDomainSetup {

    protected static final String DEFAULT_SECURITY_DOMAIN_NAME = "ejb3-tests";
    private ElytronDomainSetup elytronDomainSetup;
    private EjbElytronDomainSetup ejbElytronDomainSetup;
    private ServletElytronDomainSetup servletElytronDomainSetup;

    @Override
    protected String getSecurityDomainName() {
        return DEFAULT_SECURITY_DOMAIN_NAME;
    }

    protected String getUsersFile() {
        return new File(EjbSecurityDomainSetup.class.getResource("users.properties").getFile()).getAbsolutePath();
    }

    protected String getGroupsFile() {
        return new File(EjbSecurityDomainSetup.class.getResource("roles.properties").getFile()).getAbsolutePath();
    }

    public boolean isUsersRolesRequired() {
        return true;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        elytronDomainSetup = new ElytronDomainSetup(getUsersFile(), getGroupsFile(), getSecurityDomainName());
        ejbElytronDomainSetup = new EjbElytronDomainSetup(getSecurityDomainName());
        servletElytronDomainSetup = new ServletElytronDomainSetup(getSecurityDomainName());

        elytronDomainSetup.setup(managementClient, containerId);
        ejbElytronDomainSetup.setup(managementClient, containerId);
        servletElytronDomainSetup.setup(managementClient, containerId);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) {
        if (elytronDomainSetup != null) {
            // if one of tearDown will fail, rest of them should be called through
            Exception ex = null;
            try {
                servletElytronDomainSetup.tearDown(managementClient, containerId);
            } catch (Exception e) {
                ex = e;
            }
            try {
                ejbElytronDomainSetup.tearDown(managementClient, containerId);
            } catch (Exception e) {
                if (ex == null) ex = e;
            }
            try {
                elytronDomainSetup.tearDown(managementClient, containerId);
            } catch (Exception e) {
                if (ex == null) ex = e;
            }
            if (ex != null) throw new RuntimeException(ex);
        }
    }
}
