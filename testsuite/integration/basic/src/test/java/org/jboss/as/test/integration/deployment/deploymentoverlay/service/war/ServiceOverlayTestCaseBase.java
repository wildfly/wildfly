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

package org.jboss.as.test.integration.deployment.deploymentoverlay.service.war;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.OverlayUtils;
import org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.SimpleEJB;
import org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.TestimonyEJB;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

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
    
    public static Archive<?> createOverlayedArchive(final boolean initialInterceptor){
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
    
    public static Archive<?> createWARWithOverlayedArchive(final boolean initialInterceptor){
        WebArchive war = ShrinkWrap.create(WebArchive.class, Constants.DEPLOYMENT_EAR_NAME_TESTER_WRAPPER);
        Archive<?> jar = createOverlayedArchive(initialInterceptor);
        war.addAsLibrary(jar);
        war.addAsManifestResource(new StringAsset(
                "<jboss-ejb-client xmlns:xsi=\"urn:jboss:ejb-client:1.2\" xsi:noNamespaceSchemaLocation=\"jboss-ejb-client_1_2.xsd\">\n"+
                "    <client-context/>\n"+
                "</jboss-ejb-client>\n"
                ),
                "jboss-ejb-client.xml");
        //TODO: if this is removed, upon overlay setup and reload, AS does not see OverlayedInterceptor!
        war.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, "
                + Constants.TEST_MODULE_NAME_FULL + "\n"), "MANIFEST.MF");
        return war;
    }
    
    protected void setupOverlay() throws Exception {

        // create overlay
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, Constants.OVERLAY);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        // add content
        op = new ModelNode();
        op.get(ModelDescriptionConstants.OP_ADDR).set(new ModelNode());
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.UPLOAD_DEPLOYMENT_BYTES);
        op.get(ModelDescriptionConstants.BYTES).set("org.jboss.as.test.integration.deployment.deploymentoverlay.service.jar.OverlayedInterceptor".getBytes());
        ModelNode result = ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        // link content to specific file
        op = new ModelNode();
        ModelNode addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, Constants.OVERLAY);
        addr.add(ModelDescriptionConstants.CONTENT, "META-INF/services/org.jboss.ejb.client.EJBClientInterceptor");
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        op.get(ModelDescriptionConstants.CONTENT).get(ModelDescriptionConstants.HASH).set(result);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);

        // add link
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, Constants.OVERLAY);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, Constants.DEPLOYMENT_JAR_NAME_OVERLAYED);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        System.err.println(""+op);
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT, Constants.DEPLOYMENT_JAR_NAME_OVERLAYED);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REDEPLOY);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }
    
    protected void removeOverlay() throws Exception {
        //TODO: proper cleanup
        try {
            removeContentItem(managementClient, Constants.OVERLAY, "META-INF/services/org.jboss.ejb.client.EJBClientInterceptor");
            removeDeploymentItem(managementClient, Constants.OVERLAY, Constants.DEPLOYMENT_JAR_NAME_OVERLAYED);

            ModelNode op = new ModelNode();
            op.get(ModelDescriptionConstants.OP_ADDR).set(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, Constants.OVERLAY);
            op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void removeContentItem(final ManagementClient managementClient, final String w, final String a) throws IOException, MgmtOperationException {
        final ModelNode op;
        final ModelNode addr;
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, w);
        addr.add(ModelDescriptionConstants.CONTENT, a);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }


    protected void removeDeploymentItem(final ManagementClient managementClient, final String w, final String a) throws IOException, MgmtOperationException {
        final ModelNode op;
        final ModelNode addr;
        op = new ModelNode();
        addr = new ModelNode();
        addr.add(ModelDescriptionConstants.DEPLOYMENT_OVERLAY, w);
        addr.add(ModelDescriptionConstants.DEPLOYMENT, a);
        op.get(ModelDescriptionConstants.OP_ADDR).set(addr);
        op.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.REMOVE);
        ManagementOperations.executeOperation(managementClient.getControllerClient(), op);
    }
    
}
