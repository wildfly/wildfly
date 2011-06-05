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

package org.jboss.as.ejb3.component.description;


import org.jboss.as.ee.component.ViewDescription;
import org.jboss.as.ejb3.component.EJBViewDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.msc.service.ServiceName;

import javax.ejb.AccessTimeout;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LockType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: jpai
 */
public class EjbJarDescriptionMergingUtil {

    public static void merge(EjbJarDescription mergedResult, EjbJarDescription original, EjbJarDescription override, EjbJarDescription ejbModuleDescription) {
        Collection<SessionBeanComponentDescription> originalSessionBeans = original.getSessionBeans();
        if (originalSessionBeans.isEmpty()) {
            // no session beans in the original ejb jar description. So just use all the session beans from the override
            mergedResult.addSessionBeans(override.getSessionBeans());
        } else {
            Collection<SessionBeanComponentDescription> overrideSessionBeans = override.getSessionBeans();
            if (overrideSessionBeans.isEmpty()) {
                // no session beans in override so use all the session beans from the original
                mergedResult.addSessionBeans(originalSessionBeans);
            } else {
                // both original and override session beans are non-empty. Merge them
                List<SessionBeanComponentDescription> mergedBeans = new ArrayList<SessionBeanComponentDescription>();
                mergeSessionBeans(mergedBeans, originalSessionBeans, overrideSessionBeans, ejbModuleDescription);
                mergedResult.addSessionBeans(mergedBeans);
            }
        }
    }

    public static void mergeSessionBeans(Collection<SessionBeanComponentDescription> mergedResult, Collection<SessionBeanComponentDescription> original, Collection<SessionBeanComponentDescription> override, EjbJarDescription ejbModuleDescription) {

        if (original.isEmpty()) {
            // no session beans in the original ejb jar description. So just use all the session beans from the override
            mergedResult.addAll(override);
        } else {
            if (override.isEmpty()) {
                // no session beans in override so use all the session beans from the original
                mergedResult.addAll(original);
            } else {
                // both original and override session beans are non-empty. Merge them
                Set<String> commonBeans = new HashSet<String>();
                for (SessionBeanComponentDescription originalSessionBean : original) {
                    // find the session bean in the overriden collection
                    SessionBeanComponentDescription overrideSessionBean = findSessionBean(originalSessionBean.getEJBName(), override);
                    if (overrideSessionBean == null) {
                        // if there's no overridden session bean, then just add the original to the merged collection
                        mergedResult.add(originalSessionBean);
                    } else {
                        // make sure we are merging beans of the same type
                        SessionBeanComponentDescription.SessionBeanType sessionBeanType = originalSessionBean.getSessionBeanType();
                        if (overrideSessionBean.getSessionBeanType() != sessionBeanType) {
                            throw new RuntimeException("Cannot mergeSessionBean two EJBs with the same name = " + originalSessionBean.getEJBName() +
                                    " but with different session bean types, type 1 - " + overrideSessionBean.getSessionBeanType()
                                    + " type 2 - " + originalSessionBean.getSessionBeanType());
                        }
                        // there's a overridden session bean, so merge them
                        commonBeans.add(originalSessionBean.getEJBName());
                        SessionBeanComponentDescription mergedBean = createNewSessionBean(originalSessionBean, ejbModuleDescription);
                        switch (sessionBeanType) {
                            case STATELESS:
                                mergeStatelessBean((StatelessComponentDescription) mergedBean, (StatelessComponentDescription) originalSessionBean, (StatelessComponentDescription) overrideSessionBean);
                                break;
                            case STATEFUL:
                                mergeStatefulBean((StatefulComponentDescription) mergedBean, (StatefulComponentDescription) originalSessionBean, (StatefulComponentDescription) overrideSessionBean);
                                break;
                            case SINGLETON:
                                mergeSingletonBean((SingletonComponentDescription) mergedBean, (SingletonComponentDescription) originalSessionBean, (SingletonComponentDescription) overrideSessionBean);
                                break;

                            default:
                                throw new RuntimeException("Unknown session bean type: " + sessionBeanType + " for bean: " + originalSessionBean.getEJBName());

                        }
                        mergedResult.add(mergedBean);
                    }
                }
                // now add those session beans which are only in the override
                for (SessionBeanComponentDescription overrideSessionBean : override) {
                    if (commonBeans.contains(overrideSessionBean.getEJBName())) {
                        // skip since it's already been merged and added to the merged collection of beans
                        continue;
                    }
                    // exclusively belongs to override collection, so directly add it to the merged collection
                    mergedResult.add(overrideSessionBean);
                }
            }
        }
    }


    public static void mergeSingletonBean(SingletonComponentDescription mergedSingleton, SingletonComponentDescription original, SingletonComponentDescription override) {
        // merge the common session bean info
        mergeSessionBean(mergedSingleton, original, override);

        // now merge singleton bean specific info
        if (override.isInitOnStartup()) {
            mergedSingleton.initOnStartup();
        }

    }

    public static void mergeStatefulBean(StatefulComponentDescription mergedStatefulBean, StatefulComponentDescription original, StatefulComponentDescription override) {
        // merge the common session bean info
        mergeSessionBean(mergedStatefulBean, original, override);

        // now merge stateful bean specific info
        // tx type
        if (override.getStatefulTimeout() != null) {
            mergedStatefulBean.setStatefulTimeout(override.getStatefulTimeout());
        } else {
            if (original.getStatefulTimeout() != null) {
                mergedStatefulBean.setStatefulTimeout(original.getStatefulTimeout());
            }
        }

    }

    public static void mergeStatelessBean(StatelessComponentDescription mergedStatelessBean, StatelessComponentDescription original, StatelessComponentDescription override) {
        // merge the common session bean info
        mergeSessionBean(mergedStatelessBean, original, override);

        // now merge stateless bean specific info

    }

    private static void mergeSessionBean(SessionBeanComponentDescription mergedBean, SessionBeanComponentDescription original, SessionBeanComponentDescription override) {

        // mapped-name
        if (override.getMappedName() != null) {
            mergedBean.setMappedName(override.getMappedName());
        } else {
            mergedBean.setMappedName(original.getMappedName());
        }

        // tx type
        if (override.getTransactionManagementType() != null) {
            mergedBean.setTransactionManagementType(override.getTransactionManagementType());
        } else {
            if (original.getTransactionManagementType() != null) {
                mergedBean.setTransactionManagementType(original.getTransactionManagementType());
            }
        }

        // concurrency management type
        ConcurrencyManagementType overrideConcurrencyMgmtType = override.getConcurrencyManagementType();
        if (overrideConcurrencyMgmtType != null) {
            if (overrideConcurrencyMgmtType == ConcurrencyManagementType.BEAN) {
                mergedBean.beanManagedConcurrency();
            } else {
                mergedBean.containerManagedConcurrency();
            }
        } else {
            ConcurrencyManagementType originalConcurrencyManagemenType = original.getConcurrencyManagementType();
            if (originalConcurrencyManagemenType != null) {
                if (originalConcurrencyManagemenType == ConcurrencyManagementType.BEAN) {
                    mergedBean.beanManagedConcurrency();
                } else {
                    mergedBean.containerManagedConcurrency();
                }
            }
        }

        // bean level lock type
        LockType overrideBeanLockType = override.getBeanLevelLockType().get(mergedBean.getEJBClassName());
        if (overrideBeanLockType != null) {
            mergedBean.setBeanLevelLockType(mergedBean.getEJBClassName(), overrideBeanLockType);
        } else {
            LockType originalBeanLockType = original.getBeanLevelLockType().get(mergedBean.getEJBClassName());
            if (originalBeanLockType != null) {
                mergedBean.setBeanLevelLockType(mergedBean.getEJBClassName(), originalBeanLockType);
            }
        }

        // access timeout
        AccessTimeout overrideAccessTimeout = override.getBeanLevelAccessTimeout().get(mergedBean.getEJBClassName());
        if (overrideAccessTimeout != null) {
            mergedBean.setBeanLevelAccessTimeout(mergedBean.getEJBClassName(), overrideAccessTimeout);
        } else {
            AccessTimeout originalAccessTimeout = original.getBeanLevelAccessTimeout().get(mergedBean.getEJBClassName());
            if (originalAccessTimeout != null) {
                mergedBean.setBeanLevelAccessTimeout(mergedBean.getEJBClassName(), originalAccessTimeout);
            }
        }

        // views
        Collection<ViewDescription> overrideViews = override.getViews();
        if (overrideViews != null && !overrideViews.isEmpty()) {
            for (ViewDescription view : overrideViews) {
                String viewClassName = view.getViewClassName();
                MethodIntf viewType = ((EJBViewDescription) view).getMethodIntf();
                addView(mergedBean, viewClassName, viewType);
            }
        } else {
            Collection<ViewDescription> originalViews = original.getViews();
            if (originalViews != null) {
                for (ViewDescription view : originalViews) {
                    String viewClassName = view.getViewClassName();
                    MethodIntf viewType = ((EJBViewDescription) view).getMethodIntf();
                    addView(mergedBean, viewClassName, viewType);
                }
            }
        }
    }


    private static SessionBeanComponentDescription findSessionBean(String ejbName, Collection<SessionBeanComponentDescription> sessionBeans) {
        if (sessionBeans == null || sessionBeans.isEmpty()) {
            return null;
        }
        for (SessionBeanComponentDescription sessionBean : sessionBeans) {
            if (sessionBean.getEJBName().equals(ejbName)) {
                return sessionBean;
            }
        }
        return null;
    }


    private static SessionBeanComponentDescription createNewSessionBean(SessionBeanComponentDescription source, EjbJarDescription ejbModuleDescription) {
        SessionBeanComponentDescription.SessionBeanType sessionBeanType = source.getSessionBeanType();
        ServiceName deploymentUnitServiceName = source.getServiceName().getParent();
        switch (sessionBeanType) {
            case STATELESS:
                return new StatelessComponentDescription(source.getComponentName(), source.getComponentClassName(), ejbModuleDescription, deploymentUnitServiceName);
            case STATEFUL:
                return new StatefulComponentDescription(source.getComponentName(), source.getComponentClassName(), ejbModuleDescription, deploymentUnitServiceName);
            case SINGLETON:
                return new SingletonComponentDescription(source.getComponentName(), source.getComponentClassName(), ejbModuleDescription, deploymentUnitServiceName);
            default:
                throw new IllegalArgumentException("Unknown session bean type: " + sessionBeanType + " for bean " + source.getEJBName());
        }
    }

    private static void addView(SessionBeanComponentDescription sessionBean, String viewClassName, MethodIntf viewType) {
        switch (viewType) {
            case LOCAL:
                if (sessionBean.getEJBClassName().equals(viewClassName)) {
                    sessionBean.addNoInterfaceView();
                } else {

                    sessionBean.addLocalBusinessInterfaceViews(Collections.singleton(viewClassName));
                }
                return;
            case REMOTE:
                sessionBean.addRemoteBusinessInterfaceViews(Collections.singleton(viewClassName));
                return;
            // TODO: Handle other types
        }
    }
}
