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

import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.deployers.AbstractComponentConfigProcessor;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Processes a {@link SessionBeanComponentDescription}'s bean class and checks whether it exposes:
 * <ul>
 * <li>An implicit no-interface, as specified by EJB3.1 spec, section 4.9.8.</li>
 * <li>A default local business interface view, as specified by EJB3.1 spec, section 4.9.7.</li>
 * </ul>
 * The {@link SessionBeanComponentDescription} is updated with this info accordingly.
 * <p/>
 * This processor MUST run <b>before</b> the {@link EjbJndiBindingsDeploymentUnitProcessor EJB jndi binding} processor is run.
 *
 * @author Jaikiran Pai
 */
public class ImplicitLocalViewProcessor extends AbstractComponentConfigProcessor {

    @Override
    protected void processComponentConfig(DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext, CompositeIndex index, ComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        if (!(componentDescription instanceof SessionBeanComponentDescription)) {
            return;
        }
        SessionBeanComponentDescription sessionBeanComponentDescription = (SessionBeanComponentDescription) componentDescription;
        // if it already has a no-interface  view, then no need to check for the implicit rules
        if (sessionBeanComponentDescription.hasNoInterfaceView()) {
            return;
        }
        // if the bean already exposes some view(s) then it isn't eligible for an implicit no-interface view.
        if (!sessionBeanComponentDescription.getViews().isEmpty()) {
            return;
        }
        String ejbClassName = sessionBeanComponentDescription.getComponentClassName();

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            throw EjbLogger.ROOT_LOGGER.moduleNotAttachedToDeploymentUnit(deploymentUnit);
        }
        ClassLoader cl = module.getClassLoader();
        Class<?> ejbClass = null;
        try {
            ejbClass = cl.loadClass(ejbClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new DeploymentUnitProcessingException(cnfe);
        }
        // check whether it's eligible for implicit no-interface view
        if (this.exposesNoInterfaceView(ejbClass)) {
            EjbLogger.DEPLOYMENT_LOGGER.debugf("Bean: %s will be marked for (implicit) no-interface view", sessionBeanComponentDescription.getEJBName());
            sessionBeanComponentDescription.addNoInterfaceView();
            return;
        }

        // check for default local view
        Class<?> defaultLocalView = this.getDefaultLocalView(ejbClass);
        if (defaultLocalView != null) {
            EjbLogger.DEPLOYMENT_LOGGER.debugf("Bean: %s will be marked for default local view: %s", sessionBeanComponentDescription.getEJBName(), defaultLocalView.getName());
            sessionBeanComponentDescription.addLocalBusinessInterfaceViews(Collections.singleton(defaultLocalView.getName()));
            return;
        }


    }

    /**
     * Returns true if the passed <code>beanClass</code> is eligible for implicit no-interface view. Else returns false.
     * <p/>
     * EJB3.1 spec, section 4.9.8 states the rules for an implicit no-interface view on a bean class.
     * If the "implements" clause of the bean class is empty then the bean is considered to be exposing a no-interface view.
     * During this implements clause check, the {@link java.io.Serializable} or {@link java.io.Externalizable} or
     * any class from javax.ejb.* packages are excluded.
     *
     * @param beanClass The bean class.
     * @return
     */
    private boolean exposesNoInterfaceView(Class<?> beanClass) {
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length == 0) {
            return true;
        }

        // As per section 4.9.8 (bullet 1.3) of EJB3.1 spec
        // java.io.Serializable; java.io.Externalizable; any of the interfaces defined by the javax.ejb
        // are excluded from interface check

        List<Class<?>> implementedInterfaces = new ArrayList<Class<?>>(Arrays.asList(interfaces));
        List<Class<?>> filteredInterfaces = this.filterInterfaces(implementedInterfaces);
        // Now that we have removed the interfaces that should be excluded from the check,
        // if the filtered interfaces collection is empty then this bean can be considered for no-interface view
        return filteredInterfaces.isEmpty();
    }

    /**
     * Returns the default local view class of the {@link Class beanClass}, if one is present.
     * EJB3.1 spec, section 4.9.7 specifies the rules for a default local view of a bean. If the bean implements
     * just one interface, then that interface is returned as the default local view by this method.
     * If no such, interface is found, then this method returns null.
     *
     * @param beanClass The bean class
     * @return
     */
    private Class<?> getDefaultLocalView(Class<?> beanClass) {
        Class<?>[] interfaces = beanClass.getInterfaces();
        if (interfaces.length == 0) {
            return null;
        }
        List<Class<?>> implementedInterfaces = new ArrayList<Class<?>>(Arrays.asList(interfaces));
        List<Class<?>> filteredInterfaces = this.filterInterfaces(implementedInterfaces);
        if (filteredInterfaces.isEmpty() || filteredInterfaces.size() > 1) {
            return null;
        }
        return filteredInterfaces.get(0);

    }

    /**
     * Returns a filtered list for the passed <code>interfaces</code> list, excluding the
     * {@link java.io.Serializable}, {@link java.io.Externalizable} and any interfaces belonging to <code>javax.ejb</code>
     * package.
     *
     * @param interfaces The list of interfaces
     * @return
     */
    private List<Class<?>> filterInterfaces(List<Class<?>> interfaces) {
        if (interfaces == null) {
            return null;
        }
        List<Class<?>> filteredInterfaces = new ArrayList<Class<?>>();
        for (Class<?> intf : interfaces) {
            if (intf.equals(java.io.Serializable.class)
                    || intf.equals(java.io.Externalizable.class)
                    || intf.getName().startsWith("javax.ejb.")) {
                continue;
            }
            filteredInterfaces.add(intf);
        }
        return filteredInterfaces;
    }
}
