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
package org.jboss.as.ee.deployment.spi;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.status.ProgressObject;

import org.jboss.as.ee.deployment.spi.status.ProgressObjectImpl;
import org.jboss.logging.Logger;

/**
 * A thread that deployes the given deployment on all targets contained in the progress object. It sends events to the progress
 * object.
 *
 * @author Thomas.Diesler@jboss.com
 */
final class DeploymentWorker extends Thread {
    // deployment logging
    private static final Logger log = Logger.getLogger(DeploymentWorker.class);

    private ProgressObjectImpl progress;

    DeploymentWorker(ProgressObject progress) {
        this.progress = (ProgressObjectImpl) progress;
    }

    /**
     * Deploy the module on each given target
     */
    public void run() {
        log.trace("Begin run");
        CommandType cmdType = progress.getDeploymentStatus().getCommand();
        TargetModuleID[] modules = progress.getResultTargetModuleIDs();
        for (int i = 0; i < modules.length; i++) {
            TargetModuleID moduleid = modules[i];
            JBossTarget target = (JBossTarget) moduleid.getTarget();
            try {
                progress.sendProgressEvent(StateType.RUNNING, "Operation " + cmdType + " started", moduleid);
                if (cmdType == CommandType.DISTRIBUTE) {
                    target.deploy(moduleid);
                    deleteDeployment(moduleid);
                } else if (cmdType == CommandType.START) {
                    target.start(moduleid);
                } else if (cmdType == CommandType.STOP) {
                    target.stop(moduleid);
                } else if (cmdType == CommandType.UNDEPLOY) {
                    target.undeploy(moduleid);
                    deleteDeployment(moduleid);
                }
                progress.sendProgressEvent(StateType.COMPLETED, "Operation " + cmdType + " completed", moduleid);
            } catch (Exception e) {
                String message = "Operation " + cmdType + " failed on target " + target;
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println(message);
                e.printStackTrace(pw);
                pw.close();
                message = sw.toString();
                progress.sendProgressEvent(StateType.FAILED, message, moduleid);
                log.error(message, e);
            }
        }
        log.trace("End run");
    }

    private void deleteDeployment(TargetModuleID moduleid) throws MalformedURLException {
        File deployment = new File(new URL(moduleid.getModuleID()).getPath());
        if (deployment.exists()) {
            if (!deployment.delete()) {
                log.warn("Cannot delete deployment file " + deployment + ", will be deleted on exit");
                deployment.deleteOnExit();
            }
        } else {
            log.error("Deployment does not exist: " + deployment);
        }
    }
}
