/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.ejb.jbossall;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that jboss-ejb3.xml is picked up from jboss-all.xml
 */
@RunWith(Arquillian.class)
public class JBossAllEjbJarTestCase {
    @ArquillianResource
    private InitialContext ctx;

    @Deployment(name = "test")
    public static Archive<?> deploy() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "jbossAllEjb.jar");
        jar.addPackage(JBossAllEjbJarTestCase.class.getPackage());
        jar.addAsManifestResource(JBossAllEjbJarTestCase.class.getPackage(), "jboss-all.xml", "jboss-all.xml");
        return jar;
    }

    @Test
    public void testWeldParsing() throws Exception {
        //if the lookup passes then we know the bean was added
        ctx.lookup("java:module/" + JBossAllBean.class.getSimpleName());

    }

}
