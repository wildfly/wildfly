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
package org.jboss.as.ejb3.component.messagedriven;

import org.jboss.as.ee.component.AbstractComponentConfiguration;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.modules.Module;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class MessageDrivenComponentDescription extends EJBComponentDescription {
    private String messageListenerInterfaceName;
    private String resourceAdapterName;

    /**
     * Construct a new instance.
     *
     * @param componentName      the component name
     * @param componentClassName the component instance class name
     * @param moduleName         the module name
     * @param applicationName    the application name
     */
    public MessageDrivenComponentDescription(final String componentName, final String componentClassName, final String moduleName, final String applicationName) {
        super(componentName, componentClassName, moduleName, applicationName);
    }

    @Override
    public MethodIntf getMethodIntf(String viewClassName) {
        // an MDB doesn't expose a real view
        return MethodIntf.BEAN;
    }

    @Override
    protected MessageDrivenComponentConfiguration constructComponentConfiguration() {
        return new MessageDrivenComponentConfiguration(this);
    }

    String getMessageListenerInterfaceName() {
        return messageListenerInterfaceName;
    }

    String getResourceAdapterName() {
        return resourceAdapterName;
    }

    @Override
    protected void prepareComponentConfiguration(AbstractComponentConfiguration configuration, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        super.prepareComponentConfiguration(configuration, phaseContext);

        final MessageDrivenComponentConfiguration messageDrivenComponentConfiguration = (MessageDrivenComponentConfiguration) configuration;
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final ClassLoader classLoader = module.getClassLoader();

        try {
            messageDrivenComponentConfiguration.setMessageListenerInterface(classLoader.loadClass(getMessageListenerInterfaceName()));
        } catch (ClassNotFoundException e) {
            throw new DeploymentUnitProcessingException("Failed to load message listener interface " + getMessageListenerInterfaceName());
        }
    }

    public void setMessageListenerInterfaceName(String messageListenerInterfaceName) {
        getViewClassNames().add(messageListenerInterfaceName);
        this.messageListenerInterfaceName = messageListenerInterfaceName;
    }

    public void setResourceAdapterName(String resourceAdapterName) {
        this.resourceAdapterName = resourceAdapterName;
    }
}
