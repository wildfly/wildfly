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

package org.jboss.as.test.integration.ejb.injection.implicitDependsOn;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ImplicitDependsOnTestCase {

    private static final Logger logger = Logger.getLogger(ImplicitDependsOnTestCase.class);
    public static final String IMPLICIT_DEPLOYMENT = "implicitDeployment";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static Archive createTestDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb-test.jar");
        jar.addClasses(StaticDataClass.class, ImplicitDependsOnTestCase.class);
        return jar;
    }

    @Deployment(managed = false, testable = false, name = IMPLICIT_DEPLOYMENT)
    public static Archive createImplicitDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "implicit-depends-on.jar");
        jar.addClasses(ServiceEjb.class, ShutdownSingleton.class);
        jar.addAsManifestResource(ImplicitDependsOnTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        jar.addAsManifestResource(new StringAsset("Dependencies: deployment.ejb-test.jar\n"), "MANIFEST.MF");
        return jar;
    }


    @Test
    public void testImplicitDependsOn() throws IOException {
        deployer.deploy(IMPLICIT_DEPLOYMENT);
        deployer.undeploy(IMPLICIT_DEPLOYMENT);
        Assert.assertEquals("hello", StaticDataClass.servletPreDestroyResult);
    }
}
