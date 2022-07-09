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
import org.jboss.as.test.integration.batch.common.AbstractBatchTestCase;
import org.jboss.as.test.integration.batch.common.CountingItemReader;
import org.jboss.as.test.integration.batch.common.CountingItemWriter;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryService;

@RunWith(Arquillian.class)
@ServerSetup(JdbcRepositoryExecutionsLimitTestCase.JdbcRepositorySetup.class)
public class JdbcRepositoryExecutionsLimitTestCase extends AbstractBatchTestCase {

    private static final String DEPLOYMENT_NAME = "jdbc-batch.war";

    @ArquillianResource
    private ServiceContainer serviceContainer;

    @Deployment(name = DEPLOYMENT_NAME)
    public static WebArchive createNamedJdbcDeployment() {
        return createDefaultWar(DEPLOYMENT_NAME, JdbcRepositoryExecutionsLimitTestCase.class.getPackage(), "test-chunk.xml")
                .addClasses(CountingItemReader.class, CountingItemWriter.class, JdbcJobRepositoryService.class,
                        JobRepositoryTestUtils.class, H2JdbcJobRepositorySetUp.class)
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.msc,org.wildfly.security.manager,org.wildfly.extension.batch.jberet")
                                .exportAsString()));
    }

    @Test
    public void testGetJobExecutionsWithLimit() {
        JobRepositoryTestUtils.testGetJobExecutionsWithLimit(serviceContainer, "jdbc");
    }

    static class JdbcRepositorySetup extends H2JdbcJobRepositorySetUp {

        @Override
        protected void configureJobRepository(ModelNode op) {
            super.configureJobRepository(op);
            op.get("execution-records-limit").set(2);
        }

    }

}
