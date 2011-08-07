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

import org.jboss.as.web.VirtualHost;
import org.jboss.as.web.WebSubsystemServices;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.wsf.spi.publish.EndpointPublisher;
import org.jboss.wsf.spi.publish.EndpointPublisherFactory;

/**
 * Factory for retrieving a EndpointPublisher instance for the currently running JBoss Application Server container.
 *
 * @author <a href="mailto:alessio.soldano@jboss.com">Alessio Soldano</a>
 */
public class EndpointPublisherFactoryImpl implements EndpointPublisherFactory {

    @SuppressWarnings("unchecked")
    public EndpointPublisher newEndpointPublisher(String hostname) throws Exception {
        ServiceController<?> controller = WSServices.getContainerRegistry().getService(
                WebSubsystemServices.JBOSS_WEB_HOST.append(hostname));
        Service<VirtualHost> service = (Service<VirtualHost>) controller.getService();
        return new EndpointPublisherImpl(service.getValue().getHost());
    }
}
