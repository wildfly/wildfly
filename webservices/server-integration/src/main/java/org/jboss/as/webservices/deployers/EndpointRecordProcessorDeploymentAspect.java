/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
