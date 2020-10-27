/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.reactive.messaging.deployment;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.DotName;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.microprofile.reactive.messaging._private.MicroProfileReactiveMessagingLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingDependencyProcessor implements DeploymentUnitProcessor {

    // TODO Set to false for EAP https://issues.redhat.com/browse/JBEAP-20660
    private final boolean allowExperimental = true;

    private static final String EXPERIMENTAL_PROPERTY = "jboss.as.reactive.messaging.experimental";

    // Force strict mode in the SmallRye reactive messaging validation. This is needed to pass the TCK,
    // and also gives fail-fast behaviour
    //TODO Replace this with a ConfigSource once we have a release with https://github.com/smallrye/smallrye-reactive-messaging/pull/936/
    private static final String STRICT_MODE_PROPERTY = "smallrye-messaging-strict-binding";

    private static final List<DotName> REACTIVE_MESSAGING_ANNOTATIONS;
    private static final List<DotName> BANNED_REACTIVE_MESSAGING_ANNOTATIONS;
    static {
        List<DotName> annotations = new ArrayList<>();
        String rmPackage = "org.eclipse.microprofile.reactive.messaging.";
        annotations.add(DotName.createSimple(rmPackage + "Incoming"));
        annotations.add(DotName.createSimple(rmPackage + "Outgoing"));
        REACTIVE_MESSAGING_ANNOTATIONS = Collections.unmodifiableList(annotations);

        List<DotName> banned = new ArrayList<>();
        banned.add(DotName.createSimple(rmPackage + "Channel"));
        banned.add(DotName.createSimple(rmPackage + "OnOverflow"));
        String smallryePackage = "io.smallrye.reactive.messaging.annotations.";
        banned.add(DotName.createSimple(smallryePackage + "Blocking"));
        banned.add(DotName.createSimple(smallryePackage + "Broadcast"));
        banned.add(DotName.createSimple(smallryePackage + "Channel"));
        banned.add(DotName.createSimple(smallryePackage + "ConnectorAttribute"));
        banned.add(DotName.createSimple(smallryePackage + "ConnectorAttributes"));
        banned.add(DotName.createSimple(smallryePackage + "Incomings"));
        banned.add(DotName.createSimple(smallryePackage + "Merge"));
        banned.add(DotName.createSimple(smallryePackage + "OnOverflow"));
        BANNED_REACTIVE_MESSAGING_ANNOTATIONS = Collections.unmodifiableList(banned);
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (isReactiveMessagingDeployment(deploymentUnit)) {
            WildFlySecurityManager.doChecked(new PrivilegedAction() {
                @Override
                public Void run() {
                    System.setProperty(STRICT_MODE_PROPERTY, "true");
                    return null;
                }
            });
            addModuleDependencies(deploymentUnit);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addModuleDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.reactive-messaging.api", false, false, true, false));
        moduleSpecification.addSystemDependency(
                cdiDependency(
                        new ModuleDependency(moduleLoader, "io.smallrye.reactive.messaging", false, false, true, false)));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.config", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.config.api", false, false, true, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.reactivex.rxjava2.rxjava", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.mutiny.reactive-streams-operators", false, false, true, false));

        // These are optional über modules that export all the independent connectors/clients. However, it seems
        // to confuse the ExternalBeanArchiveProcessor which provides the modules to scan for beans, so we
        // load them and list them all individually instead
        addDependenciesForIntermediateModule(moduleSpecification, moduleLoader, "io.smallrye.reactive.messaging.connector");

        // Provisioned along with the connectors above
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.vertx.client", true, false, true, false));
    }

    private void addDependenciesForIntermediateModule(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader, String intermediateModuleName) {
        try {
            Module module = moduleLoader.loadModule(intermediateModuleName);
            for (DependencySpec dep : module.getDependencies()) {
                if (dep instanceof ModuleDependencySpec) {
                    ModuleDependencySpec mds = (ModuleDependencySpec) dep;
                    ModuleDependency md =
                            cdiDependency(
                                    new ModuleDependency(moduleLoader, mds.getName(), mds.isOptional(), false, true, false));
                    moduleSpecification.addSystemDependency(md);
                }
            }
        } catch (ModuleLoadException e) {
            // The module was not provisioned
            MicroProfileReactiveMessagingLogger.LOGGER.intermediateModuleNotPresent(intermediateModuleName);
        }
    }

    private boolean isReactiveMessagingDeployment(DeploymentUnit deploymentUnit) throws DeploymentUnitProcessingException {
        final boolean allowExperimentalAnnotations = allowExperimentalAnnotations();

        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        // Check the experimental annotations first
        for (DotName dotName : BANNED_REACTIVE_MESSAGING_ANNOTATIONS) {
            if (!index.getAnnotations(dotName).isEmpty()) {
                if (allowExperimentalAnnotations) {
                    MicroProfileReactiveMessagingLogger.LOGGER.debugf("Deployment '%s' is a MicroProfile Reactive Messaging deployment – @%s annotation found.", deploymentUnit.getName(), dotName);
                    return true;
                } else {
                    throw MicroProfileReactiveMessagingLogger.LOGGER.experimentalAnnotationNotAllowed(dotName);
                }
            }
        }

        for (DotName dotName : REACTIVE_MESSAGING_ANNOTATIONS) {
            if (!index.getAnnotations(dotName).isEmpty()) {
                MicroProfileReactiveMessagingLogger.LOGGER.debugf("Deployment '%s' is a MicroProfile Reactive Messaging deployment – @%s annotation found.", deploymentUnit.getName(), dotName);
                return true;
            }
        }

        MicroProfileReactiveMessagingLogger.LOGGER.debugf("Deployment '%s' is not a MicroProfile Reactive Messaging deployment.", deploymentUnit.getName());
        return false;
    }

    private ModuleDependency cdiDependency(ModuleDependency moduleDependency) {
        // This is needed following https://issues.redhat.com/browse/WFLY-13641 / https://github.com/wildfly/wildfly/pull/13406
        moduleDependency.addImportFilter(s -> s.equals("META-INF"), true);
        return moduleDependency;
    }

    private boolean allowExperimentalAnnotations() throws DeploymentUnitProcessingException {
        final boolean experimental;
        if (WildFlySecurityManager.isChecking()) {
            experimental = WildFlySecurityManager.doChecked((PrivilegedAction<Boolean>) () -> Boolean.getBoolean(EXPERIMENTAL_PROPERTY));
        } else {
            experimental = Boolean.getBoolean(EXPERIMENTAL_PROPERTY);
        }
        if (experimental && !allowExperimental) {
            throw MicroProfileReactiveMessagingLogger.LOGGER.experimentalPropertyNotAllowed(EXPERIMENTAL_PROPERTY);
        }
        return experimental;
    }
}
