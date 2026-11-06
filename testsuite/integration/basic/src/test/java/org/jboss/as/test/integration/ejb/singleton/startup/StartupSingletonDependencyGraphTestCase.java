/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Singleton startup dependency.
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
public class StartupSingletonDependencyGraphTestCase {
    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb3-singleton-startup.jar");
        jar.addPackage(StartupSingletonDependencyGraphTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testStartupSingletonBeanAccess() throws Exception {
        final SingletonBeanRemoteView singletonBean = InitialContext.doLookup("java:module/" + SingletonB.class.getSimpleName());
        singletonBean.doSomething();
        final String message = "Hello world!";
        final String reply = singletonBean.echo(message);
        Assertions.assertEquals(message, reply, "Unexpected reply from singleton bean");
    }

}
