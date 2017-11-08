/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.component.messagedriven;

import java.util.Properties;

import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.ejb3.inflow.EndpointDeployer;
import org.jboss.as.server.suspend.SuspendController;
import org.jboss.jca.core.spi.rar.Endpoint;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class MessageDrivenComponentCreateService extends EJBComponentCreateService {

    private final Class<?> messageListenerInterface;
    private final Properties activationProps;
    private final String resourceAdapterName;
    private final boolean deliveryActive;
    private final ServiceName deliveryControllerName;
    private final InjectedValue<ResourceAdapter> resourceAdapterInjectedValue = new InjectedValue<ResourceAdapter>();
    private final InjectedValue<PoolConfig> poolConfig = new InjectedValue<PoolConfig>();
    private final InjectedValue<EJBUtilities> ejbUtilitiesInjectedValue = new InjectedValue<EJBUtilities>();
    private final InjectedValue<SuspendController> suspendControllerInjectedValue = new InjectedValue<>();
    private final ClassLoader moduleClassLoader;

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public MessageDrivenComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration, final Class<?> messageListenerInterface) {
        super(componentConfiguration, ejbJarConfiguration);

        final MessageDrivenComponentDescription componentDescription = (MessageDrivenComponentDescription) componentConfiguration.getComponentDescription();
        this.resourceAdapterName = componentDescription.getResourceAdapterName();
        this.deliveryControllerName = componentDescription.isDeliveryControlled()? componentDescription.getDeliveryControllerName(): null;
        this.deliveryActive = !componentDescription.isDeliveryControlled() && componentDescription.isDeliveryActive();
        // see MessageDrivenComponentDescription.<init>
        this.messageListenerInterface = messageListenerInterface;

        this.activationProps = componentDescription.getActivationProps();
        this.moduleClassLoader = componentConfiguration.getModuleClassLoader();
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        // AS7-3073 just log a message about the MDB being started
        EjbLogger.ROOT_LOGGER.logMDBStart(this.getComponentName(), this.resourceAdapterName);
    }

    @Override
    protected BasicComponent createComponent() {
        String configuredResourceAdapterName = resourceAdapterName;

        // Match configured value to the actual RA names
        final String activeResourceAdapterName = searchActiveResourceAdapterName(configuredResourceAdapterName);


        final ActivationSpec activationSpec = getEndpointDeployer().createActivationSpecs(activeResourceAdapterName, messageListenerInterface, activationProps, getDeploymentClassLoader());
        final MessageDrivenComponent component = new MessageDrivenComponent(this, messageListenerInterface, activationSpec, deliveryActive, deliveryControllerName, activeResourceAdapterName);
        // set the endpoint
        final EJBUtilities ejbUtilities = this.ejbUtilitiesInjectedValue.getValue();
        final Endpoint endpoint = ejbUtilities.getEndpoint(activeResourceAdapterName);

        component.setEndpoint(endpoint);

        return component;
    }

    private String searchActiveResourceAdapterName(String configuredResourceAdapterName) {
        // Use the configured value unless it doesn't match and some variant of it does
        String result = configuredResourceAdapterName;
        if (ConnectorServices.getRegisteredResourceAdapterIdentifier(configuredResourceAdapterName) == null) {
            // No direct match. See if we have a match with .rar removed or appended
            String amended = stripDotRarSuffix(configuredResourceAdapterName);
            if (configuredResourceAdapterName.equals(amended)) {
                // There was no .rar to strip; try appending
                amended = configuredResourceAdapterName + ".rar";
            }
            if (ConnectorServices.getRegisteredResourceAdapterIdentifier(amended) != null) {
                result = amended;
            }
        }

        return result;
    }

    PoolConfig getPoolConfig() {
        return this.poolConfig.getOptionalValue();
    }

    public InjectedValue<PoolConfig> getPoolConfigInjector() {
        return this.poolConfig;
    }

    private ClassLoader getDeploymentClassLoader() {
        return getComponentClass().getClassLoader();
    }

    private EndpointDeployer getEndpointDeployer() {
        return getEJBUtilities();
    }

    public InjectedValue<ResourceAdapter> getResourceAdapterInjector() {
        return this.resourceAdapterInjectedValue;
    }

    public Injector<EJBUtilities> getEJBUtilitiesInjector() {
        return this.ejbUtilitiesInjectedValue;
    }

    public ClassLoader getModuleClassLoader() {
        return moduleClassLoader;
    }

    public InjectedValue<SuspendController> getSuspendControllerInjectedValue() {
        return suspendControllerInjectedValue;
    }

    private String stripDotRarSuffix(final String raName) {
        if (raName == null) {
            return null;
        }
        // See RaDeploymentParsingProcessor
        if (raName.endsWith(".rar")) {
            return raName.substring(0, raName.indexOf(".rar"));
        }
        return raName;
    }
}
