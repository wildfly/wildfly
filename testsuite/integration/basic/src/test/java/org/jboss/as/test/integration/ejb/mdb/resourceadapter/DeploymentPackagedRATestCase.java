/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.resourceadapter;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that a resource adapter packaged in a .ear can be used by a MDB as its resource adapter
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class DeploymentPackagedRATestCase {

    private static final String EAR_NAME = "ear-containing-rar";

    private static final String RAR_NAME = "rar-within-a-ear";

    private static final String EJB_JAR_NAME = "ejb-jar";

    @Deployment
    public static Archive createDeplyoment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear");

        final JavaArchive rar = ShrinkWrap.create(JavaArchive.class, RAR_NAME + ".rar");
        rar.addAsManifestResource(DeploymentPackagedRATestCase.class.getPackage(), "ra.xml", "ra.xml");

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, EJB_JAR_NAME + ".jar");
        ejbJar.addClasses(NonJMSMDB.class, DeploymentPackagedRATestCase.class);

        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, "common-lib.jar");
        libJar.addClasses(SimpleActivationSpec.class, SimpleResourceAdapter.class, SimpleMessageListener.class, ResourceAdapterDeploymentTracker.class);

        ear.addAsModule(rar);
        ear.addAsModule(ejbJar);
        ear.addAsLibrary(libJar);

        return ear;
    }

    /**
     * Tests that a RA deployed within the .ear is deployed and started successfully and it's endpoint
     * activation is invoked
     *
     * @throws Exception
     */
    @Test
    public void testRADeployment() throws Exception {
        Assert.assertTrue("Resource adapter deployed in the .ear was not started", ResourceAdapterDeploymentTracker.INSTANCE.wasEndpointStartCalled());
        Assert.assertTrue("Resource adapter's endpoint was not activated", ResourceAdapterDeploymentTracker.INSTANCE.wasEndpointActivationCalled());
    }
}
