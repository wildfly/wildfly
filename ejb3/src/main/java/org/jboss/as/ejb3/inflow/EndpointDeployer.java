/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.inflow;

import javax.resource.spi.ActivationSpec;
import java.util.Properties;

/**
 * Connector 1.6 13.4.3 Endpoint Deployer
 * The endpoint deployer is a human who has the responsibility to deploy the message
 * endpoint, or application, on an application server. The deployer is expected to know
 * the requirements of the application and be aware of the details of the runtime
 * environment in which the application will be deployed.
 *
 * In JBoss we have automated the endpoint deployer. :-)
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface EndpointDeployer {
    /**
     * The deployer configures an ActivationSpec JavaBean instance based on the
     * information provided by the application developer or assembler, which is contained
     * in the endpoint deployment descriptor or by way of metadata annotations described
     * in Section 18.7, “@Activation” on page 18-14. The deployer may also use additional
     * message provider-specific information to configure the ActivationSpec JavaBean
     * instance.
     *
     * @param resourceAdapterName       the name of the resource adapter to use
     * @param messageListenerInterface  the listener interface of the endpoint
     * @param beanProps                 the standard and provider-specific information for configuring the activation spec
     * @param classLoader               the class loader which holds the activation spec class
     * @return
     */
    ActivationSpec createActivationSpecs(String resourceAdapterName, Class<?> messageListenerInterface, Properties beanProps, ClassLoader classLoader);
}
