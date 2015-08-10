/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.security.context;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import static org.jboss.as.test.integration.security.context.EjbOwnSecurityDomainImpl.SECURITY_DOMAIN;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * EJB which does not use the same security-domain as deployment from ReuseAuthenticatedSubjectTestCase.
 *
 * @author olukas
 */
@Stateless
@DeclareRoles(EjbOwnSecurityDomainImpl.AUTHORIZED_ROLE)
@SecurityDomain(SECURITY_DOMAIN)
public class EjbOwnSecurityDomainImpl implements EjbOwnSecurityDomain {

    public static final String AUTHORIZED_ROLE = "admin";
    public static final String SECURITY_DOMAIN = "ejbOwnSecurityDomain";

    public static final String SAY_HELLO = "Hello from EjbOwnSecurityDomain";

    @Override
    @RolesAllowed(AUTHORIZED_ROLE)
    public String sayHello() {
        return SAY_HELLO;
    }

}
