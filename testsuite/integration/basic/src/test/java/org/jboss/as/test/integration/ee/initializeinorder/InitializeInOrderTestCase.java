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
package org.jboss.as.test.integration.ee.initializeinorder;

import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that <initialize-in-order> works as expected.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class InitializeInOrderTestCase {

    private static final List<String> initOrder = new ArrayList<String>();

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "init.ear");
        ear.addAsResource(InitializeInOrderTestCase.class.getPackage(), "application.xml", "application.xml");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(MyEjb.class, InitializeInOrderTestCase.class);
        ear.addAsModule(jar);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war");
        war.addClass(MyServlet.class);
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testPostConstruct() throws NamingException {
        Assert.assertEquals(2, initOrder.size());
        Assert.assertEquals("MyServlet", initOrder.get(0));
        Assert.assertEquals("MyEjb", initOrder.get(1));
    }

    public static void recordInit(final String clazz) {
        initOrder.add(clazz);
    }

}
