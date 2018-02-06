/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.test.integration.domain.mixed.eap640;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.mixed.MixedDomainTestSuite;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.ejb.Counter;
import org.jboss.as.test.integration.domain.mixed.ejb.CounterBean;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.operations.common.Util.createAddOperation;
import static org.jboss.as.controller.operations.common.Util.createEmptyOperation;

/**
 * Tests if clustered EJB deployment deployed on mixed domain (EAP 7.1 + EAP 6.4) is successfully deployed
 * Test for [ JBEAP-13295 ].
 *
 * @author Daniel Cihak
 */
@Version(Version.AsVersion.EAP_6_4_0)
public class MixedDomainClusteredEjbDeployment640TestCase extends MixedDomainTestSuite {

    private static final String ARCHIVE_FILE_NAME = "ClusteredEjbDeployment.jar";
    private JavaArchive clusteredEjbDeployment;
    private File tmpDir;
    private static DomainTestSupport testSupport;
    private static final PathAddress ROOT_DEPLOYMENT_ADDRESS = PathAddress.pathAddress(DEPLOYMENT, ARCHIVE_FILE_NAME);
    private static final PathAddress MAIN_SERVER_GROUP_ADDRESS = PathAddress.pathAddress(SERVER_GROUP, "main-server-group");
    private static final PathAddress MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS = MAIN_SERVER_GROUP_ADDRESS.append(DEPLOYMENT, ARCHIVE_FILE_NAME);

    @BeforeClass
    public static void beforeClass() {
        MixedDomain640HAEAP6TestSuite.initializeDomain();
        testSupport =  MixedDomainTestSuite.getSupport(MixedDomainClusteredEjbDeployment640TestCase.class);
    }

    @Before
    public void setupDomain() {
        clusteredEjbDeployment = ShrinkWrap.create(JavaArchive.class, ARCHIVE_FILE_NAME);
        clusteredEjbDeployment.addClasses(Counter.class, CounterBean.class);
        tmpDir = new File("target/deployments/" + this.getClass().getSimpleName());
        new File(tmpDir, "archives").mkdirs();
        clusteredEjbDeployment.as(ZipExporter.class).exportTo(new File(tmpDir, "archives/" + ARCHIVE_FILE_NAME), true);
    }

    @Test
    public void testClusteredEjbDeployment() throws Exception {
        ModelNode content = new ModelNode();
        content.get("archive").set(true);
        content.get("path").set(new File(tmpDir, "archives/" + ARCHIVE_FILE_NAME).getAbsolutePath());
        ModelNode composite = createDeploymentOperation(content, MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        ModelNode response = executeOnMaster(composite);
        Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());

        composite = createUnDeploymentOperation(MAIN_SERVER_GROUP_DEPLOYMENT_ADDRESS);
        response = executeOnMaster(composite);
        Assert.assertEquals(response.toString(), "success", response.get("outcome").asString());
    }

    private ModelNode createDeploymentOperation(ModelNode content, PathAddress... serverGroupAddressses) {
        ModelNode composite = createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        ModelNode step1 = steps.add();
        step1.set(createAddOperation(ROOT_DEPLOYMENT_ADDRESS));
        step1.get(CONTENT).add(content);
        for (PathAddress serverGroup : serverGroupAddressses) {
            ModelNode sg = steps.add();
            sg.set(createAddOperation(serverGroup));
            sg.get(ENABLED).set(true);
        }
        return composite;
    }

    private ModelNode createUnDeploymentOperation(PathAddress... serverGroupAddressses) {
        ModelNode composite = createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        for (PathAddress serverGroup : serverGroupAddressses) {
            ModelNode undeploy = steps.add();
            undeploy.set(Operations.createOperation(UNDEPLOY, serverGroup.toModelNode()));
        }
        return composite;
    }

    private ModelNode executeOnMaster(ModelNode op) throws IOException {
        return testSupport.getDomainMasterLifecycleUtil().getDomainClient().execute(op);
    }
}
