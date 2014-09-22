/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author baranowb
 *
 */
public class ServiceOverlayTestCaseBase {
    @ArquillianResource
    protected ManagementClient managementClient;

    @ArquillianResource
    protected Deployer deployer;

    public static Archive<?> createTestArchive(final Class test){
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.DEPLOYMENT_JAR_NAME_COUNTER);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        
        
        jar.addClasses(TestimonyEJB.class);
        jar.addClasses(test, ServiceOverlayTestCaseBase.class, SetupModuleServerSetupTask.class, OverlayUtils.class);

        //weird
        jar.addClasses(MgmtOperationException.class,ManagementOperations.class);
        return jar;
    }
    
    public static Archive<?> createOverlayedArchive(boolean initialInterceptor){
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, Constants.DEPLOYMENT_JAR_NAME_OVERLAYED);
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        if (initialInterceptor) {
            jar.addAsManifestResource(new StringAsset(DeployedInterceptor.class.getName()),
                    "services/org.jboss.ejb.client.EJBClientInterceptor");
        }
        jar.addAsManifestResource(new StringAsset(
                "<jboss-ejb-client xmlns:xsi=\"urn:jboss:ejb-client:1.2\" xsi:noNamespaceSchemaLocation=\"jboss-ejb-client_1_2.xsd\">\n"+
                "    <client-context/>\n"+
                "</jboss-ejb-client>\n"
                ),
                "jboss-ejb-client.xml");
        jar.addClass(SimpleEJB.class);
        jar.addClasses(DeployedInterceptor.class, OverlayedInterceptor.class);
        return jar;
    }
    
    
    
}
