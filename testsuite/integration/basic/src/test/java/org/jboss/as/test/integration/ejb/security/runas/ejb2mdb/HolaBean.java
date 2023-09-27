/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Returns hola greeting for INTERNAL_ROLE.
 *
 * @author Ondrej Chaloupka
 */
@Stateless(name = "Hola")
@RolesAllowed({})
@Remote(Hola.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HolaBean implements Hola {
    public static final String SAYING = "Hola";

    @Resource
    private SessionContext context;

    @RolesAllowed("INTERNAL_ROLE")
    public String sayHola() {
        return String.format("%s %s", SAYING, getName());
    }

    private String getName() {
        return context.getCallerPrincipal().getName();
    }

}
