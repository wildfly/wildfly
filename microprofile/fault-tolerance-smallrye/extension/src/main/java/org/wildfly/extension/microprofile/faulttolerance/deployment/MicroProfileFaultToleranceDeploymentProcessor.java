/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;
import io.smallrye.faulttolerance.HystrixExtension;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.Capabilities;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.wildfly.extension.microprofile.faulttolerance.MicroProfileFaultToleranceLogger;

/**
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceDeploymentProcessor implements DeploymentUnitProcessor {

    private static final ConcurrentMapConfiguration configInstance = new ConcurrentMapConfiguration();
    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);
    private static volatile int remainingHystrixConfiguringDeployments;

    static {
        // Configuration can be set only once
        ConfigurationManager.install(configInstance);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!MicroProfileFaultToleranceMarker.isMarked(deploymentUnit)) {
            return;
        }

        // Weld Extension
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

        WeldCapability weldCapability;
        try {
            weldCapability = support.getCapabilityRuntimeAPI(Capabilities.WELD_CAPABILITY_NAME, WeldCapability.class);
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException();
        }

        weldCapability.registerExtensionInstance(new MicroProfileFaultToleranceCDIExtension(), deploymentUnit);
        weldCapability.registerExtensionInstance(new HystrixExtension(), deploymentUnit);

        synchronized (this) {
            if (remainingHystrixConfiguringDeployments == 0) {
                Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                Config mpConfig = ConfigProvider.getConfig(module.getClassLoader());

                // We need to iterate over all keys since the key names are dynamic and not known in advance
                boolean isHystrixConfiguringDeployment = false;
                for (String key : mpConfig.getPropertyNames()) {
                    if (key.startsWith("hystrix.")) {
                        String value = mpConfig.getValue(key, String.class);
                        MicroProfileFaultToleranceLogger.ROOT_LOGGER.debugf("Configuring Hystrix: %s=%s", key, value);
                        configInstance.setProperty(key, value);
                        isHystrixConfiguringDeployment = true;
                    }
                }
                if (isHystrixConfiguringDeployment) {
                    deploymentUnit.putAttachment(ATTACHMENT_KEY, Boolean.TRUE);
                    remainingHystrixConfiguringDeployments++;
                }
            } else {
                MicroProfileFaultToleranceLogger.ROOT_LOGGER.hystrixAlreadyConfigured(deploymentUnit.getName());
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        synchronized (this) {
            if (deploymentUnit.removeAttachment(ATTACHMENT_KEY) != null) {
                remainingHystrixConfiguringDeployments--;
                if (remainingHystrixConfiguringDeployments == 0) {
                    configInstance.clear();
                }
            }
        }
    }
}
