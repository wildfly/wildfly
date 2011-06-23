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
package org.jboss.as.osgi.parser;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.deployment.BundleStartTracker;
import org.jboss.as.osgi.deployment.OSGiDeploymentActivator;
import static org.jboss.as.osgi.parser.CommonAttributes.ACTIVATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION;
import static org.jboss.as.osgi.parser.CommonAttributes.CONFIGURATION_PROPERTIES;
import static org.jboss.as.osgi.parser.CommonAttributes.MODULES;
import static org.jboss.as.osgi.parser.CommonAttributes.PID;
import static org.jboss.as.osgi.parser.CommonAttributes.PROPERTIES;
import static org.jboss.as.osgi.parser.CommonAttributes.STARTLEVEL;

import org.jboss.as.osgi.parser.SubsystemState.Activation;
import org.jboss.as.osgi.parser.SubsystemState.OSGiModule;
import org.jboss.as.osgi.service.ConfigAdminServiceImpl;
import org.jboss.as.osgi.service.FrameworkBootstrapService;
import org.jboss.as.osgi.service.BundleInstallProviderIntegration;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * OSGi subsystem operation handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @since 11-Sep-2010
 */
class OSGiSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    private static final Activation DEFAULT_ACTIVATION = Activation.LAZY;

    static final OSGiSubsystemAdd INSTANCE = new OSGiSubsystemAdd();

    private OSGiSubsystemAdd() {
        // Private to ensure a singleton.
    }

    protected void populateModel(final ModelNode operation, final ModelNode subModel) {
        if (operation.has(ACTIVATION)) {
            subModel.get(ACTIVATION).set(operation.get(ACTIVATION));
        }
        if (operation.has(CONFIGURATION)) {
            subModel.get(CONFIGURATION).set(operation.get(CONFIGURATION));
        }
        if (operation.has(PROPERTIES)) {
            subModel.get(PROPERTIES).set(operation.get(PROPERTIES));
        }
        if (operation.has(MODULES)) {
            subModel.get(MODULES).set(operation.get(MODULES));
        }
    }

    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                new OSGiDeploymentActivator().activate(processorTarget);
            }
        }, OperationContext.Stage.RUNTIME);

        log.infof("Activating OSGi Subsystem");
        long begin = System.currentTimeMillis();
        SubsystemState subsystemState = createSubsystemState(operation);

        ServiceTarget serviceTarget = context.getServiceTarget();
        newControllers.add(BundleStartTracker.addService(serviceTarget));
        newControllers.add(BundleInstallProviderIntegration.addService(serviceTarget));
        newControllers.addAll(FrameworkBootstrapService.addService(serviceTarget, subsystemState, verificationHandler));

        newControllers.add(ConfigAdminServiceImpl.addService(serviceTarget, subsystemState, verificationHandler));

        long end = System.currentTimeMillis();
        log.debugf("Activated OSGi Subsystem in %dms", end - begin);
    }

    // TODO - This conversion should be reviewed, initially this is to simplify
    // the conversion to detyped so the services do not yet need the detyped
    // API to be introduced.
    private SubsystemState createSubsystemState(final ModelNode operation) {
        SubsystemState subsystemState = new SubsystemState();

        Activation activation = DEFAULT_ACTIVATION;
        if (operation.has(ACTIVATION)) {
            activation = Activation.valueOf(operation.get(ACTIVATION).asString().toUpperCase());
        }
        subsystemState.setActivation(activation);

        if (operation.has(CONFIGURATION)) {
            ModelNode configuration = operation.get(CONFIGURATION);
            String pid = configuration.require(PID).asString();
            Hashtable<String, String> dictionary = new Hashtable<String, String>();
            if (configuration.has(CONFIGURATION_PROPERTIES)) {
                ModelNode configurationProperties = configuration.get(CONFIGURATION_PROPERTIES);
                Set<String> keys = configurationProperties.keys();
                if (keys != null) {
                    for (String current : keys) {
                        String value = configurationProperties.get(current).asString();
                        dictionary.put(current, value);
                    }
                }
            }

            subsystemState.putConfiguration(pid, dictionary);
        }

        if (operation.has(PROPERTIES)) {
            ModelNode properties = operation.get(PROPERTIES);
            Set<String> keys = properties.keys();
            if (keys != null) {
                for (String current : keys) {
                    String value = properties.get(current).asString();
                    subsystemState.addProperty(current, value);
                }
            }
        }

        if (operation.has(MODULES)) {
            ModelNode modules = operation.get(MODULES);
            Set<String> keys = modules.keys();
            if (keys != null) {
                for (String current : keys) {
                    ModelNode modelNode = modules.get(current).get(STARTLEVEL);
                    Integer startLevel = modelNode.isDefined() ? modelNode.asInt() : null;
                    subsystemState.addModule(new OSGiModule(ModuleIdentifier.fromString(current), startLevel));
                }
            }
        }

        return subsystemState;
    }
}
