/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

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
