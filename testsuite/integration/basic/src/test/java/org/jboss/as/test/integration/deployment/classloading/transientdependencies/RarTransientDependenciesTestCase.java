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
package org.jboss.as.test.integration.deployment.classloading.transientdependencies;

import org.jboss.as.test.integration.beanvalidation.jca.ra.ValidConnectionFactory;

import junit.framework.Assert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a rar deployments transitive deps are made availble to a deployment that references the rar
 *
 * @author Stuart Douglas
 *
 */
@RunWith(Arquillian.class)
public class RarTransientDependenciesTestCase {

    @Deployment(name="jar", order=1)
    public static Archive<?> jar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "transient.jar");
        jar.addClass(JarClass.class);
        return jar;
    }

    @Deployment(name="rar", order=2)
    public static Archive<?> rar() {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "rardeployment.rar");
        JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "main.jar");
        jar1.addPackage(ValidConnectionFactory.class.getPackage());
        rar.add(jar1, "/", ZipExporter.class);

        rar.addAsManifestResource(new StringAsset(
                "<jboss-deployment-structure>" +
                        "<deployment>" +
                        "<dependencies>" +
                        "<module name=\"deployment.transient.jar\" />" +
                        "</dependencies>" +
                        "</deployment>" +
                        "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        rar.addAsManifestResource("jca/beanvalidation/ra.xml", "ra.xml");
        return rar;
    }

    @Deployment(name="war", order=3)
    public static Archive<?> war() {
        WebArchive war = ShrinkWrap.create(WebArchive.class,"referenceingwebapp.war");
        war.addClasses(RarTransientDependenciesTestCase.class);
        war.addAsWebInfResource(new StringAsset(
                "<jboss-deployment-structure>" +
                        "<deployment>" +
                        "<dependencies>" +
                        "<module name=\"deployment.rardeployment.rar\" />" +
                        "</dependencies>" +
                        "</deployment>" +
                        "</jboss-deployment-structure>"),
                "jboss-deployment-structure.xml");
        return war;
    }

    @Test
    @OperateOnDeployment("war")
    public void testRarClassLoading() {
        Assert.assertTrue(JarClass.class.getClassLoader().toString().contains("transient.jar"));
    }

}
