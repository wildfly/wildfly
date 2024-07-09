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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil.MESSAGE_DRIVEN;

/**
 * Tests management resources exposed by EJBs in a root-level jar deployment.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbJarRuntimeResourcesMDBTestCase extends EjbJarRuntimeResourceTestBase {
    static final PathAddress BASE_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, JAR_NAME));

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = getEJBJar();
        jar.addClasses(ManagedMDB.class, NoTimerMDB.class);
        jar.addAsManifestResource(EjbJarRuntimeResourcesTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    public EjbJarRuntimeResourcesMDBTestCase() {
        super(BASE_ADDRESS);
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
