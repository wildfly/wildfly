/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.injection.ejbref;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that lifecycle methods defined on classes in a different module to the component class
 * are called.
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals("hello", remote.hello());

    }

    @Test
    public void testEjbRefLocalLookup() throws Exception {
        final LookupBean bean = (LookupBean) iniCtx.lookup("java:module/LookupBean");
        LocalHomeInterface home = bean.doLookupLocal();
        LocalInterface remote = home.create();
        Assertions.assertEquals("hello", remote.hello());

    }

    @Test
    public void testInjection() throws Exception {
        CtxInjectionTester session = (CtxInjectionTester) iniCtx.lookup("java:module/CtxInjectionTesterBean");
        try {
            session.checkInjection();
        } catch (CtxInjectionTester.FailedException e) {
            Assertions.fail("SessionContext not injected");
        }
    }
}
