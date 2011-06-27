/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.tomcat;

import org.jboss.metadata.common.ejb.IAssemblyDescriptorMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.javaee.spec.SecurityRolesMetaData;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.j2ee.EJBArchiveMetaData;

/**
 * Creates web app security meta data for EJB 21 deployment.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class SecurityMetaDataAccessorEJB21 extends AbstractSecurityMetaDataAccessorEJB {

    /**
     * Constructor.
     */
    SecurityMetaDataAccessorEJB21() {
        super();
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.AbstractSecurityMetaDataAccessorEJB#getSecurityDomain(Deployment)
     *
     * @param dep webservice deployment
     * @return security domain associated with EJB 21 deployment
     */
    public String getSecurityDomain(final Deployment dep) {
        final EJBArchiveMetaData ejbMetaData = WSHelper.getRequiredAttachment(dep, EJBArchiveMetaData.class);

        //return super.appendJaasPrefix(ejbMetaData.getSecurityDomain()); TODO: properly removed?
        return ejbMetaData.getSecurityDomain();
    }

    /**
     * @see org.jboss.webservices.integration.tomcat.AbstractSecurityMetaDataAccessorEJB#getSecurityRoles(Deployment)
     *
     * @param dep webservice deployment
     * @return security roles associated with EJB 21 deployment
     */
    public SecurityRolesMetaData getSecurityRoles(final Deployment dep) {
        final JBossMetaData jbossWebMD = WSHelper.getRequiredAttachment(dep, JBossMetaData.class);
        final IAssemblyDescriptorMetaData assemblyDescriptorMD = jbossWebMD.getAssemblyDescriptor();

        return (assemblyDescriptorMD != null) ? assemblyDescriptorMD.getSecurityRoles() : null;
    }
}
