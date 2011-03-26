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

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.AccessTimeoutMetaData;
import org.jboss.metadata.ejb.spec.BusinessLocalsMetaData;
import org.jboss.metadata.ejb.spec.BusinessRemotesMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodMetaData;
import org.jboss.metadata.ejb.spec.ConcurrentMethodsMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionMetaData;
import org.jboss.metadata.ejb.spec.ContainerTransactionsMetaData;
import org.jboss.metadata.ejb.spec.MethodMetaData;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.ejb.spec.MethodsMetaData;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.ejb.spec.SessionType;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagementType;
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
        // get the module description
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = moduleDescription.getAppName();

        SessionType sessionType = sessionBean.getSessionType();
        if (sessionType == null) {
            throw new DeploymentUnitProcessingException("Unknown session-type for session bean: " + sessionBean.getName() + " in deployment unit: " + deploymentUnit);
        }
        String beanName = sessionBean.getName();
        String beanClassName = sessionBean.getEjbClass();
        SessionBeanComponentDescription sessionBeanDescription = null;
        switch (sessionType) {
            case Stateless:
                sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
                break;
            case Stateful:
                sessionBeanDescription = new StatefulComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
                break;
            case Singleton:
                sessionBeanDescription = new SingletonComponentDescription(beanName, beanClassName, moduleDescription.getModuleName(), applicationName);
                break;
            default:
                throw new IllegalArgumentException("Unknown session bean type: " + sessionType);
        }
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
            logger.debug("Remote business interface processing isn't yet implemented");
            // TODO: Process remote business interfaces
        }
        // tx management type
        if (sessionBean.getTransactionType() != null) {
            sessionBeanDescription.setTransactionManagementType(sessionBean.getTransactionType());
        }
        // CMT Tx attributes
        if (sessionBean.getTransactionType() != TransactionManagementType.BEAN) {
            ContainerTransactionsMetaData containerTransactions = sessionBean.getContainerTransactions();
            if (containerTransactions != null && !containerTransactions.isEmpty()) {
                for (ContainerTransactionMetaData containerTx : containerTransactions) {
                    TransactionAttributeType txAttr = containerTx.getTransAttribute();
                    MethodsMetaData methods = containerTx.getMethods();
                    for (MethodMetaData method : methods) {
                        String methodName = method.getMethodName();
                        MethodIntf methodIntf = this.getMethodIntf(method.getMethodIntf());
                        if (methodName.equals("*")) {
                            sessionBeanDescription.setTransactionAttribute(methodIntf, txAttr);
                        } else {

                            MethodParametersMetaData methodParams = method.getMethodParams();
                            // update the session bean description with the tx attribute info
                            sessionBeanDescription.setTransactionAttribute(methodIntf, txAttr, methodName, this.getMethodParams(methodParams));
                        }
                    }
                }
            }
        }

        // process EJB3.1 specific session bean description
        if (sessionBean instanceof SessionBean31MetaData) {
            this.processSessionBean31((SessionBean31MetaData) sessionBean, sessionBeanDescription);
        }

        // Add this component description to the module description
        moduleDescription.addComponent(sessionBeanDescription);

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
    }

    private void processSingletonBean(SessionBean31MetaData singletonBeanMetaData, SingletonComponentDescription singletonComponentDescription) {
        if (singletonBeanMetaData.isInitOnStartup()) {
            singletonComponentDescription.initOnStartup();
        }
        // bean level lock-type
        LockType lockType = singletonBeanMetaData.getLockType();
        singletonComponentDescription.setBeanLevelLockType(lockType);
        // TODO: Add method level lock type to the description
        ConcurrentMethodsMetaData concurrentMethods = singletonBeanMetaData.getConcurrentMethods();
        for (ConcurrentMethodMetaData concurrentMethod : concurrentMethods) {
            LockType methodLockType = concurrentMethod.getLockType();
            concurrentMethod.getMethod();
        }

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
            singletonComponentDescription.setBeanLevelAccessTimeout(accessTimeout);
        }
    }

}
