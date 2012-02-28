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

import java.util.Date;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;

/**
 * Helper class to start and stop container including a deployment.
 *
 * @author Radoslav Husar
 */
public final class NodeUtil {

    private NodeUtil() {
    }

    public static void start(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            System.out.println(new Date() + "starting deployment=" + deployment + ", container=" + container);
            controller.start(container);
            deployer.deploy(deployment);
            System.out.println(new Date() + "started deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    public static void start(ContainerController controller, String container) {
        try {
            System.out.println(new Date() + "starting deployment=NONE, container=" + container);
            controller.start(container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    public static void stop(ContainerController controller, Deployer deployer, String container, String deployment) {
        try {
            System.out.println(new Date() + "stopping deployment=" + deployment + ", container=" + container);
            deployer.undeploy(deployment);
            controller.stop(container);
            System.out.println(new Date() + "stopped deployment=" + deployment + ", container=" + container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    public static void stop(ContainerController controller, String container) {
        try {
            controller.stop(container);
            System.out.println(new Date() + "stopped deployment=NONE, container=" + container);
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }
}
