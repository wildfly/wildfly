/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.model.ServerDeploymentStartStopHandler;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A really ugly hack that deploys a rar to support a datasource
 *
 * @author Jason T. Greene
 */
final class JDBCRARDeployService implements Service<JDBCRARDeployService> {
    public static final ServiceName NAME = ServiceName.JBOSS.append("connector", "jdbc-rar-deployer");

    private static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    private static final ServerDeploymentStartStopHandler handler = new ServerDeploymentStartStopHandler();
    private static final String LOCAL_RAR_NAME="jdbc-local-1.0.0.Beta3.rar";
    private static final String XA_RAR_NAME="jdbc-xa-1.0.0.Beta3.rar";

    /** create an instance **/
    public JDBCRARDeployService() {
        super();
    }

    @Override
    public JDBCRARDeployService getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting JDBC RAR Deploy Service");
        ServiceContainer container = context.getController().getServiceContainer();

        try {
            handler.deploy(LOCAL_RAR_NAME, LOCAL_RAR_NAME, null, container, UpdateResultHandler.NULL, null);
        } catch (Throwable t) {
            log.error("Could not deploy local rar", t);
        }

        try {
            handler.deploy(XA_RAR_NAME, XA_RAR_NAME, null, container, UpdateResultHandler.NULL, null);
        } catch (Throwable t) {
            try {
                handler.undeploy(LOCAL_RAR_NAME, container , UpdateResultHandler.NULL, null);
            } catch (Throwable t2) {
                log.warn("Could not undeploy local rar", t2);
            }

            log.error("Could not deploy xa rar", t);
        }
    }

    @Override
    public void stop(StopContext context) {
        ServiceContainer container = context.getController().getServiceContainer();

        try {
            handler.undeploy(LOCAL_RAR_NAME, container, UpdateResultHandler.NULL, null);
        } catch (Throwable t) {
            log.error("Could not undeploy local rar", t);
        }

        try {
            handler.undeploy(XA_RAR_NAME, container, UpdateResultHandler.NULL, null);
        } catch (Throwable t) {
            log.error("Could not undeploy xa rar", t);
        }
    }
}
