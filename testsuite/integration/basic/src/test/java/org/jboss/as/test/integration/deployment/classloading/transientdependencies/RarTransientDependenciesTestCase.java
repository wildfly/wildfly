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

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnectionFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a rar deployments transitive deps are made available to a deployment that references the rar
 *
 * @author Stuart Douglas
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RarTransientDependenciesTestCase {

    /**
     * Creates a .ear which has the following packaging structure:
     * <p/>
     * rar-transient-dep.ear
     * |
     * |--- rardeployment.rar (RAR deployment)
     * |        |
     * |        |--- META-INF
     * |        |       |
     * |        |       |--- ra.xml
     * |        |       |
     * |        |       |--- MANIFEST.MF containing -> Class-Path: transient.jar (to point to the jar at the root of the .ear)
     * |
     * |
     * |--- referenceingwebapp.war (war deployment)
     * |        |
     * |        |
     * |        |--- org.jboss.as.test.integration.deployment.classloading.transientdependencies.Servlet (the servlet which tries to access the class in the transient.jar, when invoked)
     * |
     * |
     * |--- transient.jar (simple jar file and NOT a deployment. this jar is referenced via the Class-Path manifest attribute in the .rar and as a result, is transitively expected to be available for access in the .war)
     * |        |
     * |        |--- org.jboss.as.test.integration.deployment.classloading.transientdependencies.JarClass
     *
     * @return
     */
    @Deployment
    public static Archive<?> createDeployment() {

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "rar-transient-dep.ear");

        final ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "rardeployment.rar");
        JavaArchive jar1 = ShrinkWrap.create(JavaArchive.class, "main.jar");
        jar1.addPackage(ValidConnectionFactory.class.getPackage());
        rar.add(jar1, "/", ZipExporter.class);
        rar.addAsManifestResource(RarTransientDependenciesTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        rar.addAsManifestResource(RarTransientDependenciesTestCase.class.getPackage(), "ra.xml", "ra.xml");

        // add the .rar to the .ear
        ear.addAsModule(rar);

        // now create the transient.jar with the JarClass.class
        final JavaArchive transientDepJar = ShrinkWrap.create(JavaArchive.class, "transient.jar");
        transientDepJar.addClass(JarClass.class);
        // add this jar to the root of the .ear as simple jar and not as a module. the .rar in the .ear will have a Class-Path reference
        // to this jar
        ear.add(transientDepJar, "/", ZipExporter.class);

        // finally create and add the .war to the .ear
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "referenceingwebapp.war");
        war.addClasses(RarTransientDependenciesTestCase.class, Servlet.class);
        war.addAsWebInfResource(RarTransientDependenciesTestCase.class.getPackage(), "web.xml", "web.xml");
        ear.addAsModule(war);

        return ear;
    }

    /**
     * Tests that a class available in a jar, which is added as a Class-Path manifest attribute of a .rar is available for access from within a class
     * in a .war deployment, belonging to the same .ear top level deployment. This tests section 8.3 of Java EE6 spec
     *
     * @param baseUrl
     * @throws Exception
     */
    @Test
    public void testTransitiveDependencyAccessViaRar(@ArquillianResource URL baseUrl) throws Exception {
        final String url = "http://" + baseUrl.getHost() + ":" + baseUrl.getPort() + "/referenceingwebapp/servlet?className=" + JarClass.class.getName();
        final String res = HttpRequest.get(url, 2, TimeUnit.SECONDS);
        Assert.assertEquals(Servlet.SUCCESS, res);

    }

}
