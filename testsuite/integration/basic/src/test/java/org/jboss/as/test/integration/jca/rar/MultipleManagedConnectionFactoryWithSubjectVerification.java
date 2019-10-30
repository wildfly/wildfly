/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.rar;

import java.security.Principal;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.security.PasswordCredential;
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
