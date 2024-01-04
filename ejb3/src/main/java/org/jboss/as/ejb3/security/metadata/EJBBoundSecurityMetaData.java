/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.security.metadata;

import org.jboss.metadata.ejb.parser.jboss.ejb3.AbstractEJBBoundMetaData;

/**
 * Metadata for security related information of EJB components
 *
 * @author <a href="mailto:mmoyses@redhat.com">Marcus Moyses</a>
 */
public class EJBBoundSecurityMetaData extends AbstractEJBBoundMetaData {

    private static final long serialVersionUID = 1350796966752920231L;

    private String securityDomain;

    private String runAsPrincipal;

    private Boolean missingMethodPermissionsDenyAccess;

    public String getSecurityDomain() {
        return securityDomain;
    }

    public void setSecurityDomain(String securityDomain) {
        this.securityDomain = securityDomain;
    }

    public String getRunAsPrincipal() {
        return runAsPrincipal;
    }

    public void setRunAsPrincipal(String runAsPrincipal) {
        this.runAsPrincipal = runAsPrincipal;
    }

    public Boolean getMissingMethodPermissionsDenyAccess() {
        return this.missingMethodPermissionsDenyAccess;
    }

    public void setMissingMethodPermissionsDenyAccess(Boolean missingMethodPermissionsDenyAccess) {
        this.missingMethodPermissionsDenyAccess = missingMethodPermissionsDenyAccess;
    }
}
