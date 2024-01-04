/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup.postconstruct;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for AS7-[3855|2785]: Do not call @PostConstruct multiple times on @Startup @Singleton bean.
 *
 * @author Jan Martiska / jmartisk@redhat.com
 */
@RunWith(Arquillian.class)
public class SinglePostConstructInvocationTestCase {

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "single-postconstruct-invocation-test.jar");
        archive.addClasses(Client.class, Controller.class);
        return archive;
    }

    @Test
    public void doTest() throws Exception {
        InitialContext ctx = new InitialContext();
        Controller controller = (Controller) ctx.lookup("java:module/Controller");
        Assert.assertEquals(1, controller.getPostConstructInvocationCounter());
        ctx.close();
    }
}
