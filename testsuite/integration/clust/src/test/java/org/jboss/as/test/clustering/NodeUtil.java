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
package org.jboss.as.test.clustering;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to start and stop containers and/or deployments.
 *
 * @author Radoslav Husar
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 * @version Oct 2012
 */
public final class NodeUtil {

    private static final Logger log = Logger.getLogger(NodeUtil.class);

    private static Map<String, Set<String>> deployed = new HashMap<String, Set<String>>();

    public static void resetDeployments() {
        if (!deployed.isEmpty()) {
            deployed.clear();
        }
    }

    public static void cleanDeployments(ContainerController controller, Deployer deployer) {
        Map<String, Set<String>> tempDeployed = new HashMap<String, Set<String>>(deployed);
        for (String container: tempDeployed.keySet()) {
            try {
                Set<String> deployments = tempDeployed.get(container);
                if (deployments == null) {
                    continue;
                }
                boolean started = false;
                if (!controller.isStarted(container)) {
                    start(controller, container);
                    started = true;
                }
                Set<String> tempDeployments = new HashSet<String>(deployments);
                for (String deployment: tempDeployments) {
                    undeploy(container, deployer, deployment);
                }
                if (started) {
                    stop(controller, container);
                }
            } catch (Exception e) {
                log.error("Failed to undeploy from container(s)", e);
            }
        }
    }

    public static void deploy(String container, Deployer deployer, String deployments) {
        Set<String> containerDeployments = deployed.get(container);
        if (containerDeployments == null) {
            containerDeployments = new HashSet<String>();
            deployed.put(container, containerDeployments);
        } else if (containerDeployments.contains(deployments)) {
            log.info("Already deployed deployment: " + deployments);
            return;
        }

        log.info("Deploying deployment=" + deployments);
        deployer.deploy(deployments);

        // remember this deployment
        containerDeployments.add(deployments);
    }

    public static void undeploy(String container, Deployer deployer, String deployments) {
        log.info("Undeploying deployment=" + deployments);
        deployer.undeploy(deployments);

        // forget this deployment
        Set<String> containerDeployments = deployed.get(container);
        if (containerDeployments != null) {
            containerDeployments.remove(deployments);
            if (containerDeployments.isEmpty()) {
                deployed.remove(container);
            }
        }
    }

    public static void start(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            log.info("Starting deployment=" + deployment + ", container=" + container);
            controller.start(container);
            deploy(container, deployer, deployment);
            log.info("Started deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            log.error("Failed to start container(s)", e);
        }
    }

    public static void start(ContainerController controller, String[] containers) {
        // TODO do this in parallel.
        for (int i = 0; i < containers.length; i++) {
            try {
                log.info("Starting deployment=NONE, container=" + containers[i]);
                controller.start(containers[i]);
            } catch (Throwable e) {
                log.error("Failed to start containers", e);
            }
        }
    }

    public static void start(ContainerController controller, String container) {
        try {
            log.info("Starting deployment=NONE, container=" + container);
            controller.start(container);
        } catch (Throwable e) {
            log.error("Failed to start containers", e);
        }
    }

    public static void stop(ContainerController controller, String[] containers) {
        for (int i = 0; i < containers.length; i++) {
            try {
                log.info("Stopping container=" + containers[i]);
                controller.stop(containers[i]);
                log.info("Stopped container=" + containers[i]);
            } catch (Throwable e) {
                log.error("Failed to stop containers", e);
            }
        }
    }

    public static void stop(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            log.info("Stopping deployment=" + deployment + ", container=" + container);
            undeploy(container, deployer, deployment);
            controller.stop(container);
            log.info("Stopped deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            log.error("Failed to stop containers", e);
        }
    }

    public static void stop(ContainerController controller, String container) {
        try {
            controller.stop(container);
            log.info("Stopped deployment=NONE, container=" + container);
        } catch (Throwable e) {
            log.error("Failed to stop containers", e);
        }
    }

    private NodeUtil() {
    }

}
