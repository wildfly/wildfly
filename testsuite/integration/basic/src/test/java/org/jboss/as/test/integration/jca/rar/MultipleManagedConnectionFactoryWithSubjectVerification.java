/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.rar;

import java.security.Principal;
import java.util.Set;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.principal.NamePrincipal;

public class MultipleManagedConnectionFactoryWithSubjectVerification extends MultipleManagedConnectionFactory1 {

    private static Logger log = Logger.getLogger(MultipleManagedConnectionFactoryWithSubjectVerification.class.getName());

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        log.trace("createManagedConnection()");

        Set<Principal> principals = subject.getPrincipals();
        if (!principals.contains(new NamePrincipal("sa"))) {
            throw new ResourceException("Subject should contain principal with username 'sa'");
        }

        Set<Object> privateCredentials = subject.getPrivateCredentials();
        if (!privateCredentials.contains(new PasswordCredential("sa", "sa".toCharArray()))) {
            throw new ResourceException("Subject should contain private credential with password 'sa'");
        }

        return new MultipleManagedConnection1(this);
    }
}
