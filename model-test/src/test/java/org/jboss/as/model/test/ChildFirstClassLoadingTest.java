/*
 * Copyright (C) 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file
 * in the distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.model.test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.regex.Pattern;

import org.jboss.as.model.test.api.SingleChildFirst1;
import org.jboss.as.model.test.api.SingleChildFirst2;
import org.jboss.as.model.test.api.SingleParentFirst;
import org.jboss.as.model.test.api.Welcome;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class ChildFirstClassLoadingTest {

    public ChildFirstClassLoadingTest() {
    }

    @Test
    public void testWithoutExclusion() throws Exception {
        URLClassLoader parent = new URLClassLoader(new URL[]{ChildFirstClassLoadingTest.class.getResource("parent.jar")}, this.getClass().getClassLoader());
        parent.loadClass("org.jboss.as.model.test.parent.WelcomeParent");
        ChildFirstClassLoader child = new ChildFirstClassLoader(parent, new HashSet<Pattern>(), new HashSet<Pattern>(), null, new URL[]{ChildFirstClassLoadingTest.class.getResource("child.jar")});
        Class<?> welcomeParent = child.loadClass("org.jboss.as.model.test.parent.WelcomeParent");
        Class<?> welcomeChild = child.loadClass("org.jboss.as.model.test.child.WelcomeChild");
        Class<?> welcome = this.getClass().getClassLoader().loadClass("org.jboss.as.model.test.api.Welcome");
        welcomeChild.asSubclass(welcome);
        welcomeParent.asSubclass(welcome);
    }

    @Test(expected = NoClassDefFoundError.class)
    public void testWithExclusion() throws Exception {
        URLClassLoader parent = new URLClassLoader(new URL[]{ChildFirstClassLoadingTest.class.getResource("parent.jar")}, this.getClass().getClassLoader());
        parent.loadClass("org.jboss.as.model.test.parent.WelcomeParent");
        ChildFirstClassLoader child = new ChildFirstClassLoader(parent, new HashSet<Pattern>(), new HashSet<Pattern>(),
                SingleClassFilter.createFilter(Welcome.class),
                new URL[]{ChildFirstClassLoadingTest.class.getResource("child.jar")});
        Class<?> welcomeParent = child.loadClass("org.jboss.as.model.test.parent.WelcomeParent");
        Class<?> welcomeChild = child.loadClass("org.jboss.as.model.test.child.WelcomeChild");
    }

    @Test
    public void testSingleClassFromTests() throws Exception {
        ChildFirstClassLoaderBuilder builder = new ChildFirstClassLoaderBuilder(false);
        builder.addSingleChildFirstClass(SingleChildFirst1.class, SingleChildFirst2.class);
        ClassLoader loader = builder.build();
        Class<?> clazz = loader.loadClass(SingleChildFirst1.class.getName());
        Assert.assertSame(loader, clazz.getClassLoader());
        clazz = loader.loadClass(SingleChildFirst2.class.getName());
        Assert.assertSame(loader, clazz.getClassLoader());
        clazz = loader.loadClass(SingleParentFirst.class.getName());
        Assert.assertNotSame(loader, clazz.getClassLoader());
        clazz = loader.loadClass(ChildFirstClassLoadingTest.class.getName());
        Assert.assertNotSame(loader, clazz.getClassLoader());
    }
}
