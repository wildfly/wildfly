/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import org.jboss.as.arquillian.api.WildFlyContainerController;
import org.jboss.logging.Logger;

/**
 * Utility class to start and stop containers and/or deployments.
 *
 * @author Radoslav Husar
 * @version Oct 2012
 */
public final class NodeUtil {

    private static final Logger log = Logger.getLogger(NodeUtil.class);

    public static void deploy(Deployer deployer, String... deployments) {
        for (String deployment : deployments) {
            log.trace("Deploying deployment=" + deployment);
            deployer.deploy(deployment);
        }
    }

    public static void undeploy(Deployer deployer, String... deployments) {
        for (String deployment : deployments) {
            log.trace("Undeploying deployment=" + deployment);
            deployer.undeploy(deployment);
        }
    }

    public static void start(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            log.trace("Starting deployment=" + deployment + ", container=" + container);
            controller.start(container);
            deployer.deploy(deployment);
            log.trace("Started deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            log.error("Failed to start container(s)", e);
        }
    }

    public static void start(ContainerController controller, String... containers) {
        // TODO do this in parallel.
        for (String container : containers) {
            try {
                log.trace("Starting deployment=NONE, container=" + container);
                controller.start(container);
            } catch (Throwable e) {
                log.error("Failed to start containers", e);
            }
        }
    }

    public static void stop(ContainerController controller, String... containers) {
        for (String container : containers) {
            try {
                log.trace("Stopping container=" + container);
                controller.stop(container);
                log.trace("Stopped container=" + container);
            } catch (Throwable e) {
                log.error("Failed to stop containers", e);
            }
        }
    }

    public static void stop(int timeout, WildFlyContainerController controller, String... containers) {
        for (String container : containers) {
            try {
                log.trace("Stopping container=" + container + ", timeout=" + timeout);
                controller.stop(container, timeout);
                log.trace("Stopped container=" + container + ", timeout=" + timeout);
            } catch (Throwable e) {
                log.error("Failed to stop containers", e);
            }
        }
    }

    public static void stop(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            log.trace("Stopping deployment=" + deployment + ", container=" + container);
            deployer.undeploy(deployment);
            controller.stop(container);
            log.trace("Stopped deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            log.error("Failed to stop containers", e);
        }
    }

    private NodeUtil() {
    }
}
