/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

public class CommonDeploymentService implements Service<CommonDeployment> {

    private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("data-source").append("common-deployment");

    public static ServiceName getServiceName(ContextNames.BindInfo bindInfo) {
        return SERVICE_NAME_BASE.append(bindInfo.getBinderServiceName().getCanonicalName());
    }

    private CommonDeployment value;

    /** create an instance **/
    public CommonDeploymentService(CommonDeployment value) {
        super();
        this.value = value;

    }

    @Override
    public CommonDeployment getValue() throws IllegalStateException, IllegalArgumentException {
        return value;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Started CommonDeployment %s", context.getController().getName());
    }

    @Override
    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopped CommonDeployment %s", context.getController().getName());
    }
}
