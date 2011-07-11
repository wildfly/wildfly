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

package org.jboss.as.ejb3.deployment.processors.dd;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.AccessTimeoutMetaData;
import org.jboss.metadata.ejb.spec.AroundInvokeMetaData;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.ejb.spec.StatefulTimeoutMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

/**
 * @author Jaikiran Pai
 */
public class SessionBeanXmlDescriptorProcessor extends AbstractEjbXmlDescriptorProcessor<SessionBeanMetaData> {

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(SessionBeanXmlDescriptorProcessor.class);

    @Override
    protected Class<SessionBeanMetaData> getMetaDataType() {
        return SessionBeanMetaData.class;
    }

    /**
     * Processes the passed {@link org.jboss.metadata.ejb.spec.SessionBeanMetaData} and creates appropriate {@link org.jboss.as.ejb3.component.session.SessionBeanComponentDescription} out of it.
     * The {@link org.jboss.as.ejb3.component.session.SessionBeanComponentDescription} is then added to the {@link org.jboss.as.ee.component.EEModuleDescription module description} available
     * in the deployment unit of the passed {@link DeploymentPhaseContext phaseContext}
     *
     * @param sessionBean  The session bean metadata
     * @param phaseContext
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    protected void processBeanMetaData(SessionBeanMetaData sessionBean, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EjbJarDescription ejbJarDescription = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        final EEApplicationClasses applicationClassesDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        // get the module description
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = moduleDescription.getApplicationName();

        final String beanName = sessionBean.getName();
        final SessionBeanComponentDescription sessionBeanDescription = (SessionBeanComponentDescription) moduleDescription.getComponentByName(beanName);

        sessionBeanDescription.setDeploymentDescriptorEnvironment(new DeploymentDescriptorEnvironment("java:comp/env/", sessionBean));

        // mapped-name
        sessionBeanDescription.setMappedName(sessionBean.getMappedName());
        // local business interface views
        BusinessLocalsMetaData businessLocals = sessionBean.getBusinessLocals();
        if (businessLocals != null && !businessLocals.isEmpty()) {
            sessionBeanDescription.addLocalBusinessInterfaceViews(businessLocals);
        }
        // remote business interface views
        BusinessRemotesMetaData businessRemotes = sessionBean.getBusinessRemotes();
        if (businessRemotes != null && !businessRemotes.isEmpty()) {
            sessionBeanDescription.addRemoteBusinessInterfaceViews(businessRemotes);
        }
        // tx management type
        if (sessionBean.getTransactionType() != null) {
            sessionBeanDescription.setTransactionManagementType(sessionBean.getTransactionType());
        }
        // CMT Tx attributes
        if (sessionBean.getTransactionType() != TransactionManagementType.BEAN) {
            ContainerTransactionsMetaData containerTransactions = sessionBean.getContainerTransactions();
            if (containerTransactions != null && !containerTransactions.isEmpty()) {
                final String className = null; // applies to any class
                for (ContainerTransactionMetaData containerTx : containerTransactions) {
                    TransactionAttributeType txAttr = containerTx.getTransAttribute();
                    MethodsMetaData methods = containerTx.getMethods();
                    for (MethodMetaData method : methods) {
                        String methodName = method.getMethodName();
                        MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf());
                        if (methodName.equals("*")) {
                            sessionBeanDescription.setTransactionAttribute(methodIntf, className, txAttr);
                        } else {

                            MethodParametersMetaData methodParams = method.getMethodParams();
                            // update the session bean description with the tx attribute info
                            sessionBeanDescription.setTransactionAttribute(methodIntf, txAttr, className, methodName, this.getMethodParams(methodParams));
                        }
                    }
                }
            }
        }

        // interceptors
        this.processInterceptors(sessionBean, sessionBeanDescription, applicationClassesDescription);

        // process EJB3.1 specific session bean description
        if (sessionBean instanceof SessionBean31MetaData) {
            this.processSessionBean31((SessionBean31MetaData) sessionBean, sessionBeanDescription);
        }
    }

    private static void processSessionSynchronization(final SessionBean31MetaData metaData, final SessionBeanComponentDescription sessionBeanComponentDescription) {
        if (!(sessionBeanComponentDescription instanceof StatefulComponentDescription))
            return;
        final StatefulComponentDescription description = (StatefulComponentDescription) sessionBeanComponentDescription;

        if (metaData.getAfterBeginMethod() != null)
            description.setAfterBegin(null, metaData.getAfterBeginMethod().getMethodName());
        if (metaData.getAfterCompletionMethod() != null)
            description.setAfterCompletion(null, metaData.getAfterCompletionMethod().getMethodName());
        if (metaData.getBeforeCompletionMethod() != null)
            description.setBeforeCompletion(null, metaData.getBeforeCompletionMethod().getMethodName());
    }

    protected void processInterceptors(SessionBeanMetaData enterpriseBean, EJBComponentDescription ejbComponentDescription, final EEApplicationClasses applicationClassesDescription) {

        //for interceptor methods that specify a null class we cannot deal with them here
        //instead we stick them on the component configuration, and deal with them once we have a module

        if (enterpriseBean.getAroundInvokes() != null) {
            for (AroundInvokeMetaData interceptor : enterpriseBean.getAroundInvokes()) {

                if (interceptor.getClassName() == null) {
                    ejbComponentDescription.getAroundInvokeDDMethods().add(interceptor.getMethodName());
                } else {
                    EEModuleClassDescription interceptorModuleClassDescription = applicationClassesDescription.getOrAddClassByName(interceptor.getClassName());
                    final MethodIdentifier identifier = MethodIdentifier.getIdentifier(Object.class, interceptor.getMethodName(), InvocationContext.class);
                    interceptorModuleClassDescription.setAroundInvokeMethod(identifier);
                }
            }
        }
        if (enterpriseBean.getPreDestroys() != null) {
            for (LifecycleCallbackMetaData interceptor : enterpriseBean.getPreDestroys()) {
                if (interceptor.getClassName() == null) {
                    ejbComponentDescription.getPreDestroyDDMethods().add(interceptor.getMethodName());
                } else {
                    final EEModuleClassDescription interceptorModuleClassDescription = applicationClassesDescription.getOrAddClassByName(interceptor.getClassName());
                    final MethodIdentifier identifier = MethodIdentifier.getIdentifier(Object.class, interceptor.getMethodName(), InvocationContext.class);
                    interceptorModuleClassDescription.setPreDestroyMethod(identifier);
                }
            }
        }

        if (enterpriseBean.getPostConstructs() != null) {
            for (LifecycleCallbackMetaData interceptor : enterpriseBean.getPostConstructs()) {
                if (interceptor.getClassName() == null) {
                    ejbComponentDescription.getPostConstructDDMethods().add(interceptor.getMethodName());
                } else {
                    final EEModuleClassDescription interceptorModuleClassDescription = applicationClassesDescription.getOrAddClassByName(interceptor.getClassName());
                    final MethodIdentifier identifier = MethodIdentifier.getIdentifier(Object.class, interceptor.getMethodName(), InvocationContext.class);
                    interceptorModuleClassDescription.setPostConstructMethod(identifier);
                }
            }
        }

    }


    private void processSessionBean31(SessionBean31MetaData sessionBean31MetaData, SessionBeanComponentDescription sessionBeanComponentDescription) {
        // no-interface view
        if (sessionBean31MetaData.isNoInterfaceBean()) {
            sessionBeanComponentDescription.addNoInterfaceView();
        }
        // process singleton bean specific description
        if (sessionBean31MetaData.isSingleton() && sessionBeanComponentDescription instanceof SingletonComponentDescription) {
            this.processSingletonBean(sessionBean31MetaData, (SingletonComponentDescription) sessionBeanComponentDescription);
        }
        if(sessionBean31MetaData.isStateful() && sessionBeanComponentDescription instanceof StatefulComponentDescription) {
            processStatefulBean(sessionBean31MetaData, (StatefulComponentDescription)sessionBeanComponentDescription);
        }

        processSessionSynchronization(sessionBean31MetaData, sessionBeanComponentDescription);
    }

    private void processStatefulBean(final SessionBean31MetaData sessionBean31MetaData, final StatefulComponentDescription sessionBeanComponentDescription) {
        final StatefulTimeoutMetaData statefulTimeout = sessionBean31MetaData.getStatefulTimeout();
        if(statefulTimeout != null) {
            TimeUnit unit = TimeUnit.MINUTES;
            if(statefulTimeout.getUnit()!=null) {
                unit = statefulTimeout.getUnit();
            }
            sessionBeanComponentDescription.setStatefulTimeout(new StatefulTimeoutInfo(statefulTimeout.getTimeout(), unit));
        }
    }

    private void processSingletonBean(SessionBean31MetaData singletonBeanMetaData, SingletonComponentDescription singletonComponentDescription) {
        Boolean initOnStartup = singletonBeanMetaData.isInitOnStartup();
        if (initOnStartup != null && initOnStartup.booleanValue() == true) {
            singletonComponentDescription.initOnStartup();
        }
        // bean level lock-type
        LockType lockType = singletonBeanMetaData.getLockType();
        singletonComponentDescription.setBeanLevelLockType(singletonComponentDescription.getEJBClassName(), lockType);

        // concurrency management type
        ConcurrencyManagementType concurrencyManagementType = singletonBeanMetaData.getConcurrencyManagementType();
        if (concurrencyManagementType == ConcurrencyManagementType.BEAN) {
            singletonComponentDescription.beanManagedConcurrency();
        } else {
            singletonComponentDescription.containerManagedConcurrency();
        }

        // bean level access timeout
        // TODO: This should apply to other bean types too (JBoss specific feature) and not just singleton beans
        AccessTimeoutMetaData accessTimeoutMetaData = singletonBeanMetaData.getAccessTimeout();
        if (accessTimeoutMetaData != null) {
            final long timeout = accessTimeoutMetaData.getTimeout();
            final TimeUnit unit = accessTimeoutMetaData.getUnit();
            AccessTimeout accessTimeout = new AccessTimeout() {
                @Override
                public long value() {
                    return timeout;
                }

                @Override
                public TimeUnit unit() {
                    return unit;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return AccessTimeout.class;
                }
            };
            singletonComponentDescription.setBeanLevelAccessTimeout(singletonComponentDescription.getEJBClassName(), accessTimeout);
        }
    }

}
