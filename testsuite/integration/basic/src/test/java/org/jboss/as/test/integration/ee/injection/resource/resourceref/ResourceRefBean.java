/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import jakarta.ejb.Stateless;
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
