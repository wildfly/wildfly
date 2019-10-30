/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

/**
 * ResourceRefBean
 * <p>
 * This bean will be used to test the EJBTHREE-1823 issue.
 * <p>
 * Brief description of the issue: If a resource-ref entry is available in jboss.xml, but there is no corresponding resource-ref
 * entry neither in ejb-jar.xml nor a @Resource in the bean, then because of the non-availability of the "res-type" information,
 * a NullPointerException gets thrown when the {@link ResourceHandler} tries to process the entries to be made in ENC.
 *
 * @author Jaikiran Pai
 */
@Stateless
public class ResourceRefBean implements ResourceRefRemote {

    private static Logger logger = Logger.getLogger(ResourceRefBean.class);

    /**
     * Looks up a datasource within the ENC of this bean. The datasource is expected to be configured through the deployment
     * descriptors and should be available at java:comp/env/EJBTHREE-1823_DS
     * <p>
     * The "res-type" of this datasource resource-ref will not be provided through ejb-jar.xml nor through a @Resource
     * annotation.
     */
    public boolean isDataSourceAvailableInEnc() throws NamingException {
        boolean ret = false;
        Context ctx = new InitialContext();
        String encJndiName = "java:comp/env/EJBTHREE-1823_DS";
        DataSource ds = (DataSource) ctx.lookup(encJndiName);
        ret = ds != null;
        logger.trace("Datasource was found: " + ret + ", on: " + encJndiName);
        return ret;
    }
}
