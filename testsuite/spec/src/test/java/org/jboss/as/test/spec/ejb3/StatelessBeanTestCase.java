/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.spec.ejb3;

import java.util.logging.Logger;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.demos.ejb3.archive.SimpleInterceptor;
import org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionBean;
import org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionLocal;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testcase for testing the basic functionality of a EJB3 stateless session bean.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class StatelessBeanTestCase {

    private static final Logger log = Logger.getLogger(StatelessBeanTestCase.class.getName());
    
    @Deployment
    public static JavaArchive createDeployment() {
        // create the ejb jar
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-slsb-example.jar");
        jar.addPackage(SimpleStatelessSessionBean.class.getPackage());
        jar.addClass(SimpleInterceptor.class);
        log.info(jar.toString(true));
        return jar;
    }
    
    @EJB(mappedName="java:global/test/SimpleStatelessSessionBean!org.jboss.as.demos.ejb3.archive.SimpleStatelessSessionLocal")
    private SimpleStatelessSessionLocal localBean;

    /**
     * Test a basic invocation on a SLSB.
     *
     * @throws Exception
     */
    @Test
    public void testSLSB() throws Exception {
        String message = "Zzzzzzzz.....!";
        String echo = localBean.echo(message);
        String expectedEcho = SimpleInterceptor.class.getSimpleName() + "#" + SimpleStatelessSessionBean.class.getSimpleName() + "#" + "Echo " + message + " -- (1:Other, 2:Other, 3:Other)" ;
        Assert.assertEquals("Unexpected echo message received from stateless bean", expectedEcho, echo);
    }
}
