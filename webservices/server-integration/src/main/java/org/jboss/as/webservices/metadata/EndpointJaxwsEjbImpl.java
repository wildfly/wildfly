/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.metadata;

import java.util.List;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.msc.service.ServiceName;

/**
 * Adopts EJB3 bean meta data to a
 * {@link org.jboss.wsf.spi.deployment.EndpointJaxwsEjb.WebServiceDeclaration}.
 */
public final class EndpointJaxwsEjbImpl implements EndpointJaxwsEjb {

    /** EJB meta data. */
    private final SessionBeanComponentDescription ejbMD;
    private final ClassInfo webServiceClassInfo; // TODO: propagate just annotations?
    private final String containerName;

    /**
     * Constructor.
     *
     * @param ejbMD EJB metadata
     */
    public EndpointJaxwsEjbImpl(final SessionBeanComponentDescription ejbMD, final ClassInfo webServiceClassInfo, final String containerName) {
        this.ejbMD = ejbMD;
        this.webServiceClassInfo = webServiceClassInfo;
        this.containerName = containerName;
    }

    /**
     * Returns EJB container name.
     *
     * @return container name
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Returns EJB name.
     *
     * @return name
     */
    public String getName() {
        return ejbMD.getComponentName();
    }

    public ServiceName getContextServiceName() {
        return ejbMD.getContextServiceName();
    }

    public DeploymentDescriptorEnvironment getDeploymentDescriptorEnvironment() {
        return ejbMD.getDeploymentDescriptorEnvironment();
    }

    /**
     * Returns EJB class name.
     *
     * @return class name
     */
    public String getClassName() {
        return ejbMD.getComponentClassName();
    }

    /**
     * Returns requested annotation associated with EJB container or EJB bean.
     *
     * @param annotationType annotation type
     * @param <T> annotation class type
     * @return requested annotation or null if not found
     */
    public AnnotationInstance getAnnotation(final DotName annotationType) {// DotName
        List<AnnotationInstance> list = webServiceClassInfo.annotations().get(annotationType);
        if (list != null) {
            return list.get(0);
        }
        return null;
        //          throw new UnsupportedOperationException(); // TODO: implement
        //         final boolean haveEjbContainer = this.ejbContainer != null;
        //
        //         if (haveEjbContainer)
        //         {
        //            return this.ejbContainer.getAnnotation(annotationType);
        //         }
        //         else
        //         {
        //            final Class<?> bean = this.getComponentClass();
        //            return (T) bean.getAnnotation(annotationType);
        //         }
    }

   }