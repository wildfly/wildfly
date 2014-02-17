/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.webservices.publish;

import org.jboss.as.web.host.WebHost;
import org.jboss.as.webservices.util.ASHelper;
import org.jboss.wsf.spi.publish.EndpointPublisher;
import org.jboss.wsf.spi.publish.EndpointPublisherFactory;

/**
 * Factory for retrieving an EndpointPublisher instance for the currently running JBoss Application Server container.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public class EndpointPublisherFactoryImpl implements EndpointPublisherFactory {

    public EndpointPublisher newEndpointPublisher(String hostname) throws Exception {
        WebHost virtualHost = ASHelper.getMSCService(WebHost.SERVICE_NAME.append(hostname),
                WebHost.class);
        return new EndpointPublisherImpl(virtualHost);
    }

}
