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
import org.junit.runner.RunWith;

/**
 * Tests management resources exposed by EJBs in a root-level jar deployment.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbJarRuntimeResourcesTestCase extends EjbJarRuntimeResourceTestBase {
    static final PathAddress BASE_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, JAR_NAME));

    @Deployment
    public static Archive<?> deploy() {
        return getEJBJar();
    }

    public EjbJarRuntimeResourcesTestCase() {
        super(BASE_ADDRESS);
    }
}
