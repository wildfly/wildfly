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
package org.jboss.as.webservices.metadata;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * A minimum web service meta data representation that offers a generic
 * way to access more fine grained meta data through {@link #getAnnotation(Class)}
 * @author Heiko.Braun <heiko.braun@jboss.com>
 */
// TODO: get rid of this class
@Deprecated
public interface WebServiceDeclaration {
   /**
    * A distinct identifier across deployments.<br>
    * In case of EJB3 this would be the <code>ObjectName</code> under which get's registered with the MC.
    * @return
    */
   String getContainerName();

   /**
    * An identifier within a deployment.
    * In case of EJB3 this would be the <code>ejb-name</code>.
    * @return a name, that can be used to susequently address the service impl.
    */
   String getComponentName();

   /**
    * Web service endpoint implementation class
    * @return
    */
   String getComponentClassName();

   /**
    * Get a unified meta data view represented by an annotation.
    *
    * @param t
    * @return
    */
   AnnotationInstance getAnnotation(DotName dotName);
}
