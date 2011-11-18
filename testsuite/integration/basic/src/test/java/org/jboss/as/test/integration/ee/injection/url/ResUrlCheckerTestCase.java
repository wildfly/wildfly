/*
 * JBoss, Home of Professional Open Source
 * Copyright 2007, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.ee.injection.url;

import java.net.URL;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (ejbthree-989) to AS7 [JIRA JBQA-5483].
 * 
 * Test to see if resources of type URL work.
 * 
 * @author Carlo de Wolf, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class ResUrlCheckerTestCase {
    private static final Logger log = Logger.getLogger(ResUrlCheckerTestCase.class);

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "url-injection-test.jar")
                .addPackage(ResUrlCheckerTestCase.class.getPackage());
                jar.addAsManifestResource(ResUrlCheckerTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        log.info(jar.toString(true));
        return jar;
    }

    private InitialContext getInitialContext() {
        return ctx;
    }

    private ResUrlChecker lookupBean() throws NamingException {
        return (ResUrlChecker) getInitialContext().lookup("java:module/ResUrlCheckerBean");
    }
    
    @Ignore("AS7-2744") // TODO: @see ResUrlCheckerBean and uncomment @Resource annotations
    @Test
    public void test1() throws Exception {
        ResUrlChecker bean = lookupBean();
        // defined in ResUrlCheckerBean
        URL expected = new URL("http://localhost");
        URL actual = bean.getURL1();
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void test2() throws Exception {
        ResUrlChecker bean = lookupBean();
        // defined in jboss.xml
        URL expected = new URL("http://localhost/url2");
        URL actual = bean.getURL2();
        Assert.assertEquals(expected, actual);
    }

    @Ignore("AS7-2744")
    @Test
    public void test3() throws Exception {
        ResUrlChecker bean = lookupBean();
        // defined in ResUrlCheckerBean
        URL expected = new URL("http://localhost/url3");
        URL actual = bean.getURL3();
        Assert.assertEquals(expected, actual);
    }
}
