/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.jwt.ejb;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;

/**
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
public class TargetBean {


    @RolesAllowed({ "Subscriber" })
    public boolean successfulCall() {

        return true;
    }

}
