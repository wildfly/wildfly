/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.EjbJar3xMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.InterceptorBindingMetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.NamedMethodMetaData;
import org.jboss.modules.Module;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processor that handles interceptor bindings that are defined in the deployment descriptor.
 *
 * @author Stuart Douglas
 */
public class DeploymentDescriptorInterceptorBindingsProcessor implements DeploymentUnitProcessor {


    private static final Logger log = Logger.getLogger(DeploymentDescriptorInterceptorBindingsProcessor.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);


        if (ejbJarMetaData == null) {
            return;
        }
        if (!(ejbJarMetaData instanceof EjbJar3xMetaData)) {
            return;
        }

        final EjbJar3xMetaData metaData = (EjbJar3xMetaData) ejbJarMetaData;
        if (metaData.getAssemblyDescriptor() == null) {
            return;
        }
        if (metaData.getAssemblyDescriptor().getInterceptorBindings() == null) {
            return;
        }

        //default interceptors must be mentioned in the interceptors section
        final Set<String> interceptorClasses = new HashSet<String>();
        if (metaData.getInterceptors() != null) {
            for (final InterceptorMetaData interceptor : metaData.getInterceptors()) {
                interceptorClasses.add(interceptor.getInterceptorClass());
            }
        }

        final Map<String, List<InterceptorBindingMetaData>> bindingsPerComponent = new HashMap<String, List<InterceptorBindingMetaData>>();
        final List<InterceptorBindingMetaData> defaultInterceptorBindings = new ArrayList<InterceptorBindingMetaData>();

        for (final InterceptorBindingMetaData binding : metaData.getAssemblyDescriptor().getInterceptorBindings()) {
            if (binding.getEjbName().equals("*")) {
                if (binding.getMethod() != null) {
                    throw new DeploymentUnitProcessingException("Default interceptors cannot specify a method to bind to in ejb-jar.xml");
                }
                defaultInterceptorBindings.add(binding);
            } else {
                List<InterceptorBindingMetaData> bindings = bindingsPerComponent.get(binding.getEjbName());
                if (bindings == null) {
                    bindingsPerComponent.put(binding.getEjbName(), bindings = new ArrayList<InterceptorBindingMetaData>());
                }
                bindings.add(binding);
            }
        }


        final List<InterceptorDescription> defaultInterceptors = new ArrayList<InterceptorDescription>();

        for (InterceptorBindingMetaData binding : defaultInterceptorBindings) {
            if (binding.getInterceptorClasses() != null) {
                for (final String clazz : binding.getInterceptorClasses()) {
                    //we only want default interceptors referenced in the interceptors section
                    if (interceptorClasses.contains(clazz)) {
                        defaultInterceptors.add(new InterceptorDescription(clazz));
                    } else {
                        log.warnf("Default interceptor class %s is not listed in the <interceptors> section of ejb-jar.xml and will not be applied", clazz);
                    }
                }
            }
        }

        //now we need to process the components, and add interceptor information
        //we iterate over all components, as we need to process default interceptors
        for (final ComponentDescription componentDescription : eeModuleDescription.getComponentDescriptions()) {

            final Class<?> componentClass;
            try {
                componentClass = module.getClassLoader().loadClass(componentDescription.getComponentClassName());
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException("Could not load component class " + componentDescription.getComponentClassName());
            }

            final List<InterceptorBindingMetaData> bindings = bindingsPerComponent.get(componentDescription.getComponentName());
            final Map<Method, List<InterceptorBindingMetaData>> methodInterceptors = new HashMap<Method, List<InterceptorBindingMetaData>>();
            final List<InterceptorBindingMetaData> classLevelBindings = new ArrayList<InterceptorBindingMetaData>();
            //we only want to exclude default and class level interceptors if every binding
            //has the exclude element.
            Boolean classLevelExcludeDefaultInterceptors = null;
            Map<Method, Boolean> methodLevelExcludeDefaultInterceptors = new HashMap<Method, Boolean>();
            Map<Method, Boolean> methodLevelExcludeClassInterceptors = new HashMap<Method, Boolean>();

            //if an absolute order has been defined at any level
            //absolute ordering takes precedence
            boolean classLevelAbsoluteOrder = false;
            final Map<Method, Boolean> methodLevelAbsoluteOrder = new HashMap<Method, Boolean>();


            if (bindings != null) {
                for (final InterceptorBindingMetaData binding : bindings) {
                    if (binding.getMethod() == null) {
                        classLevelBindings.add(binding);
                        //if even one binding does not say exclude default then we do not exclude
                        if (binding.isExcludeDefaultInterceptors() && classLevelExcludeDefaultInterceptors == null) {
                            classLevelExcludeDefaultInterceptors = true;
                        } else if (!binding.isExcludeClassInterceptors()) {
                            classLevelExcludeDefaultInterceptors = false;
                        }
                        if (binding.isTotalOrdering()) {
                            if (classLevelAbsoluteOrder) {
                                throw new DeploymentUnitProcessingException("Two ejb-jar.xml bindings for " + componentClass + " specify an absolute order");
                            } else {
                                classLevelAbsoluteOrder = true;
                            }
                        }
                    } else {

                        //method level bindings
                        //first find the right method
                        final NamedMethodMetaData methodData = binding.getMethod();
                        final ClassReflectionIndex<?> classIndex = index.getClassIndex(componentClass);
                        Method resolvedMethod = null;
                        if (methodData.getMethodParams() == null) {
                            final Collection<Method> methods = classIndex.getAllMethods(methodData.getMethodName());
                            if (methods.isEmpty()) {
                                throw new DeploymentUnitProcessingException("Could not find method" + componentClass.getName() + "." + methodData.getMethodName() + " referenced in ejb-jar.xml");
                            } else if (methods.size() > 1) {
                                throw new DeploymentUnitProcessingException("More than one method " + methodData.getMethodName() + "found on class" + componentClass.getName() + " referenced in ejb-jar.xml. Specify the parameter types to resolve the ambiguity");
                            }
                            resolvedMethod = methods.iterator().next();
                        } else {
                            final Collection<Method> methods = classIndex.getAllMethods(methodData.getMethodName(), methodData.getMethodParams().size());
                            for (final Method method : methods) {
                                boolean match = true;
                                for (int i = 0; i < method.getParameterTypes().length; ++i) {
                                    if (!method.getParameterTypes()[i].getName().equals(methodData.getMethodParams().get(i))) {
                                        match = false;
                                        break;
                                    }
                                }
                                if (match) {
                                    resolvedMethod = method;
                                    break;
                                }
                            }
                            if (resolvedMethod == null) {
                                throw new DeploymentUnitProcessingException("Could not find method" + componentClass.getName() + "." + methodData.getMethodName() + "with parameter types" + methodData.getMethodParams() + " referenced in ejb-jar.xml");
                            }
                        }
                        List<InterceptorBindingMetaData> list = methodInterceptors.get(resolvedMethod);
                        if (list == null) {
                            methodInterceptors.put(resolvedMethod, list = new ArrayList<InterceptorBindingMetaData>());
                        }
                        list.add(binding);
                        if (binding.isExcludeDefaultInterceptors() && !methodLevelExcludeDefaultInterceptors.containsKey(resolvedMethod)) {
                            methodLevelExcludeDefaultInterceptors.put(resolvedMethod, true);
                        } else if (!binding.isExcludeDefaultInterceptors()) {
                            methodLevelExcludeDefaultInterceptors.put(resolvedMethod, false);
                        }
                        if (binding.isExcludeClassInterceptors() && !methodLevelExcludeClassInterceptors.containsKey(resolvedMethod)) {
                            methodLevelExcludeClassInterceptors.put(resolvedMethod, true);
                        } else if (!binding.isExcludeClassInterceptors()) {
                            methodLevelExcludeClassInterceptors.put(resolvedMethod, false);
                        }

                        if (binding.isTotalOrdering()) {
                            if (methodLevelAbsoluteOrder.containsKey(resolvedMethod)) {
                                throw new DeploymentUnitProcessingException("Two ejb-jar.xml bindings for " + resolvedMethod + " specify an absolute order");
                            } else {
                                methodLevelAbsoluteOrder.put(resolvedMethod, true);
                            }
                        }

                    }
                }
            }

            //now we have all the bindings in a format we can use
            //build the list of default interceptors
            componentDescription.setDefaultInterceptors(defaultInterceptors);

            boolean classLevelExclude = classLevelExcludeDefaultInterceptors == null ? false : classLevelExcludeDefaultInterceptors;
            if(classLevelExclude) {
                componentDescription.setExcludeDefaultInterceptors(true);
            }

            final List<InterceptorDescription> classLevelInterceptors = new ArrayList<InterceptorDescription>();
            if (classLevelAbsoluteOrder) {
                //we have an absolute ordering for the class level interceptors
                for (final InterceptorBindingMetaData binding : classLevelBindings) {
                    if (binding.isTotalOrdering()) {
                        for (final String interceptor : binding.getInterceptorOrder()) {
                            classLevelInterceptors.add(new InterceptorDescription(interceptor));
                        }
                    }
                }
                //we have merged the default interceptors into the class interceptors
                componentDescription.setExcludeDefaultInterceptors(true);
            } else {
                //the order we want is default interceptors (this will be empty if they are excluded)
                //the annotation interceptors
                //then dd interceptors
                classLevelInterceptors.addAll(componentDescription.getClassInterceptors());
                for (InterceptorBindingMetaData binding : classLevelBindings) {
                    if (binding.getInterceptorClasses() != null) {
                        for (final String interceptor : binding.getInterceptorClasses()) {
                            classLevelInterceptors.add(new InterceptorDescription(interceptor));
                        }
                    }
                }
            }
            componentDescription.setClassInterceptors(classLevelInterceptors);

            for (Map.Entry<Method, List<InterceptorBindingMetaData>> entry : methodInterceptors.entrySet()) {
                final Method method = entry.getKey();
                final List<InterceptorBindingMetaData> methodBindings = entry.getValue();
                boolean totalOrder = methodLevelAbsoluteOrder.containsKey(method);
                final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifierForMethod(method);

                Boolean excludeDefaultInterceptors = methodLevelExcludeDefaultInterceptors.get(method);
                excludeDefaultInterceptors = excludeDefaultInterceptors == null ? false : excludeDefaultInterceptors;
                if (!excludeDefaultInterceptors) {
                    excludeDefaultInterceptors = componentDescription.isExcludeDefaultInterceptors(methodIdentifier);
                }

                Boolean excludeClassInterceptors = methodLevelExcludeClassInterceptors.get(method);
                excludeClassInterceptors = excludeClassInterceptors == null ? false : excludeClassInterceptors;
                if (!excludeClassInterceptors) {
                    excludeClassInterceptors = componentDescription.isExcludeClassInterceptors(methodIdentifier);
                }

                final List<InterceptorDescription> methodLevelInterceptors = new ArrayList<InterceptorDescription>();

                if (totalOrder) {
                    //if there is a total order we just use it
                    for (final InterceptorBindingMetaData binding : methodBindings) {
                        if (binding.isTotalOrdering()) {
                            for (final String interceptor : binding.getInterceptorOrder()) {
                                methodLevelInterceptors.add(new InterceptorDescription(interceptor));
                            }
                        }
                    }
                } else {
                    //add class level and default interceptors, if not excluded
                    //class level interceptors also includes default interceptors
                    if (!excludeDefaultInterceptors) {
                        methodLevelInterceptors.addAll(defaultInterceptors);
                    }
                    if (!excludeClassInterceptors) {
                        for (InterceptorDescription interceptor : classLevelInterceptors) {
                            methodLevelInterceptors.add(interceptor);
                        }
                    }
                    List<InterceptorDescription> annotationMethodLevel = componentDescription.getMethodInterceptors().get(methodIdentifier);
                    if (annotationMethodLevel != null) {
                        methodLevelInterceptors.addAll(annotationMethodLevel);
                    }
                    //now add all the interceptors from the bindings
                    for (InterceptorBindingMetaData binding : methodBindings) {
                        if (binding.getInterceptorClasses() != null) {
                            for (final String interceptor : binding.getInterceptorClasses()) {
                                methodLevelInterceptors.add(new InterceptorDescription(interceptor));
                            }
                        }
                    }

                }
                //we have already taken component and default interceptors into account
                componentDescription.excludeClassInterceptors(methodIdentifier);
                componentDescription.excludeDefaultInterceptors(methodIdentifier);
                componentDescription.setMethodInterceptors(methodIdentifier, methodLevelInterceptors);

            }

        }


    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
