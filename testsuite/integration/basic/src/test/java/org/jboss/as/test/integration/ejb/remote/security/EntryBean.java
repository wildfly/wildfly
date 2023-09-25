/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remote.security;

import org.jboss.as.test.shared.integration.ejb.security.Util;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

/**
 * An unsecured EJB used to test switching the identity before calling a secured EJB.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@Remote(IntermediateAccess.class)
public class EntryBean implements IntermediateAccess {

    @Resource
    private EJBContext context;

    @EJB
    private SecurityInformation ejb;

    @Override
    public String getPrincipalName() {
        return context.getCallerPrincipal().getName();
    }

    @Override
    public String getPrincipalName(String username, String password) {
        try {
            return Util.switchIdentity(username, password, () -> ejb.getPrincipalName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
