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

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ResourceAdapter;
import java.util.Properties;

import org.jboss.as.ee.component.BasicComponent;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.EjbLogger;
import org.jboss.as.ejb3.component.EJBComponentCreateService;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.deployment.ApplicationExceptions;
import org.jboss.as.ejb3.inflow.EndpointDeployer;
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
    private final InjectedValue<ResourceAdapter> resourceAdapterInjectedValue = new InjectedValue<ResourceAdapter>();
    private final InjectedValue<PoolConfig> poolConfig = new InjectedValue<PoolConfig>();

    /**
     * Construct a new instance.
     *
     * @param componentConfiguration the component configuration
     */
    public MessageDrivenComponentCreateService(final ComponentConfiguration componentConfiguration, final ApplicationExceptions ejbJarConfiguration) {
        super(componentConfiguration, ejbJarConfiguration);

        final MessageDrivenComponentDescription componentDescription = (MessageDrivenComponentDescription) componentConfiguration.getComponentDescription();
        this.resourceAdapterName = this.stripDotRarSuffix(componentDescription.getResourceAdapterName());
        // see MessageDrivenComponentDescription.<init>
        this.messageListenerInterface = componentConfiguration.getViews().get(0).getViewClass();

        this.activationProps = componentDescription.getActivationProps();
    }

    @Override
    public void start(StartContext context) throws StartException {
        super.start(context);
        // AS7-3073 just log a message about the MDB being started
        EjbLogger.ROOT_LOGGER.logMDBStart(this.getComponentName(), this.resourceAdapterName);
    }

    @Override
    protected BasicComponent createComponent() {
        final ActivationSpec activationSpec = getEndpointDeployer().createActivationSpecs(resourceAdapterName, messageListenerInterface, activationProps, getDeploymentClassLoader());
        //final ActivationSpec activationSpec = null;
        final MessageDrivenComponent component = new MessageDrivenComponent(this, messageListenerInterface, activationSpec);
        final ResourceAdapter resourceAdapter = this.resourceAdapterInjectedValue.getValue();
        component.setResourceAdapter(resourceAdapter);
        try {
            activationSpec.setResourceAdapter(resourceAdapter);
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
        return component;
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
