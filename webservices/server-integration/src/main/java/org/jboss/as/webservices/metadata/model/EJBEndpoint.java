/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.metadata.model;

import java.util.List;

import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class EJBEndpoint implements WSEndpoint {

   public static final String EJB_COMPONENT_VIEW_NAME = EJBEndpoint.class.getPackage().getName() + "EjbComponentViewName";
   private final SessionBeanComponentDescription ejbMD;
   private final ClassInfo webServiceClassInfo;
   private final String containerName;

   public EJBEndpoint(final SessionBeanComponentDescription ejbMD, final ClassInfo webServiceClassInfo, final String containerName) {
       this.ejbMD = ejbMD;
       this.webServiceClassInfo = webServiceClassInfo;
       this.containerName = containerName;
   }

   public String getComponentViewName() {
       return containerName;
   }

   public String getName() {
       return ejbMD.getComponentName();
   }

   public ServiceName getContextServiceName() {
       return ejbMD.getContextServiceName();
   }

   public DeploymentDescriptorEnvironment getDeploymentDescriptorEnvironment() {
       return ejbMD.getDeploymentDescriptorEnvironment();
   }

   public String getClassName() {
       return ejbMD.getComponentClassName();
   }

   public AnnotationInstance getAnnotation(final DotName annotationType) {// DotName
       List<AnnotationInstance> list = webServiceClassInfo.annotations().get(annotationType);
       if (list != null) {
           return list.get(0);
       }
       return null;
   }

}
