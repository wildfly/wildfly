/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.compat.jpa.hibernate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


import java.io.File;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verify that application deployments can include their own copy of Hibernate jars
 */

@RunWith(Arquillian.class)
public class HibernateJarsInDeploymentTestCase {

    private static final String ARCHIVE_NAME = "HibernateJarsInDeploymentTestCase";

    private static void addHibernate42xJars(EnterpriseArchive ear) {
        final String basedir = System.getProperty("basedir");
        final String testdir = basedir + File.separatorChar + "target" + File.separatorChar + "test-libs";

        File hibernatecore = new File(testdir, "hibernate-core.jar");
        File hibernatejava8 = new File(testdir, "hibernate-java8.jar");
        File hibernateannotations = new File(testdir, "hibernate-commons-annotations.jar");
        File hibernateentitymanager = new File(testdir, "hibernate-entitymanager.jar");
        File jipi = new File(testdir,"jipijapa-hibernate5.jar");
        File dom4j = new File(testdir, "dom4j.jar");
        File antlr = new File(testdir, "antlr.jar");
        File classmate = new File(testdir, "classmate.jar");
        ear.addAsLibraries(
                hibernatecore,
                hibernateannotations,
                hibernatejava8,
                hibernateentitymanager,
                dom4j,
                antlr,
                classmate,
                jipi
        );
    }

    @Deployment
    public static Archive<?> deploy() throws Exception {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ARCHIVE_NAME + ".ear");
        addHibernate42xJars(ear);

        JavaArchive lib = ShrinkWrap.create(JavaArchive.class, "beans.jar");
        lib.addClasses(SFSB1.class, HibernateJarsInDeploymentTestCase.class);
        lib.addAsManifestResource(HibernateJarsInDeploymentTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        ear.addAsManifestResource(HibernateJarsInDeploymentTestCase.class.getPackage(), "permissions.xml", "permissions.xml");
        ear.addAsManifestResource(new StringAsset("Dependencies: org.javassist export, org.jboss.jandex\n"), "MANIFEST.MF");
        ear.addAsModule(lib);

        lib = ShrinkWrap.create(JavaArchive.class,"lib.jar");
        lib.addClass(LibClass.class);
        ear.addAsLibrary(lib);
        return ear;
    }

    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + "beans/" + beanName + "!" + interfaceType.getName()));
    }

    @Test
    public void verifyPackagedHibernateJarsUsed() throws Exception {
        SFSB1 sfsb1 = lookup("SFSB1", SFSB1.class);
        assertNotNull(sfsb1.getEmImpl());
        // verify that the Hibernate ORM classloader == ear classloader
        // fail if the static Hibernate module classloader is used instead of Hibernate jars in application
        assertEquals(LibClass.class.getClassLoader(), sfsb1.getEmImpl().getClass().getClassLoader());
    }
}
