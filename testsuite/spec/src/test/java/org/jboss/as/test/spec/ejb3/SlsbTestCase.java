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

package org.jboss.as.test.spec.ejb3;

import java.util.logging.Logger;

import javax.ejb.EJB;

import junit.framework.Assert;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for simple SLSBs
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
@RunWith(Arquillian.class)
public class SlsbTestCase {

    /**
     * Logger
     */
    private static final Logger log = Logger.getLogger(SlsbTestCase.class.getName());

    /**
     * Deployment
     *
     * @return
     */
    @Deployment
    public static JavaArchive getDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "slsb.jar").addClasses(GreeterCommonBusiness.class,
                GreeterSlsb.class);
        log.info(archive.toString(true));
        return archive;
    }

    /**
     * EJB under test
     */
    @EJB(mappedName = "java:global/test/GreeterSlsb!org.jboss.as.test.spec.ejb3.GreeterCommonBusiness")
    private GreeterCommonBusiness slsb;

    /**
     * Ensures the EJB may be invoked
     */
    @Test
    public void testSlsb() {
        Assert.assertNotNull("SLSB was not injected", slsb);
        final String name = "ALR";
        final String result = slsb.greet(name);
        log.info("Got result: " + result);
        Assert.assertEquals("SLSB invocation did not return as expected", GreeterCommonBusiness.PREFIX + name, result);
    }

}
