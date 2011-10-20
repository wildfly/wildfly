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

package org.jboss.as.ejb3.deployment.processors;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.deployers.AbstractDeploymentDescriptorBindingsProcessor;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.javaee.spec.EJBLocalReferenceMetaData;
import org.jboss.metadata.javaee.spec.EJBLocalReferencesMetaData;
import org.jboss.metadata.javaee.spec.EJBReferenceMetaData;
import org.jboss.metadata.javaee.spec.EJBReferencesMetaData;
import org.jboss.metadata.javaee.spec.Environment;
import org.jboss.metadata.javaee.spec.RemoteEnvironment;

/**
 * Deployment processor responsible for processing ejb references from deployment descriptors
 *
 * @author Stuart Douglas
 */
public class EjbRefProcessor extends AbstractDeploymentDescriptorBindingsProcessor {

    /**
     * Resolves ejb-ref and ejb-local-ref elements
     *
     * @param deploymentUnit
     * @param environment               The environment to resolve the elements for
     * @param classLoader               The deployment class loader
     * @param deploymentReflectionIndex The reflection index
     * @param applicationClasses
     * @return The bindings for the environment entries
     */
    protected List<BindingConfiguration> processDescriptorEntries(DeploymentUnit deploymentUnit, DeploymentDescriptorEnvironment environment, EEModuleDescription moduleDescription, ComponentDescription componentDescription, ClassLoader classLoader, DeploymentReflectionIndex deploymentReflectionIndex, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        final RemoteEnvironment remoteEnvironment = environment.getEnvironment();
        final DeploymentClassIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);
        List<BindingConfiguration> bindingDescriptions = new ArrayList<BindingConfiguration>();

        EJBReferencesMetaData ejbRefs = remoteEnvironment.getEjbReferences();
        if (ejbRefs != null) {
            for (EJBReferenceMetaData ejbRef : ejbRefs) {
                String name = ejbRef.getEjbRefName();
                String ejbName = ejbRef.getLink();
                String lookup = ejbRef.getLookupName();
                String remoteInterface = ejbRef.getRemote();
                String home = ejbRef.getHome();
                Class<?> remoteInterfaceType = null;

                //if a home is specified this is the type that is bound
                if (!isEmpty(home)) {
                    try {
                        remoteInterfaceType = index.classIndex(home).getModuleClass();
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException("Could not load home interface type " + home, e);
                    }
                } else if (!isEmpty(remoteInterface)) {
                    try {
                        remoteInterfaceType = index.classIndex(remoteInterface).getModuleClass();
                    } catch (ClassNotFoundException e) {
                        throw new DeploymentUnitProcessingException("Could not load remote interface type " + remoteInterface, e);
                    }
                }

                if (!name.startsWith("java:")) {
                    name = environment.getDefaultContext() + name;
                }

                // our injection (source) comes from the local (ENC) lookup, no matter what.
                LookupInjectionSource injectionSource = new LookupInjectionSource(name);

                //add any injection targets
                remoteInterfaceType = processInjectionTargets(moduleDescription, componentDescription, applicationClasses, injectionSource, classLoader, deploymentReflectionIndex, ejbRef, remoteInterfaceType);

                if (remoteInterfaceType == null) {
                    throw new DeploymentUnitProcessingException("Could not determine type of ejb-ref " + name + " for component " + componentDescription);
                }
                final BindingConfiguration bindingConfiguration;
                EjbInjectionSource ejbInjectionSource = null;

                if (!isEmpty(lookup)) {
                    if (lookup.startsWith("ejb:")) {
                        bindingConfiguration = new BindingConfiguration(name, new EjbLookupInjectionSource(lookup));
                    } else {
                        bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(lookup));
                    }
                } else if (!isEmpty(ejbName)) {
                    bindingConfiguration = new BindingConfiguration(name, ejbInjectionSource = new EjbInjectionSource(ejbName, remoteInterfaceType.getName()));
                } else {
                    bindingConfiguration = new BindingConfiguration(name, ejbInjectionSource = new EjbInjectionSource(remoteInterfaceType.getName()));
                }
                if (ejbInjectionSource != null) {
                    deploymentUnit.addToAttachmentList(EjbDeploymentAttachmentKeys.EJB_INJECTIONS, ejbInjectionSource);
                }
                bindingDescriptions.add(bindingConfiguration);
            }
        }

        if (remoteEnvironment instanceof Environment) {
            EJBLocalReferencesMetaData ejbLocalRefs = ((Environment) remoteEnvironment).getEjbLocalReferences();
            if (ejbLocalRefs != null) {
                for (EJBLocalReferenceMetaData ejbRef : ejbLocalRefs) {
                    String name = ejbRef.getEjbRefName();
                    String ejbName = ejbRef.getLink();
                    String lookup = ejbRef.getLookupName();
                    String localInterface = ejbRef.getLocal();
                    String localHome = ejbRef.getLocalHome();
                    Class<?> localInterfaceType = null;

                    //if a home is specified this is the type that is bound
                    if (!isEmpty(localHome)) {
                        try {
                            localInterfaceType = index.classIndex(localHome).getModuleClass();
                        } catch (ClassNotFoundException e) {
                            throw new DeploymentUnitProcessingException("Could not load local home interface type " + localHome, e);
                        }
                    } else if (!isEmpty(localInterface)) {
                        try {
                            localInterfaceType = classLoader.loadClass(localInterface);
                        } catch (ClassNotFoundException e) {
                            throw new DeploymentUnitProcessingException("Could not load local interface type " + localInterface, e);
                        }
                    }

                    if (!name.startsWith("java:")) {
                        name = environment.getDefaultContext() + name;
                    }

                    // our injection (source) comes from the local (ENC) lookup, no matter what.
                    LookupInjectionSource injectionSource = new LookupInjectionSource(name);

                    //add any injection targets
                    localInterfaceType = processInjectionTargets(moduleDescription, componentDescription, applicationClasses, injectionSource, classLoader, deploymentReflectionIndex, ejbRef, localInterfaceType);

                    if (localInterfaceType == null) {
                        throw new DeploymentUnitProcessingException("Could not determine type of ejb-local-ref " + name + " for component " + componentDescription);
                    }
                    final BindingConfiguration bindingConfiguration;
                    EjbInjectionSource ejbInjectionSource = null;

                    if (!isEmpty(lookup)) {
                        if (lookup.startsWith("ejb:")) {
                            bindingConfiguration = new BindingConfiguration(name, new EjbLookupInjectionSource(lookup));
                        } else {
                            bindingConfiguration = new BindingConfiguration(name, new LookupInjectionSource(lookup));
                        }
                    } else if (!isEmpty(ejbName)) {
                        bindingConfiguration = new BindingConfiguration(name, ejbInjectionSource = new EjbInjectionSource(ejbName, localInterfaceType.getName()));
                    } else {
                        bindingConfiguration = new BindingConfiguration(name, ejbInjectionSource = new EjbInjectionSource(localInterfaceType.getName()));
                    }
                    if (ejbInjectionSource != null) {
                        deploymentUnit.addToAttachmentList(EjbDeploymentAttachmentKeys.EJB_INJECTIONS, ejbInjectionSource);
                    }
                    bindingDescriptions.add(bindingConfiguration);
                }
            }
        }
        return bindingDescriptions;
    }

    private boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

}
