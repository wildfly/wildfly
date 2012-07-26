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

package org.jboss.as.test.integration.ee.injection.ztatic;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests static field and method injection for EE apps.
 *
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
public class StaticInjectionTestCase {

    private static final String DEPLOYMENT_NAME = "static-injection-test-du";

    @EJB(mappedName = "java:global/" + DEPLOYMENT_NAME + "/FieldTestEJB")
    FieldTestEJB fieldTestEJB;

    @EJB(mappedName = "java:global/" + DEPLOYMENT_NAME + "/MethodTestEJB")
    MethodTestEJB methodTestEJB;

    @Deployment
    public static WebArchive createFieldTestDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        war.addPackage(StaticInjectionTestCase.class.getPackage());
        war.addAsWebInfResource(StaticInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testStaticInjection() {
        Assert.assertTrue("Static field should not be injected", !fieldTestEJB.isStaticResourceInjected());
        Assert.assertTrue("Static method should not be injected", !methodTestEJB.isStaticResourceInjected());
    }
}
