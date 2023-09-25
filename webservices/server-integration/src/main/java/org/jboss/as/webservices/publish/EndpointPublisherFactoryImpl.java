/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.publish;

import static org.jboss.as.webservices.util.ASHelper.getMSCService;

import org.jboss.as.web.host.WebHost;
import org.jboss.wsf.spi.publish.EndpointPublisher;
import org.jboss.wsf.spi.publish.EndpointPublisherFactory;

/**
 * Factory for retrieving an EndpointPublisher instance for the currently running JBoss Application Server container.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EndpointPublisherFactoryImpl implements EndpointPublisherFactory {

    public EndpointPublisher newEndpointPublisher(final String hostname) {
        return new EndpointPublisherImpl(getMSCService(WebHost.SERVICE_NAME.append(hostname), WebHost.class));
    }

}
