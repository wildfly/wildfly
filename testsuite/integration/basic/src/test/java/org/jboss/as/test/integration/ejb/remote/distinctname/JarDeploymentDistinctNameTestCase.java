/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.distinctname;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

/**
 * Tests that the distinct-name configured in the jboss-ejb3.xml of an EJB jar deployment is taken into
 * consideration during remote EJB invocations.
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JarDeploymentDistinctNameTestCase extends DistinctNameTestCase {


    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "distinct-name-in-jboss-ejb3-xml";
    private static final String MODULE_NAME = "remote-ejb-distinct-name-jar-test-case";


    @Deployment(testable = false)
    public static JavaArchive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(JarDeploymentDistinctNameTestCase.class.getPackage());
        jar.addAsManifestResource(JarDeploymentDistinctNameTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
        return jar;
    }


    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected String getModuleName() {
        return MODULE_NAME;
    }

    @Override
    protected String getDistinctName() {
        return DISTINCT_NAME;
    }

}
