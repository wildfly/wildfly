/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.deployment.classloading.ear;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class EarClassLoadingTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        JavaArchive libJar = ShrinkWrap.create(JavaArchive.class);
        libJar.addClass(TestAA.class);
        war.addAsLibraries(libJar);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class);
        ear.addAsModule(war);
        JavaArchive earLib = ShrinkWrap.create(JavaArchive.class);
        earLib.addClass(TestBB.class);
        earLib.addClass(EarClassLoadingTestCase.class);
        ear.addAsLibraries(earLib);
        return ear;
    }

    @Test(expected = ClassNotFoundException.class)
    public void testWebInfLibNotAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestAA");
    }

    @Test
    public void testLibAccessible() throws ClassNotFoundException {
        loadClass("org.jboss.as.test.integration.deployment.classloading.ear.TestBB");
    }

    private static Class<?> loadClass(String name) throws ClassNotFoundException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return Class.forName(name, false, cl);
        } else
            return Class.forName(name);
    }
}
