/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.directory.server.annotations.CreateTransport;

/**
 * A annotation used to define a KDC server configuration. This is only a copy of an original
 * {@link org.apache.directory.server.annotations.CreateKdcServer} annotation, which adds {@link #searchBaseDn()} attribute.
 * It's only a workaround for ApacheDS issue https://issues.apache.org/jira/browse/DIRKRB-85
 * 
 * @author Josef Cacek
 * @see KDCServerAnnotationProcessor
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ExtCreateKdcServer {
    /** The instance name */
    String name() default "DefaultKrbServer";

    /** The transports to use, default to LDAP */
    CreateTransport[] transports() default {};

    /** The default kdc realm */
    String primaryRealm() default "EXAMPLE.COM";

    String searchBaseDn() default "ou=users,dc=example,dc=com";

    /** The default kdc service principal */
    String kdcPrincipal() default "krbtgt/EXAMPLE.COM@EXAMPLE.COM";

    /** The maximum ticket lifetime. */
    long maxTicketLifetime() default 24 * 60 * 60 * 1000; //default = 1 day

    /** The maximum renewable lifetime. */
    long maxRenewableLifetime() default 7 * 24 * 60 * 60 * 1000; //default = 7 days
}