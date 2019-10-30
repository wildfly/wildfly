/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.ejb;

import java.util.Map;

import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.EJBComponent;
import org.jboss.as.ejb3.deployment.EjbDeploymentInformation;
import org.jboss.as.ejb3.iiop.EjbIIOPService;
import org.jboss.msc.value.InjectedValue;

/**
 * Gets around having no EJBComponent in the injected value
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ManagementEjbDeploymentInformation extends EjbDeploymentInformation {

    public ManagementEjbDeploymentInformation(String ejbName, Map<String, InjectedValue<ComponentView>> componentViews, ClassLoader deploymentClassLoader) {
        super(ejbName, new InjectedValue<EJBComponent>(), componentViews, null, deploymentClassLoader, new InjectedValue<EjbIIOPService>());
    }

    @Override
    public EJBComponent getEjbComponent() {
        return null;
    }
}
