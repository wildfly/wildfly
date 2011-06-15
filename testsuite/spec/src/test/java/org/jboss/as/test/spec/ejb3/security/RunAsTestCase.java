/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.spec.ejb3.security;

import static org.jboss.as.test.spec.ejb3.security.Util.getCLMLoginContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.ejb.EJB;
import javax.security.auth.login.LoginContext;

import java.util.logging.Logger;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.spec.common.HttpRequest;
import org.jboss.as.test.spec.ejb3.security.base.WhoAmIBean;
import org.jboss.as.test.spec.ejb3.security.runas.EntryBean;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.util.Base64;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
public class RunAsTestCase {

    private static final Logger log = Logger.getLogger(RunAsTestCase.class.getName());

    @EJB(mappedName = "java:global/ejb3security/WhoAmIBean")
    private WhoAmI whoAmIBean;

    @EJB(mappedName = "java:global/ejb3security/EntryBean")
    private Entry entryBean;

    /*
     * isCallerInRole Scenarios with @RunAs Defined
     *
     * EJB 3.1 FR 17.2.5.2 isCallerInRole tests the principal that represents the caller of the enterprise bean,
     * not the principal that corresponds to the run-as security identity for the bean.
     */

    @Deployment
    public static Archive<?> runAsDeployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addPackage(WhoAmI.class.getPackage())
                .addPackage(WhoAmIBean.class.getPackage())
                .addPackage(EntryBean.class.getPackage())
                .addPackage(HttpRequest.class.getPackage())
                .addClass(Base64.class)
                .addAsResource("ejb3/security/users.properties", "users.properties")
                .addAsResource("ejb3/security/roles.properties", "roles.properties")
                .addAsWebInfResource("ejb3/security/web.xml", "web.xml");
        log.info(war.toString(true));
        return war;
    }

    @Test
    public void testAuthentication_TwoBeans() throws Exception {
        LoginContext lc = getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            String[] response = entryBean.doubleWhoAmI();
            assertEquals("user1", response[0]);
            assertEquals("anonymous", response[1]);
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testRunAsICIR_TwoBeans() throws Exception {
        LoginContext lc = getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            // TODO - Verify direct call to WhoAmIBean fails

            boolean[] response;
            response = entryBean.doubleDoIHaveRole("Users");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role1");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role2");
            assertFalse(response[0]);
            assertFalse(response[1]);
        } finally {
            lc.logout();
        }

        lc = getCLMLoginContext("user2", "password2");
        lc.login();
        try {
            // TODO - Verify direct call to WhoAmIBean succeeds

            boolean[] response;
            response = entryBean.doubleDoIHaveRole("Users");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role1");
            assertFalse(response[0]);
            assertFalse(response[1]);

            response = entryBean.doubleDoIHaveRole("Role2");
            assertTrue(response[0]);
            assertTrue(response[1]);
        } finally {
            lc.logout();
        }
    }
}
