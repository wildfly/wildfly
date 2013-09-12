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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Vector;

import org.jboss.ws.api.monitoring.RecordProcessor;
import org.jboss.ws.api.monitoring.RecordProcessorFactory;
import org.jboss.ws.common.integration.AbstractDeploymentAspect;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * An aspect that sets the record processors for each endpoint.
 *
 * @author alessio.soldano@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointRecordProcessorDeploymentAspect extends AbstractDeploymentAspect {

    private List<RecordProcessor> processors = new LinkedList<RecordProcessor>();

    public EndpointRecordProcessorDeploymentAspect() {
        ServiceLoader<RecordProcessorFactory> loader = ServiceLoader.load(RecordProcessorFactory.class);
        Iterator<RecordProcessorFactory> iterator = loader.iterator();
        while (iterator.hasNext()) {
            RecordProcessorFactory factory = iterator.next();
            processors.addAll(factory.newRecordProcessors());
        }
    }

    @Override
    public void start(final Deployment dep) {
        final int size = processors.size();
        for (final Endpoint ep : dep.getService().getEndpoints()) {
            List<RecordProcessor> processorList = new Vector<RecordProcessor>(size);
            for (RecordProcessor pr : processors) {
                try {
                    RecordProcessor clone = (RecordProcessor) pr.clone();
                    processorList.add(clone);
                } catch (final CloneNotSupportedException ex) {
                    throw new RuntimeException(ex);
                }
            }
            ep.setRecordProcessors(processorList);
        }
    }

    public void stop(final Deployment dep) {
        for (final Endpoint ep : dep.getService().getEndpoints()) {
            ep.setRecordProcessors(Collections.<RecordProcessor>emptyList());
        }
    }

}
