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

import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import javax.ejb.Local;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import java.io.Externalizable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Processes {@link Local @Local} and {@link @Remote} annotation of a session bean and sets up the {@link SessionBeanComponentDescription}
 * out of it.
 * <p/>
 *
 * @author Jaikiran Pai
 */
public class BusinessViewAnnotationProcessor extends AbstractAnnotationEJBProcessor<SessionBeanComponentDescription> {

    private static final DotName LOCAL = DotName.createSimple(Local.class.getName());
    private static final DotName LOCAL_BEAN = DotName.createSimple(LocalBean.class.getName());
    private static final DotName REMOTE = DotName.createSimple(Remote.class.getName());

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(BusinessViewAnnotationProcessor.class);

    @Override
    protected Class<SessionBeanComponentDescription> getComponentDescriptionType() {
        return SessionBeanComponentDescription.class;
    }

    /**
     * Processes the session bean for remote and local views and updates the {@link SessionBeanComponentDescription}
     * accordingly
     *
     * @param sessionBeanClass     The bean class
     * @param compositeIndex       The composite annotation index
     * @param sessionBeanComponentDescription The component description
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    protected void processAnnotations(ClassInfo sessionBeanClass, CompositeIndex compositeIndex, SessionBeanComponentDescription sessionBeanComponentDescription) throws DeploymentUnitProcessingException {
        final Collection<String> remoteBusinessInterfaces = getBusinessInterfaces(sessionBeanClass, compositeIndex, REMOTE);
        sessionBeanComponentDescription.addRemoteBusinessInterfaceViews(remoteBusinessInterfaces);

        // fetch the local business interfaces of the bean
        Collection<String> localBusinessInterfaces = getBusinessInterfaces(sessionBeanClass, compositeIndex, LOCAL);
        if (logger.isTraceEnabled()) {
            logger.trace("Session bean: " + sessionBeanComponentDescription.getEJBName() + " has " + localBusinessInterfaces.size() + " local business interfaces namely: " + localBusinessInterfaces);
        }
        // add it to the component description
        sessionBeanComponentDescription.addLocalBusinessInterfaceViews(localBusinessInterfaces);

        if (hasNoInterfaceView(sessionBeanClass))
            sessionBeanComponentDescription.addNoInterfaceView();

        // EJB 3.1 FR 4.9.7 & 4.9.8, if the bean exposes no views
        if (hasNoViews(sessionBeanComponentDescription)) {
            final Set<DotName> names = getPotentialBusinessInterfaces(sessionBeanClass);
            if (names.isEmpty())
                sessionBeanComponentDescription.addNoInterfaceView();
            else if (names.size() == 1)
                sessionBeanComponentDescription.addLocalBusinessInterfaceViews(names.iterator().next().toString());
        }
    }

    private static Collection<String> getBusinessInterfaces(ClassInfo sessionBeanClass, CompositeIndex compositeIndex, DotName annotationType) throws DeploymentUnitProcessingException {
        Map<DotName, List<AnnotationInstance>> annotationsOnBean = sessionBeanClass.annotations();
        List<AnnotationInstance> annotations = annotationsOnBean.get(annotationType);
        if (annotations == null || annotations.isEmpty()) {

            Collection<String> interfaces = getBusinessInterfacesFromInterfaceAnnotations(sessionBeanClass, compositeIndex, annotationType);
            if(!interfaces.isEmpty()) {
                return interfaces;
            }
            return Collections.emptySet();
        }
        if (annotations.size() > 1) {
            throw new DeploymentUnitProcessingException("@" + annotationType + " appears more than once in EJB class: " + sessionBeanClass.name());
        }

        final AnnotationInstance annotation = annotations.get(0);
        final AnnotationTarget target = annotation.target();
        if (target instanceof ClassInfo == false) {
            throw new RuntimeException("@" + annotationType + " should only appear on a class. Target: " + target + " is not a class");
        }
        AnnotationValue annotationValue = annotation.value();
        if (annotationValue == null) {
            Set<DotName> interfaces = getPotentialBusinessInterfaces(sessionBeanClass);
            if (interfaces.size() != 1)
                throw new DeploymentUnitProcessingException("Bean " + sessionBeanClass + " specifies @" + annotationType + ", but does not implement 1 interface");
            return Collections.singleton(interfaces.iterator().next().toString());
        }
        final Collection<String> businessInterfaces = new HashSet<String>();
        final Type[] interfaceTypes = annotationValue.asClassArray();
        for (final Type type : interfaceTypes) {
            businessInterfaces.add(type.name().toString());
        }
        return businessInterfaces;
    }

    private static Collection<String> getBusinessInterfacesFromInterfaceAnnotations(ClassInfo sessionBeanClass, CompositeIndex compositeIndex, DotName annotationType) {
        Set<DotName> interfaces = getPotentialBusinessInterfaces(sessionBeanClass);
        Set<String> localInterfaces = new HashSet<String>();
        for(DotName iface : interfaces) {
            final ClassInfo ifaceClass = compositeIndex.getClassByName(iface);
            final List<AnnotationInstance> annotations = ifaceClass.annotations().get(annotationType);
            if(annotations != null) {
                for(AnnotationInstance annotation : annotations) {
                    if(annotation.target() instanceof ClassInfo) {
                        localInterfaces.add(iface.toString());
                        break;
                    }
                }
            }
        }
        return localInterfaces;
    }

    /**
     * Gets a beans implicit local interface
     * @param sessionBeanClass The bean class
     * @return The implicit business interface, or null if one is not found
     */
    private static String getDefaultLocalInterface(ClassInfo sessionBeanClass,CompositeIndex index) {
        final Set<DotName> names = getPotentialBusinessInterfaces(sessionBeanClass);
        if(names.size() != 1) {
            return null;
        }
        //now we have an interface, but it is not an implicit local interface
        //if it is annotated @Remote
        final DotName iface = names.iterator().next();
        final ClassInfo classInfo = index.getClassByName(iface);
        List<AnnotationInstance> annotations = classInfo.annotations().get(REMOTE);
        if(annotations == null || annotations.isEmpty()) {
            return iface.toString();
        }
        for(AnnotationInstance annotation : annotations) {
            if(annotation.target() instanceof ClassInfo) {
                return null;
            }
        }

        return iface.toString();
    }

    /**
     * Returns all interfaces implemented by a bean that are eligible to be business interfaces
     *
     * @param sessionBeanClass The bean class
     * @return A collection of all potential business interfaces
     */
    private static Set<DotName> getPotentialBusinessInterfaces(ClassInfo sessionBeanClass) {
        DotName[] interfaces = sessionBeanClass.interfaces();
        if (interfaces == null) {
            return Collections.emptySet();
        }
        final Set<DotName> names = new HashSet<DotName>();
        for(DotName dotName : interfaces) {
            String name = dotName.toString();
            // EJB 3.1 FR 4.9.7 bullet 5.3
            if(name.equals(Serializable.class.getName()) ||
                    name.equals(Externalizable.class.getName()) ||
                    name.startsWith("javax.ejb.")) {
                continue;
            }
            names.add(dotName);
        }
        return names;
    }

    /**
     * Returns true if the <code>sessionBeanClass</code> has a {@link LocalBean no-interface view annotation}.
     * Else returns false.
     *
     * @param sessionBeanClass The session bean {@link ClassInfo class}
     * @return
     */
    private static boolean hasNoInterfaceView(ClassInfo sessionBeanClass) {
        Map<DotName, List<AnnotationInstance>> annotationsOnBeanClass = sessionBeanClass.annotations();
        if (annotationsOnBeanClass == null || annotationsOnBeanClass.isEmpty()) {
            return false;
        }
        List<AnnotationInstance> localBeanAnnotations = annotationsOnBeanClass.get(LOCAL_BEAN);
        return localBeanAnnotations != null && !localBeanAnnotations.isEmpty();
    }

    private static boolean hasNoViews(SessionBeanComponentDescription sessionBeanComponentDescription) {
        Set<String> viewClassNames = sessionBeanComponentDescription.getViewClassNames();
        return viewClassNames == null || viewClassNames.isEmpty();
    }
}
