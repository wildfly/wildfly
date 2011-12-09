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
package org.jboss.as.ejb3.deployment.processors.merging;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.tx.TransactionMethodAttribute;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;
import org.jboss.modules.Module;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * @author Stuart Douglas
 */
public class TransactionAttributeMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {


    public TransactionAttributeMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    private static TransactionMethodAttribute createFromMetaData(final ContainerTransactionMetaData metaData) {
        final TransactionAttributeType txAttr = metaData.getTransAttribute();
        return new TransactionMethodAttribute(txAttr);
    }

    private static TransactionMethodAttribute createFromTimeout(final TransactionMethodAttribute original, final TransactionTimeout timeout) {
        final TransactionMethodAttribute attr;
        if (original != null)
            attr = new TransactionMethodAttribute(original.getType(), timeout.value(), timeout.unit());
        else
            attr = new TransactionMethodAttribute(TransactionAttributeType.REQUIRED, timeout.value(), timeout.unit());
        return attr;
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        processTransactionAttributeAnnotation(applicationClasses, deploymentReflectionIndex, componentClass, null, componentConfiguration);
        processTransactionTimeoutAnnotation(applicationClasses, deploymentReflectionIndex, componentClass, null, componentConfiguration);
        for (ViewDescription view : componentConfiguration.getViews()) {

            try {
                final Class<?> viewClass = module.getClassLoader().loadClass(view.getViewClassName());
                EJBViewDescription ejbView = (EJBViewDescription) view;
                processTransactionAttributeAnnotation(applicationClasses, deploymentReflectionIndex, viewClass, ejbView.getMethodIntf(), componentConfiguration);
                processTransactionTimeoutAnnotation(applicationClasses, deploymentReflectionIndex, viewClass, ejbView.getMethodIntf(), componentConfiguration);
            } catch (ClassNotFoundException e) {
                throw MESSAGES.failToLoadEjbViewClass(e);
            }

        }
    }

    private void processTransactionAttributeAnnotation(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, MethodIntf methodIntf, final EJBComponentDescription componentConfiguration) {
        final RuntimeAnnotationInformation<TransactionAttributeType> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, TransactionAttribute.class);
        for (Map.Entry<String, List<TransactionAttributeType>> entry : data.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                //we can't specify both methodIntf and class name
                final String className = methodIntf == null ? entry.getKey() : null;
                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, className, new TransactionMethodAttribute(entry.getValue().get(0)));
            }
        }

        for (Map.Entry<Method, List<TransactionAttributeType>> entry : data.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final MethodIdentifier method = MethodIdentifier.getIdentifierForMethod(entry.getKey());
                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, new TransactionMethodAttribute(entry.getValue().get(0)), entry.getKey().getDeclaringClass().getName(), method.getName(), method.getParameterTypes());
            }
        }
    }

    private void processTransactionTimeoutAnnotation(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, MethodIntf methodIntf, final EJBComponentDescription componentConfiguration) {
        final RuntimeAnnotationInformation<TransactionTimeout> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, TransactionTimeout.class);
        for (Map.Entry<String, List<TransactionTimeout>> entry : data.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                //we can't specify both methodIntf and class name
                final String className = methodIntf == null ? entry.getKey() : null;
                // merge with TransactionAttribute
                final TransactionMethodAttribute attr = createFromTimeout(componentConfiguration.getTransactionAttributes().getAttributeStyle1(methodIntf, className), entry.getValue().get(0));
                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, className, attr);
            }
        }

        for (Map.Entry<Method, List<TransactionTimeout>> entry : data.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final MethodIdentifier method = MethodIdentifier.getIdentifierForMethod(entry.getKey());
                final String className = entry.getKey().getDeclaringClass().getName();
                // merge with TransactionAttribute
                final TransactionMethodAttribute attr = createFromTimeout(componentConfiguration.getTransactionAttributes().getAttributeStyle3(methodIntf, className, method.getName(), method.getParameterTypes()), entry.getValue().get(0));
                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, attr, className, method.getName(), method.getParameterTypes());
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        // CMT Tx attributes

        //DO NOT USE componentConfiguration.getDescriptorData()
        //It will return null if there is no <enterprise-beans/> declaration even if there is an assembly descriptor entry

        EjbJarMetaData ejbJarMetadata = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetadata != null) {
            final AssemblyDescriptorMetaData assemblyDescriptor = ejbJarMetadata.getAssemblyDescriptor();
            if (assemblyDescriptor != null) {
                final ContainerTransactionsMetaData containerTransactions = assemblyDescriptor.getContainerTransactionsByEjbName(componentConfiguration.getEJBName());
                if (containerTransactions != null) {
                    for (final ContainerTransactionMetaData containerTx : containerTransactions) {
                        final MethodsMetaData methods = containerTx.getMethods();
                        for (final MethodMetaData method : methods) {
                            final String methodName = method.getMethodName();
                            final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf());
                            if (methodName.equals("*")) {
                                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, null, createFromMetaData(containerTx));
                            } else {

                                final MethodParametersMetaData methodParams = method.getMethodParams();
                                // update the session bean description with the tx attribute info
                                if (methodParams == null) {
                                    componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, createFromMetaData(containerTx), methodName);
                                } else {

                                    componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, createFromMetaData(containerTx), null, methodName, this.getMethodParams(methodParams));
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
