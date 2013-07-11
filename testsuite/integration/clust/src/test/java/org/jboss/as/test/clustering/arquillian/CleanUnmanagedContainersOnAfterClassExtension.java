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
package org.jboss.as.test.clustering.arquillian;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.Container.State;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.spi.event.container.AfterDeploy;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.event.suite.AfterClass;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Arquillian extension which undeploys any deployments left in custom containers on AfterClass event.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 * @version $Revision: $
 */
public class CleanUnmanagedContainersOnAfterClassExtension implements LoadableExtension {

    private static final Logger log = Logger.getLogger(CleanUnmanagedContainersOnAfterClassExtension.class);

    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(CleanUnmanagedDeployments.class);
    }

    public static class CleanUnmanagedDeployments {

        private Map<String, Set<String>> deployments = new HashMap<String, Set<String>>();

        @Inject
        private Instance<Deployer> deployer;
        @Inject
        protected Instance<ContainerController> containerController;

        public void handleBeforeClass(@Observes BeforeClass event, final ContainerRegistry registry) throws Exception {
            if (!deployments.isEmpty()) {
                deployments.clear();    // this should not be needed if cleanDeploymentsAfterClass does not fail
            }
        }

        public void cleanDeploymentsAfterClass(@Observes AfterClass event, final ContainerRegistry registry) throws Exception {
            for (Container c : registry.getContainers()) {
                if (isUnmanagedContainer(c) && deployments.containsKey(c.getName())) {
                    // clean up remaining deployments even if the container is stopped
                    undeployAll(c);
                }
            }
        }

        public void handleUnmanagedDeploy(@Observes AfterDeploy event, final Container container) throws Exception {
            if (!isUnmanagedContainer(container)) {
                return;
            }

            final DeploymentDescription deployment = event.getDeployment();
            if (deployment.managed()) {
                return;
            }

            String containerName = container.getName();
            synchronized (deployments) {
                Set<String> containerDeployments = deployments.get(containerName);
                if (containerDeployments == null) {
                    containerDeployments = new HashSet<String>();
                    deployments.put(containerName, containerDeployments);
                }
                containerDeployments.add(deployment.getName());
            }
        }

        public void handleUnmanagedUndeploy(@Observes AfterUnDeploy event, final Container container) throws Exception {
            if (!isUnmanagedContainer(container)) {
                return;
            }

            final DeploymentDescription deployment = event.getDeployment();
            if (deployment.managed()) {
                return;
            }

            String containerName = container.getName();
            final String deploymentName = deployment.getName();
            final Set<String> containerDeployments = deployments.get(containerName);
            if (containerDeployments == null || !containerDeployments.contains(deploymentName)) {
                return;
            }

            synchronized (deployments) {
                containerDeployments.remove(deploymentName);
                if (containerDeployments.isEmpty()) {
                    deployments.remove(containerName);
                }
            }
        }

        private void undeployAll(final Container container) {
            // check the state of the given container and start it eventually
            boolean started = false;
            if (container.getState() == State.STOPPED) {
                try {
                    log.warn("Starting a custom container in order to clean up deployments left there: " + container.getName());
                    containerController.get().start(container.getName());
                    started = true;
                } catch (Exception e) {
                    log.error("Failed to start custom container: " + container.getName(), e);
                }
            }
            // undeploy all remaining deployments
            Set<String> tempDeployments = new HashSet<String>(deployments.get(container.getName()));
            for (String deployment : tempDeployments) {
                try {
                    log.warn("Undeploying a deployment you left deployed, please do your own housekeeping: [" + container.getName() + "] " + deployment);
                    deployer.get().undeploy(deployment);
                } catch (Exception e) {
                    log.error("Failed to undeploy " + deployment + " from custom container " + container.getName(), e);
                }
            }
            // forget the deployments
            deployments.remove(container.getName());
            // and eventually stop the container again
            if (started) {
                try {
                    containerController.get().stop(container.getName());
                } catch (Exception e) {
                    log.error("Failed to stop custom container: " + container.getName(), e);
                }
            }
        }

        private boolean isUnmanagedContainer(final Container container) {
            String mode = container.getContainerConfiguration().getMode();
            return "custom".equalsIgnoreCase(mode) || "manual".equalsIgnoreCase(mode);
        }
    }
}
