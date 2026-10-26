/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.injection.injectiontarget;

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
 *
 */
@ExtendWith(ArquillianExtension.class)
public class EjbRefInjectionTargetTestCase {

    private static final String ARCHIVE_NAME = "ejbreftest.jar";


    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        jar.addPackage(EjbRefInjectionTargetTestCase.class.getPackage());
        jar.addAsManifestResource(EjbRefInjectionTargetTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testEjbRefLookup() throws Exception {
        final InjectingBean bean = (InjectingBean) iniCtx.lookup("java:module/" + InjectingBean.class.getSimpleName());
        Assertions.assertEquals(InjectedBean.class.getName(), bean.getName());

    }
}
