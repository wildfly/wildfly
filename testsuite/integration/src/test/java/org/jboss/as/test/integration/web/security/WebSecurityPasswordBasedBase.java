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
package org.jboss.as.test.integration.web.security;

import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;

/**
 * Base class for web security tests that are based on passwords
 *
 * @author Anil Saldhana
 */
public abstract class WebSecurityPasswordBasedBase extends SecurityTest {

    protected final String URL = "http://localhost:8080/" + getContextPath() + "/secured/";

    /**
     * Base method to create a {@link WebArchive}
     *
     * @param name Name of the war file
     * @param servletClass a class that is the servlet
     * @param addProps should we add users.properties and roles.properties to war
     * @param webxml {@link URL} to the web.xml. This can be null
     * @return
     */
    public static WebArchive create(String name, Class<?> servletClass, boolean addProps, URL webxml) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, name);
        war.addClass(servletClass);
        war.addClass(SecurityTest.class);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        war.addAsResource(tccl.getResource("security/users.properties"), "users.properties");
        war.addAsResource(tccl.getResource("security/roles.properties"), "roles.properties");

        if (webxml != null) {
            war.setWebXML(webxml);
        }

        return war;
    }

    /**
     * Obtain the context path of the {@link WebArchive}
     *
     * @return
     */
    public abstract String getContextPath();

    /**
     * Print the contents of the {@link WebArchive}
     *
     * @param war
     */
    public static void printWar(WebArchive war) {
        System.out.println(war.toString(true));
    }

    /**
     * Test with user "anil" who has the right password and the right role to access the servlet
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedSuccessfulAuth() throws Exception {
        makeCall("anil", "anil", 200);
    }

    /**
     * <p>
     * Test with user "marcus" who has the right password but does not have the right role
     * </p>
     * <p>
     * Should be a HTTP/403
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void testPasswordBasedUnsuccessfulAuth() throws Exception {
        makeCall("marcus", "marcus", 403);
    }

    /**
     * Method that needs to be overridden with the HTTPClient code
     *
     * @param user username
     * @param pass password
     * @param expectedCode http status code
     * @throws Exception
     */
    protected abstract void makeCall(String user, String pass, int expectedCode) throws Exception;
}