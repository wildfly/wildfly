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

package org.jboss.as.test.integration.ejb.injection.ejbref;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that lifecycle methods defined on classes in a different module to the component class
 * are called.
 */
@RunWith(Arquillian.class)
public class EjbRefTestCase {

    private static final String ARCHIVE_NAME = "ejbreftest.jar";


    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        jar.addPackage(EjbRefTestCase.class.getPackage());
        jar.addAsManifestResource(EjbRefTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testEjbRefLookup() throws Exception {
        final LookupBean bean = (LookupBean) iniCtx.lookup("java:module/LookupBean");
        HomeInterface home = bean.doLookupRemote();
        RemoteInterface remote = home.create();
        Assert.assertEquals("hello", remote.hello());

    }

    @Test
    public void testEjbRefLocalLookup() throws Exception {
        final LookupBean bean = (LookupBean) iniCtx.lookup("java:module/LookupBean");
        LocalHomeInterface home = bean.doLookupLocal();
        LocalInterface remote = home.create();
        Assert.assertEquals("hello", remote.hello());

    }

    @Test
    public void testInjection() throws Exception {
        CtxInjectionTester session = (CtxInjectionTester) iniCtx.lookup("java:module/CtxInjectionTesterBean");
        try {
            session.checkInjection();
        } catch (CtxInjectionTester.FailedException e) {
            Assert.fail("SessionContext not injected");
        }
    }
}
