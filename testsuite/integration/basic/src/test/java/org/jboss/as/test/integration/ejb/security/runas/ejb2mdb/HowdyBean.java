/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Returns howdy greeting for INTERNAL_ROLE.
 *
 * @author Ondrej Chaloupka
 */
@Stateless(name = "Howdy")
@RolesAllowed("INTERNAL_ROLE")
@Local(Howdy.class)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HowdyBean implements Howdy {
    public static final String SAYING = "Howdy";

    @Resource
    private SessionContext context;

    @EJB
    Hola hola;

    public String sayHowdy() {
        return String.format("%s %s, %s", SAYING, getName(), hola.sayHola());
    }

    private String getName() {
        return context.getCallerPrincipal().getName();
    }
}
