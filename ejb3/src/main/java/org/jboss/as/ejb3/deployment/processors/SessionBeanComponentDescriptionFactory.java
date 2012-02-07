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

import java.lang.reflect.Modifier;
import java.util.List;

import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import org.jboss.as.ee.metadata.MetadataCompleteMarker;
import org.jboss.as.ejb3.EjbMessages;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.ejb3.component.singleton.SingletonComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.metadata.ejb.spec.EjbType;
import org.jboss.metadata.ejb.spec.EnterpriseBeanMetaData;
import org.jboss.metadata.ejb.spec.GenericBeanMetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;
import org.jboss.metadata.ejb.spec.SessionType;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ejb3.deployment.processors.AbstractDeploymentUnitProcessor.getEjbJarDescription;

/**
 * User: jpai
 */
public class SessionBeanComponentDescriptionFactory extends EJBComponentDescriptionFactory {

    private static final Logger logger = Logger.getLogger(SessionBeanComponentDescriptionFactory.class);

    private static final DotName STATELESS_ANNOTATION = DotName.createSimple(Stateless.class.getName());
    private static final DotName STATEFUL_ANNOTATION = DotName.createSimple(Stateful.class.getName());
    private static final DotName SINGLETON_ANNOTATION = DotName.createSimple(Singleton.class.getName());

    public SessionBeanComponentDescriptionFactory(final boolean appclient) {
        super(appclient);
    }

    /**
     * Process annotations and merge any available metadata at the same time.
     */
    @Override
    protected void processAnnotations(final DeploymentUnit deploymentUnit, final CompositeIndex compositeIndex) throws DeploymentUnitProcessingException {

        if (MetadataCompleteMarker.isMetadataComplete(deploymentUnit)) {
            return;
        }

        // Find and process any @Stateless bean annotations
        final List<AnnotationInstance> slsbAnnotations = compositeIndex.getAnnotations(STATELESS_ANNOTATION);
        if (!slsbAnnotations.isEmpty()) {
            processSessionBeans(deploymentUnit, slsbAnnotations, SessionBeanComponentDescription.SessionBeanType.STATELESS);
        }

        // Find and process any @Stateful bean annotations
        final List<AnnotationInstance> sfsbAnnotations = compositeIndex.getAnnotations(STATEFUL_ANNOTATION);
        if (!sfsbAnnotations.isEmpty()) {
            processSessionBeans(deploymentUnit, sfsbAnnotations, SessionBeanComponentDescription.SessionBeanType.STATEFUL);
        }

        // Find and process any @Singleton bean annotations
        final List<AnnotationInstance> sbAnnotations = compositeIndex.getAnnotations(SINGLETON_ANNOTATION);
        if (!sbAnnotations.isEmpty()) {
            processSessionBeans(deploymentUnit, sbAnnotations, SessionBeanComponentDescription.SessionBeanType.SINGLETON);
        }
    }

    @Override
    protected void processBeanMetaData(final DeploymentUnit deploymentUnit, final EnterpriseBeanMetaData enterpriseBeanMetaData) throws DeploymentUnitProcessingException {
        if (enterpriseBeanMetaData.isSession()) {
            assert enterpriseBeanMetaData instanceof SessionBeanMetaData : enterpriseBeanMetaData + " is not a SessionBeanMetaData";
            processSessionBeanMetaData(deploymentUnit, (SessionBeanMetaData) enterpriseBeanMetaData);
        }
    }

    private void processSessionBeans(final DeploymentUnit deploymentUnit, final List<AnnotationInstance> sessionBeanAnnotations, final SessionBeanComponentDescription.SessionBeanType annotatedSessionBeanType) {

        final EjbJarDescription ejbJarDescription = getEjbJarDescription(deploymentUnit);
        final ServiceName deploymentUnitServiceName = deploymentUnit.getServiceName();

        // process these session bean annotations and create component descriptions out of it
        for (final AnnotationInstance sessionBeanAnnotation : sessionBeanAnnotations) {
            final AnnotationTarget target = sessionBeanAnnotation.target();
            if (!(target instanceof ClassInfo)) {
                // Let's just WARN and move on. No need to throw an error
                logger.warn(sessionBeanAnnotation.name() + " annotation is expected to be applied on class level. " + target + " is not a class");
                continue;
            }
            final ClassInfo sessionBeanClassInfo = (ClassInfo) target;
            // skip if it's not a valid class for session bean
            if (!assertSessionBeanClassValidity(sessionBeanClassInfo)) {
                continue;
            }
            final String ejbName = sessionBeanClassInfo.name().local();
            final AnnotationValue nameValue = sessionBeanAnnotation.value("name");
            final String beanName = nameValue == null || nameValue.asString().isEmpty() ? ejbName : nameValue.asString();
            final SessionBeanMetaData beanMetaData = getEnterpriseBeanMetaData(deploymentUnit, beanName, SessionBeanMetaData.class);
            final SessionBeanComponentDescription.SessionBeanType sessionBeanType;
            final String beanClassName;
            if (beanMetaData != null) {
                beanClassName = override(sessionBeanClassInfo.name().toString(), beanMetaData.getEjbClass());
                sessionBeanType = override(annotatedSessionBeanType, descriptionOf(((SessionBeanMetaData) beanMetaData).getSessionType()));
            } else {
                beanClassName = sessionBeanClassInfo.name().toString();
                sessionBeanType = annotatedSessionBeanType;
            }

            final SessionBeanComponentDescription sessionBeanDescription;
            switch (sessionBeanType) {
                case STATELESS:
                    sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName, beanMetaData);
                    break;
                case STATEFUL:
                    sessionBeanDescription = new StatefulComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName, beanMetaData);
                    break;
                case SINGLETON:
                    sessionBeanDescription = new SingletonComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnitServiceName, beanMetaData);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown session bean type: " + sessionBeanType);
            }

            addComponent(deploymentUnit, sessionBeanDescription);
        }

        EjbDeploymentMarker.mark(deploymentUnit);
    }

    private static SessionBeanComponentDescription.SessionBeanType descriptionOf(final SessionType sessionType) {
        if (sessionType == null)
            return null;
        switch (sessionType) {
            case Stateless:
                return SessionBeanComponentDescription.SessionBeanType.STATELESS;
            case Stateful:
                return SessionBeanComponentDescription.SessionBeanType.STATEFUL;
            case Singleton:
                return SessionBeanComponentDescription.SessionBeanType.SINGLETON;
            default:
                throw new IllegalArgumentException("Unknown session bean type: " + sessionType);
        }
    }

    /**
     * Returns true if the passed <code>sessionBeanClass</code> meets the requirements set by the EJB3 spec about
     * bean implementation classes. The passed <code>sessionBeanClass</code> must not be an interface and must be public
     * and not final and not abstract. If it passes these requirements then this method returns true. Else it returns false.
     *
     * @param sessionBeanClass The session bean class
     * @return
     */
    private static boolean assertSessionBeanClassValidity(final ClassInfo sessionBeanClass) {
        final short flags = sessionBeanClass.flags();
        final String className = sessionBeanClass.name().toString();
        // must *not* be a interface
        if (Modifier.isInterface(flags)) {
            logger.warn("[EJB3.1 spec, section 4.9.2] Session bean implementation class MUST NOT be a interface - "
                    + className + " is an interface, hence won't be considered as a session bean");
            return false;
        }
        // bean class must be public, must *not* be abstract or final
        if (!Modifier.isPublic(flags) || Modifier.isAbstract(flags) || Modifier.isFinal(flags)) {
            logger.warn("[EJB3.1 spec, section 4.9.2] Session bean implementation class MUST be public, not abstract and not final - "
                    + className + " won't be considered as a session bean, since it doesn't meet that requirement");
            return false;
        }
        // valid class
        return true;
    }

    private void processSessionBeanMetaData(final DeploymentUnit deploymentUnit, final SessionBeanMetaData sessionBean) throws DeploymentUnitProcessingException {
        final EjbJarDescription ejbJarDescription = getEjbJarDescription(deploymentUnit);
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);

        final String beanName = sessionBean.getName();
        SessionType sessionType = sessionBean.getSessionType();

        if (sessionType == null && sessionBean instanceof GenericBeanMetaData) {
            final GenericBeanMetaData bean = (GenericBeanMetaData) sessionBean;
            if (bean.getEjbType() == EjbType.SESSION) {
                sessionType = determineSessionType(sessionBean.getEjbClass(), compositeIndex);
                if (sessionType == null) {
                    throw EjbMessages.MESSAGES.sessionTypeNotSpecified(beanName);
                }
            } else {
                //it is not a session bean, so we ignore it
                return;
            }
        } else if (sessionType == null) {
            sessionType = determineSessionType(sessionBean.getEjbClass(), compositeIndex);
            if (sessionType == null) {
                throw EjbMessages.MESSAGES.sessionTypeNotSpecified(beanName);
            }
        }

        final String beanClassName = sessionBean.getEjbClass();
        final SessionBeanComponentDescription sessionBeanDescription;
        switch (sessionType) {
            case Stateless:
                sessionBeanDescription = new StatelessComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnit.getServiceName(), sessionBean);
                break;
            case Stateful:
                sessionBeanDescription = new StatefulComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnit.getServiceName(), sessionBean);
                break;
            case Singleton:
                sessionBeanDescription = new SingletonComponentDescription(beanName, beanClassName, ejbJarDescription, deploymentUnit.getServiceName(), sessionBean);
                break;
            default:
                throw new IllegalArgumentException("Unknown session bean type: " + sessionType);
        }
        addComponent(deploymentUnit, sessionBeanDescription);
    }

    private SessionType determineSessionType(final String ejbClass, final CompositeIndex compositeIndex) {
        if(ejbClass == null) {
            return null;
        }
        final ClassInfo info = compositeIndex.getClassByName(DotName.createSimple(ejbClass));
        if(info.annotations().get(STATEFUL_ANNOTATION) != null) {
            return SessionType.Stateful;
        } else if(info.annotations().get(STATELESS_ANNOTATION) != null) {
            return SessionType.Stateless;
        } else if(info.annotations().get(SINGLETON_ANNOTATION) != null) {
            return SessionType.Singleton;
        }
        return null;
    }

}
