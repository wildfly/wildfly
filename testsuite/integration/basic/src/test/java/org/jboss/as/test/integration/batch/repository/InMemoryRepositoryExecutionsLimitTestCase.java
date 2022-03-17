/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.test.integration.batch.repository;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.SnapshotRestoreSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(InMemoryRepositoryExecutionsLimitTestCase.InMemoryRepositorySetup.class)
public class InMemoryRepositoryExecutionsLimitTestCase extends AbstractBatchTestCase {

    private static final String DEPLOYMENT_NAME = "jdbc-batch.war";

    @ArquillianResource
    private ServiceContainer serviceContainer;

    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive createNamedJdbcDeployment() {
        return createDefaultWar(DEPLOYMENT_NAME, InMemoryRepositoryExecutionsLimitTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class, JobRepositoryTestUtils.class,
                        InMemoryRepositorySetup.class)
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.msc,org.wildfly.security.manager,org.wildfly.extension.batch.jberet")
                                .exportAsString()));
    }

    @Test
    public void testGetJobExecutionsWithLimit() {
        JobRepositoryTestUtils.testGetJobExecutionsWithLimit(serviceContainer, "in-memory");
    }

    static class InMemoryRepositorySetup extends SnapshotRestoreSetupTask {

        @Override
        protected void doSetup(ManagementClient client, String containerId) throws Exception {
            super.doSetup(client, containerId);

            ModelNode op = Operations.createWriteAttributeOperation(
                    Operations.createAddress("subsystem", "batch-jberet", "in-memory-job-repository", "in-memory"),
                    "execution-records-limit", 2);

            final ModelNode result = client.getControllerClient().execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.fail(Operations.getFailureDescription(result).toString());
            }
            Operations.readResult(result);
            ServerReload.reloadIfRequired(client);
        }

    }

}
