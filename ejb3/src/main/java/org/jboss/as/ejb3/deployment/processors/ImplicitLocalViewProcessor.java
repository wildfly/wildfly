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

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Processes a {@link SessionBeanComponentDescription}'s bean class and checks whether it exposes an implicit no-interface
 * view, as specified by EJB3.1 spec, section 4.9.8. If an implicit no-interface view is identified then the
 * {@link SessionBeanComponentDescription} is updated with this info accordingly.
 * <p/>
 * This processor MUST run <b>before</b> the {@link EjbJndiBindingsDeploymentUnitProcessor EJB jndi binding} processor is run.
 *
 * @author Jaikiran Pai
 */
public class ImplicitLocalViewProcessor extends AbstractComponentConfigProcessor {

    private static Logger logger = Logger.getLogger(ImplicitLocalViewProcessor.class);

    @Override
    protected void processComponentConfig(DeploymentUnit deploymentUnit, DeploymentPhaseContext phaseContext, CompositeIndex index, AbstractComponentDescription componentDescription) throws DeploymentUnitProcessingException {

        if (!(componentDescription instanceof SessionBeanComponentDescription)) {
            return;
        }
        SessionBeanComponentDescription sessionBeanComponentDescription = (SessionBeanComponentDescription) componentDescription;
        // if it already has a no-interface  view, then no need to check for the implicit rules
        if (sessionBeanComponentDescription.hasNoInterfaceView()) {
            return;
        }
        // if the bean already exposes some view(s) then it isn't eligible for an implicit no-interface view.
        if (!sessionBeanComponentDescription.getViewClassNames().isEmpty()) {
            return;
        }
        String ejbClassName = sessionBeanComponentDescription.getComponentClassName();

        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (module == null) {
            throw new IllegalStateException("Module hasn't yet been attached to deployment unit: " + deploymentUnit);
        }
        ClassLoader cl = module.getClassLoader();
        try {
            Class<?> ejbClass = cl.loadClass(ejbClassName);
            // check whether it's eligible for implicit no-interface view
            if (this.exposesNoInterfaceView(ejbClass)) {
                logger.debug("Bean: " + sessionBeanComponentDescription.getEJBName() + " will be marked for (implicit) no-interface view");
                sessionBeanComponentDescription.addNoInterfaceView();
            }

        } catch (ClassNotFoundException cnfe) {
            throw new DeploymentUnitProcessingException(cnfe);
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
        Iterator<Class<?>> implementedInterfacesIterator = implementedInterfaces.iterator();
        while (implementedInterfacesIterator.hasNext()) {
            Class<?> implementedInterface = implementedInterfacesIterator.next();
            if (implementedInterface.equals(java.io.Serializable.class)
                    || implementedInterface.equals(java.io.Externalizable.class)
                    || implementedInterface.getName().startsWith("javax.ejb.")) {
                implementedInterfacesIterator.remove();
            }
        }
        // Now that we have removed the interfaces that should be excluded from the check,
        // if the implementedInterfaces collection is empty then this bean can be considered for no-interface view
        return implementedInterfaces.isEmpty();
    }
}
