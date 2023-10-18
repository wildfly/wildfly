/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil.MESSAGE_DRIVEN;

/**
 * Tests management resources exposed by EJBs in a jar deployment packaged inside an EAR.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbJarInEarRuntimeResourcesMDBTestCase extends EjbJarRuntimeResourceTestBase {

    public static final String EAR_NAME = "ejb-management.ear";

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
        final JavaArchive jar = getEJBJar();
        jar.addClasses(ManagedMDB.class, NoTimerMDB.class);
        jar.addAsManifestResource(EjbJarRuntimeResourcesTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        ear.addAsModule(jar);
        return ear;
    }

    public EjbJarInEarRuntimeResourcesMDBTestCase() {
        super(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, EAR_NAME),
                PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT, JAR_NAME)));
    }

    @Test
    public void testMDB() throws Exception {
        testComponent(MESSAGE_DRIVEN, ManagedMDB.class.getSimpleName(), true);
    }

    @Test
    public void testNoTimerMDB() throws Exception {
        testComponent(MESSAGE_DRIVEN, NoTimerMDB.class.getSimpleName(), false);
    }
}
