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
import org.junit.runner.RunWith;

/**
 * Tests management resources exposed by EJBs in a jar deployment packaged inside an EAR.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbJarInEarRuntimeResourcesTestCase extends EjbJarRuntimeResourceTestBase {

    public static final String EAR_NAME = "ejb-management.ear";

    @Deployment
    public static Archive<?> deploy() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
        ear.addAsModule(getEJBJar());
        return ear;
    }

    public EjbJarInEarRuntimeResourcesTestCase() {
        super(PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, EAR_NAME),
                PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT, JAR_NAME)));
    }
}
