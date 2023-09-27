/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mapbased;

import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJBContext;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author Jan Martiska / jmartisk@redhat.com
 */
@Stateful
@Remote(StatefulIface.class)
@SecurityDomain("other")
@PermitAll
public class StatefulBean implements StatefulIface {

    @Resource
    private EJBContext ejbContext;


    @Override
    public String getCallerPrincipalName() {
        return ejbContext.getCallerPrincipal().getName();
    }
}
