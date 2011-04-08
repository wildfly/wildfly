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
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ee.component.InterceptorMethodDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.AroundInvokeMetaData;
import org.jboss.metadata.ejb.spec.AroundInvokesMetaData;
import org.jboss.metadata.ejb.spec.EjbJarMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.EnterpriseBeansMetaData;
import org.jboss.metadata.ejb.spec.InterceptorMetaData;
import org.jboss.metadata.ejb.spec.InterceptorsMetaData;
import org.jboss.metadata.ejb.spec.MethodInterfaceType;
import org.jboss.metadata.ejb.spec.MethodParametersMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbackMetaData;
import org.jboss.metadata.javaee.spec.LifecycleCallbacksMetaData;

import javax.interceptor.InvocationContext;

/**
 * User: jpai
 */
public abstract class AbstractEjbXmlDescriptorProcessor<T extends EnterpriseBeanMetaData> implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(AbstractEjbXmlDescriptorProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // get the deployment unit
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // find the EJB jar metadata and start processing it
        EjbJarMetaData ejbJarMetaData = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
        if (ejbJarMetaData == null) {
            return;
        }

        EnterpriseBeansMetaData ejbs = ejbJarMetaData.getEnterpriseBeans();
        if (ejbs == null || ejbs.isEmpty()) {
            return;
        }
        for (EnterpriseBeanMetaData ejb : ejbs) {
            if (this.getMetaDataType().isInstance(ejb)) {
                this.processBeanMetaData((T) ejb, phaseContext);
            }
        }
    }

    protected abstract Class<T> getMetaDataType();

    protected abstract void processBeanMetaData(T beanMetaData, DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException;

    protected MethodIntf getMethodIntf(MethodInterfaceType viewType) {
        if (viewType == null) {
            return MethodIntf.BEAN;
        }
        switch (viewType) {
            case Home:
                return MethodIntf.HOME;
            case LocalHome:
                return MethodIntf.LOCAL_HOME;
            case ServiceEndpoint:
                return MethodIntf.SERVICE_ENDPOINT;
            case Local:
                return MethodIntf.LOCAL;
            case Remote:
                return MethodIntf.REMOTE;
            // TODO: Need to handle more recent ones (like timer, mdb)
        }
        return MethodIntf.BEAN;
    }

    protected String[] getMethodParams(MethodParametersMetaData methodParametersMetaData) {
        if (methodParametersMetaData == null) {
            return null;
        }
        return methodParametersMetaData.toArray(new String[0]);
    }

    protected void processInterceptors(EnterpriseBeanMetaData enterpriseBean, EJBComponentDescription ejbComponentDescription) {

        EjbJarMetaData ejbJarMetaData = enterpriseBean.getEjbJarMetaData();
        String ejbName = enterpriseBean.getEjbName();
        InterceptorsMetaData applicableInterceptors = EjbJarMetaData.getInterceptors(ejbName, ejbJarMetaData);
        if (applicableInterceptors != null) {
            for (InterceptorMetaData interceptor : applicableInterceptors) {
                // get (or create the interceptor description)
                InterceptorDescription interceptorDescription = ejbComponentDescription.getClassInterceptor(interceptor.getInterceptorClass());
                if (interceptorDescription == null) {
                    interceptorDescription = new InterceptorDescription(interceptor.getInterceptorClass());
                    ejbComponentDescription.addClassInterceptor(interceptorDescription);
                }

                // around-invoke(s) of the interceptor configured (if any) in the deployment descriptor
                AroundInvokesMetaData aroundInvokes = interceptor.getAroundInvokes();
                if (aroundInvokes != null) {
                    for (AroundInvokeMetaData aroundInvoke : aroundInvokes) {
                        String methodName = aroundInvoke.getMethodName();
                        MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Object.class, methodName, new Class<?>[]{InvocationContext.class});
                        // TODO: This constructor for InterceptorMethodDescription needs a review. How does one get hold of the "declaraingClass"
                        // for a DD based interceptor configuration. Why not just specify the instance class and then "find" the correct method
                        // internally
                        InterceptorMethodDescription aroundInvokeMethodDescription = new InterceptorMethodDescription(interceptor.getInterceptorClass(), interceptor.getInterceptorClass(), methodIdentifier, false);
                        // add the around-invoke to the interceptor description
                        interceptorDescription.addAroundInvokeMethod(aroundInvokeMethodDescription);
                    }
                }

                // post-construct(s) of the interceptor configured (if any) in the deployment descriptor
                LifecycleCallbacksMetaData postConstructs = interceptor.getPostConstructs();
                if (postConstructs != null) {
                    for (LifecycleCallbackMetaData postConstruct : postConstructs) {
                        String methodName = postConstruct.getMethodName();
                        MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Void.TYPE, methodName, new Class<?>[]{InvocationContext.class});
                        // TODO: This constructor for InterceptorMethodDescription needs a review. How does one get hold of the "declaraingClass"
                        // for a DD based interceptor configuration. Why not just specify the instance class and then "find" the correct method
                        // internally
                        InterceptorMethodDescription postConstructInterceptor = new InterceptorMethodDescription(interceptor.getInterceptorClass(), interceptor.getInterceptorClass(), methodIdentifier, false);
                        // add it to the interceptor description
                        interceptorDescription.addPostConstruct(postConstructInterceptor);
                    }
                }

                // pre-destroy(s) of the interceptor configured (if any) in the deployment descriptor
                LifecycleCallbacksMetaData preDestroys = interceptor.getPreDestroys();
                if (preDestroys != null) {
                    for (LifecycleCallbackMetaData preDestroy : preDestroys) {
                        String methodName = preDestroy.getMethodName();
                        MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(Void.TYPE, methodName, new Class<?>[]{InvocationContext.class});
                        // TODO: This constructor for InterceptorMethodDescription needs a review. How does one get hold of the "declaraingClass"
                        // for a DD based interceptor configuration. Why not just specify the instance class and then "find" the correct method
                        // internally
                        InterceptorMethodDescription preDestroyInterceptor = new InterceptorMethodDescription(interceptor.getInterceptorClass(), interceptor.getInterceptorClass(), methodIdentifier, false);
                        // add it to the interceptor description
                        interceptorDescription.addPreDestroy(preDestroyInterceptor);
                    }
                }
            }
        }

    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

}
