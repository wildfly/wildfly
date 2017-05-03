/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware, Inc., and individual contributors
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
package org.wildfly.test.integration.elytron.audit;

import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;


/**
 * Abstract class for Elytron Audit Logging tests. It provides a deployment with {@link SimpleServlet} and a couple of helper
 * methods.
 *
 * @author Jan Tymel
 */
public abstract class AbstractAuditLogTestCase {

    @ArquillianResource
    protected URL url;

    protected static final String SUCCESSFUL_AUTH_EVENT = "SecurityPermissionCheckSuccessfulEvent";
    protected static final String UNSUCCESSFUL_AUTH_EVENT = "SecurityAuthenticationFailedEvent";

    protected static final String USER = "user1";
    protected static final String UNKNOWN_USER = "unknown-user";
    protected static final String PASSWORD = "password1";

    private static final String NAME = "AuditlogTestCase";

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleServlet.class)
                .addAsWebInfResource(FileAuditLogTestCase.class.getPackage(), "BasicAuthentication-web.xml", "web.xml");
    }

    protected static void setDefaultEventListenerOfApplicationDomain(CLIWrapper cli) {
        setEventListenerOfApplicationDomain(cli, "local-audit");

    }
    protected static void setEventListenerOfApplicationDomain(CLIWrapper cli, String auditlog) {
        cli.sendLine(String.format(
                "/subsystem=elytron/security-domain=ApplicationDomain:write-attribute(name=security-event-listener,value=%s)",
                auditlog));
    }
}
