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
