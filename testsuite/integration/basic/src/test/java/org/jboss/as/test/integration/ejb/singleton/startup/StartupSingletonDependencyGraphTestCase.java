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

package org.jboss.as.test.integration.ejb.singleton.startup;

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
 * Singleton startup dependency.
 * Part of the migration AS6->AS7 testsuite [JBQA-5275] - ejb3/singleton.
 *
 * @author Jaikiran Pai, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
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
        Assert.assertEquals("Unexpected reply from singleton bean", message, reply);
    }

}
