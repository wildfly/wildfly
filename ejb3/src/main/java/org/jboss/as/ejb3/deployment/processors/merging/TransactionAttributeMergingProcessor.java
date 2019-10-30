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
import java.util.concurrent.TimeUnit;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.messagedriven.MessageDrivenComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.util.MethodInfoHelper;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.ejb3.annotation.TransactionTimeout;
import org.jboss.metadata.ejb.jboss.ejb3.TransactionTimeoutMetaData;
import org.jboss.metadata.ejb.spec.AssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;

/**
 * Because trans-attr and trans-timeout are both contained in container-transaction
 * process both annotations and container-transaction metadata in one spot.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionAttributeMergingProcessor extends AbstractMergingProcessor<EJBComponentDescription> {

    public TransactionAttributeMergingProcessor() {
        super(EJBComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentConfiguration) throws DeploymentUnitProcessingException {
        processTransactionAttributeAnnotation(applicationClasses, deploymentReflectionIndex, componentClass, null, componentConfiguration);
        processTransactionTimeoutAnnotation(applicationClasses, deploymentReflectionIndex, componentClass, null, componentConfiguration);
    }

    private void processTransactionAttributeAnnotation(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, MethodIntf methodIntf, final EJBComponentDescription componentConfiguration) {
        final RuntimeAnnotationInformation<TransactionAttributeType> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, TransactionAttribute.class);
        for (Map.Entry<String, List<TransactionAttributeType>> entry : data.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                //we can't specify both methodIntf and class name
                final String className = methodIntf == null ? entry.getKey() : null;
                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, className, entry.getValue().get(0));
            }
        }

        for (Map.Entry<Method, List<TransactionAttributeType>> entry : data.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                String[] parameterTypes = MethodInfoHelper.getCanonicalParameterTypes(entry.getKey());
                componentConfiguration.getTransactionAttributes().setAttribute(methodIntf, entry.getValue().get(0), entry.getKey().getDeclaringClass().getName(), entry.getKey().getName(), parameterTypes);
            }
        }
    }

    private void processTransactionTimeoutAnnotation(final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, MethodIntf methodIntf, final EJBComponentDescription componentConfiguration) {
        final RuntimeAnnotationInformation<Integer> data = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, TransactionTimeout.class);
        for (Map.Entry<String, List<Integer>> entry : data.getClassAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                //we can't specify both methodIntf and class name
                final String className = methodIntf == null ? entry.getKey() : null;
                componentConfiguration.getTransactionTimeouts().setAttribute(methodIntf, className, entry.getValue().get(0));
            }
        }

        for (Map.Entry<Method, List<Integer>> entry : data.getMethodAnnotations().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                final String className = entry.getKey().getDeclaringClass().getName();
                String[] parameterTypes = MethodInfoHelper.getCanonicalParameterTypes(entry.getKey());
                componentConfiguration.getTransactionTimeouts().setAttribute(methodIntf, entry.getValue().get(0), className, entry.getKey().getName(), parameterTypes);
            }
        }
    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final EJBComponentDescription componentDescription) throws DeploymentUnitProcessingException {
        // CMT Tx attributes

        //DO NOT USE componentConfiguration.getDescriptorData()
        //It will return null if there is no <enterprise-beans/> declaration even if there is an assembly descriptor entry

        EjbJarMetaData ejbJarMetadata = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetadata != null) {

            boolean wildcardAttributeSet = false;
            boolean wildcardTimeoutSet = false;
            ContainerTransactionMetaData global = null;
            final AssemblyDescriptorMetaData assemblyDescriptor = ejbJarMetadata.getAssemblyDescriptor();
            if(assemblyDescriptor != null) {
                final ContainerTransactionsMetaData globalTransactions = assemblyDescriptor.getContainerTransactionsByEjbName("*");

                if (globalTransactions != null) {
                    if (globalTransactions.size() > 1) {
                        throw EjbLogger.ROOT_LOGGER.mustOnlyBeSingleContainerTransactionElementWithWildcard();
                    }
                    global = globalTransactions.iterator().next();
                    for(MethodMetaData method : global.getMethods()) {
                        if(!method.getMethodName().equals("*")) {
                            throw EjbLogger.ROOT_LOGGER.wildcardContainerTransactionElementsMustHaveWildcardMethodName();
                        }
                    }
                }
                final ContainerTransactionsMetaData containerTransactions = assemblyDescriptor.getContainerTransactionsByEjbName(componentDescription.getEJBName());
                if (containerTransactions != null) {
                    for (final ContainerTransactionMetaData containerTx : containerTransactions) {
                        final TransactionAttributeType txAttr = containerTx.getTransAttribute();
                        final Integer timeout = timeout(containerTx);
                        final MethodsMetaData methods = containerTx.getMethods();
                        for (final MethodMetaData method : methods) {
                            final String methodName = method.getMethodName();
                            final MethodIntf defaultMethodIntf = (componentDescription instanceof MessageDrivenComponentDescription) ? MethodIntf.MESSAGE_ENDPOINT : MethodIntf.BEAN;
                            final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf(), defaultMethodIntf);
                            if (methodName.equals("*")) {
                                if (txAttr != null){
                                    wildcardAttributeSet = true;
                                    componentDescription.getTransactionAttributes().setAttribute(methodIntf, null, txAttr);
                                }
                                if (timeout != null) {
                                    wildcardTimeoutSet = true;
                                    componentDescription.getTransactionTimeouts().setAttribute(methodIntf, null, timeout);
                                }
                            } else {

                                final MethodParametersMetaData methodParams = method.getMethodParams();
                                // update the session bean description with the tx attribute info
                                if (methodParams == null) {
                                    if (txAttr != null)
                                        componentDescription.getTransactionAttributes().setAttribute(methodIntf, txAttr, methodName);
                                    if (timeout != null)
                                        componentDescription.getTransactionTimeouts().setAttribute(methodIntf, timeout, methodName);
                                } else {
                                    if (txAttr != null)
                                        componentDescription.getTransactionAttributes().setAttribute(methodIntf, txAttr, null, methodName, this.getMethodParams(methodParams));
                                    if (timeout != null)
                                        componentDescription.getTransactionTimeouts().setAttribute(methodIntf, timeout, null, methodName, this.getMethodParams(methodParams));
                                }
                            }
                        }
                    }
                }
            }
            if(global != null) {
                if(!wildcardAttributeSet && global.getTransAttribute() != null) {
                    for(MethodMetaData method : global.getMethods()) {
                        final MethodIntf defaultMethodIntf = (componentDescription instanceof MessageDrivenComponentDescription) ? MethodIntf.MESSAGE_ENDPOINT : MethodIntf.BEAN;
                        final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf(), defaultMethodIntf);
                        componentDescription.getTransactionAttributes().setAttribute(methodIntf, null, global.getTransAttribute());
                    }
                }
                final Integer timeout = timeout(global);
                if(!wildcardTimeoutSet && timeout != null) {
                    for(MethodMetaData method : global.getMethods()) {
                        final MethodIntf defaultMethodIntf = (componentDescription instanceof MessageDrivenComponentDescription) ? MethodIntf.MESSAGE_ENDPOINT : MethodIntf.BEAN;
                        final MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf(), defaultMethodIntf);
                        componentDescription.getTransactionTimeouts().setAttribute(methodIntf, null, timeout);
                    }
                }
            }
        }

    }

    private static Integer timeout(final ContainerTransactionMetaData containerTransaction) {
        final List<TransactionTimeoutMetaData> transactionTimeouts = containerTransaction.getAny(TransactionTimeoutMetaData.class);
        if (transactionTimeouts == null || transactionTimeouts.isEmpty())
            return null;
        final TransactionTimeoutMetaData transactionTimeout = transactionTimeouts.get(0);
        final TimeUnit unit = transactionTimeout.getUnit() == null ? TimeUnit.SECONDS : transactionTimeout.getUnit();
        return (int)unit.toSeconds(transactionTimeout.getTimeout());
    }
}
