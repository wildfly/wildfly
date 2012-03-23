/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.jsr88;

import static org.jboss.as.test.http.Authentication.PASSWORD;
import static org.jboss.as.test.http.Authentication.USERNAME;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.shared.factories.DeploymentFactoryManager;
import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.factories.DeploymentFactory;
import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.spi.status.ProgressEvent;
import javax.enterprise.deploy.spi.status.ProgressListener;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.ee.deployment.spi.DeploymentManagerImpl;
import org.jboss.as.ee.deployment.spi.factories.DeploymentFactoryImpl;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

/**
 * Deployment API JSR-88 tests
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Feb-2012
 */
abstract class AbstractDeploymentTest {

    static final long TIMEOUT = 10000;

    DeploymentManager getDeploymentManager(ManagementClient managementClient) throws Exception {
        String uri = DeploymentManagerImpl.DEPLOYER_URI + "?targetType=as7&serverHost=" + managementClient.getMgmtAddress() + "&serverPort=" + managementClient.getMgmtPort();
        DeploymentFactoryImpl.register();
        DeploymentFactoryManager dfManager = DeploymentFactoryManager.getInstance();
        DeploymentFactory[] factories = dfManager.getDeploymentFactories();
        DeploymentManager deploymentManager = factories[0].getDeploymentManager(uri, USERNAME, PASSWORD);
        return deploymentManager;
    }

    ProgressObject jsr88Deploy(DeploymentManager manager, Archive<?> archive) throws Exception {
        Target[] targets = manager.getTargets();
        assertEquals(1, targets.length);

        InputStream deploymentPlan = createDeploymentPlan(archive.getName());

        // Deploy the test archive
        InputStream inputStream = archive.as(ZipExporter.class).exportAsInputStream();
        ProgressObject progress = manager.distribute(targets, inputStream, deploymentPlan);
        StateType state = awaitCompletion(progress, TIMEOUT);

        if (state == StateType.COMPLETED) {
            progress = manager.start(progress.getResultTargetModuleIDs());
            awaitCompletion(progress, TIMEOUT);
        }

        return progress;
    }

    abstract InputStream createDeploymentPlan(String name) throws Exception;

    ProgressObject jsr88Undeploy(DeploymentManager manager, TargetModuleID[] resultTargetModuleIDs) throws Exception {
        Target[] targets = manager.getTargets();
        assertEquals(1, targets.length);

        ProgressObject progress = manager.stop(resultTargetModuleIDs);
        awaitCompletion(progress, TIMEOUT);

        progress = manager.undeploy(resultTargetModuleIDs);
        awaitCompletion(progress, TIMEOUT);

        return progress;
    }

    private StateType awaitCompletion(ProgressObject progress, long timeout) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        progress.addProgressListener(new ProgressListener() {
            public void handleProgressEvent(ProgressEvent event) {
                DeploymentStatus status = event.getDeploymentStatus();
                if (status.isCompleted() || status.isFailed()) {
                    latch.countDown();
                }
            }
        });

        final DeploymentStatus status = progress.getDeploymentStatus();
        if (status.isCompleted())
            return status.getState();

        if (latch.await(timeout, TimeUnit.MILLISECONDS) == false)
            throw new IllegalStateException("Deployment timeout: " + progress);

        return status.getState();
    }
}
