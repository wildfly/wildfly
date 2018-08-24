/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.ejb.stateful.serialization.deserialization;

import org.jboss.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Properties;

@WebServlet(name = "DefaultConstructorServlet", urlPatterns = DefaultConstructorServlet.URL_PATTERN)
public class DefaultConstructorServlet extends HttpServlet {

    private static final long serialVersionUID = -6189108351718996259L;

    private static Logger log = Logger.getLogger(DefaultConstructorServlet.class.getName());

    public static final String URL_PATTERN = "DefaultConstructorServlet";

    /**
     * Test Scenarios:
     * test-1: _mutable_one defaults to true (set in constructor), we send true on client side
     * test-2: _mutable_two defaults to false (set in constructor), we send false on client side
     * test-3: _mutable_three defaults to true (set in constructor), but we change it to false, we send false on client side
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.trace("========== doGet method of the DefaultConstructorServlet called ==========");

        Context context = null;
        HelloRemote remote = null;
        Response ejbResponse = null;

        boolean init_mutable_one;
        boolean init_mutable_two;
        boolean init_mutable_three;

        try {
            LEContact leContact = new LEContact("test contact");

            leContact.set_mutable_three(false);

            init_mutable_one = leContact.is_mutable_one();
            init_mutable_two = leContact.is_mutable_two();
            init_mutable_three = leContact.is_mutable_three();
            log.trace("Initial mutable_one value on EJB client: " + init_mutable_one + " *****");
            log.trace("Initial mutable_two value on EJB client: " + init_mutable_two + " *****");
            log.trace("Initial mutable_three value on EJB client: " + init_mutable_three + " *****");

            context = new InitialContext(loadProperties());
            String ejbName = getEjb();
            remote = (HelloRemote) context.lookup(ejbName);

            ejbResponse = remote.hello(leContact);

            log.trace("***** values received on EJB server:" + ejbResponse.getRequestData() + " *****");

            Writer writer = response.getWriter();
            writer.write(ejbResponse.getRequestData());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != context) {
                try {
                    context.close();
                    remote = null;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getEjb() {
        final String appName = DefaultConstructorNonSerializedParentTestCase.DEPLOYMENT;
        final String moduleName = DefaultConstructorNonSerializedParentTestCase.MODULE;
        final String distinctName = "";
        final String beanName = HelloBean.class.getSimpleName();
        final String interfaceName = HelloRemote.class.getName();
        final String name = "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + interfaceName;

        return name;
    }

    public Hashtable loadProperties() {
        Properties prop = new Properties();
        String propFileName = "jndi.properties";
        Hashtable jndiProperties = new Hashtable();

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
            }

            jndiProperties.put("java.naming.factory.url.pkgs", prop.get("java.naming.factory.url.pkgs"));
            jndiProperties.put("java.naming.factory.initial", prop.get("java.naming.factory.initial"));
            jndiProperties.put("java.naming.provider.url", prop.get("java.naming.provider.url"));
            jndiProperties.put("java.naming.security.principal", prop.get("java.naming.security.principal"));
            jndiProperties.put("java.naming.security.credentials", prop.get("java.naming.security.credentials"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jndiProperties;
    }
}
