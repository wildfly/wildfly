/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.deployers;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

import javax.management.MBeanServer;

import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.ws.api.monitoring.RecordProcessor;
import org.jboss.ws.api.monitoring.RecordProcessorFactory;
import org.jboss.wsf.spi.deployment.Deployment;

/**
 * A deployer that sets the record processors for each endpoint
 *
 * @author alessio.soldano@jboss.com
 * @since 18-Jul-2011
 */
public class EndpointRecordProcessorDeploymentAspect extends org.jboss.ws.common.deployment.EndpointRecordProcessorDeploymentAspect {

    public EndpointRecordProcessorDeploymentAspect() {
        super();
        ServiceLoader<RecordProcessorFactory> loader = ServiceLoader.load(RecordProcessorFactory.class);
        Iterator<RecordProcessorFactory> iterator = loader.iterator();
        List<RecordProcessor> list = new LinkedList<RecordProcessor>();
        while (iterator.hasNext()) {
            RecordProcessorFactory factory = iterator.next();
            list.addAll(factory.newRecordProcessors());
        }
        setProcessors(list);
    }

    @Override
    public void start(Deployment dep) {
        final ServiceController<?> controller = WSServices.getContainerRegistry().getService(ServiceName.JBOSS.append("mbean", "server"));
        if (controller != null) {
            setMbeanServer((MBeanServer) controller.getService().getValue());
        } else {
            setMbeanServer(ManagementFactory.getPlatformMBeanServer());
        }
        super.start(dep);
    }
}
