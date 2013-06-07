/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.security.form;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.web.security.SecuredServlet;
import org.jboss.as.test.integration.web.security.WebSecurityPasswordBasedBase;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author wangchao
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebTestsSecurityDomainSetup.class)
@Category(CommonCriteria.class)
public class WebSecurityGenericHeaderTestCase extends AbstractWebSecurityFORMTestCase {

    @Deployment
    public static WebArchive deployment() throws Exception {

        WebArchive war = ShrinkWrap.create(WebArchive.class, "web-secure.war");
        war.addClasses(SecuredServlet.class);

        war.addAsWebResource(WebSecurityFORMTestCase.class.getPackage(), "login.jsp", "login.jsp");
        war.addAsWebResource(WebSecurityFORMTestCase.class.getPackage(), "error.jsp", "error.jsp");

        war.addAsWebInfResource(WebSecurityFORMTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");
        war.addAsWebInfResource(WebSecurityFORMTestCase.class.getPackage(), "web.xml", "web.xml");

        war.addAsResource(WebSecurityFORMTestCase.class.getPackage(), "users.properties", "users.properties");
        war.addAsResource(WebSecurityFORMTestCase.class.getPackage(), "roles.properties", "roles.properties");
        WebSecurityPasswordBasedBase.printWar(war);
        return war;
    }

    /**
     * <p>
     * Test usecases where the userid is sent via header and the session key is used as the password. To simplify testing, we
     * pass a password as part of the session key. In reality, there needs to be a login module that can take the username and
     * session key and validate.
     * </p>
     * @throws Exception if an error occurs when running the test.
     */
    @Test
    public void testGenericHeaderBaseAuth() throws Exception {
        // Siteminder usecase
        performHeaderAuth("sm_ssoid", "SMSESSION", "anil", "anil", 200);
        // Cleartrust usecase
        performHeaderAuth("ct-remote-user", "CTSESSION", "anil", "anil", 200);
        // Oblix usecase
        performHeaderAuth("HTTP_OBLIX_UID", "ObSSOCookie", "anil", "anil", 200);
    }
}
