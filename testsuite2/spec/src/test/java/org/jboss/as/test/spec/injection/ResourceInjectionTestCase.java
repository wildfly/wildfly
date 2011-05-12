/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.spec.injection;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.io.File;
import java.util.logging.Logger;


/**
 * User: jpai
 */
@RunWith(Arquillian.class)
public class ResourceInjectionTestCase {

    private static final Logger logger = Logger.getLogger(ResourceInjectionTestCase.class.getName());

    @EJB
    private SimpleSLSB slsb;

    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "resource-injection-test.jar");
        jar.addPackage(SimpleSLSB.class.getPackage());
        logger.info(jar.toString(true));
        return jar;
    }

    @Test
    public void testResourceInjectionInEJB() {
        final String user = "Charlie Sheen";
        final String greeting = this.slsb.sayHello(user);
        Assert.assertEquals("Unepxected greeting received from bean", CommonBean.HELLO_GREETING_PREFIX + user, greeting);

//        Class<?> invokedBusinessInterface = this.slsb.getInvokedBusinessInterface();
//        Assert.assertEquals("Unexpected invoked business interface returned by bean", SimpleSLSB.class, invokedBusinessInterface);
    }
}
