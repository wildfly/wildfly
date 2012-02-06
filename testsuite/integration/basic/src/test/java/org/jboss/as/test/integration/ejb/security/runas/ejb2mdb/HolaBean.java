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
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
// Security related imports
import javax.annotation.security.RolesAllowed;

import org.jboss.logging.Logger;

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
    private static final Logger log = Logger.getLogger(HowdyBean.class);
    
    @Resource
    private SessionContext context;

    @RolesAllowed("INTERNAL_ROLE")
    public String sayHola() {
        log.info("HolaBean.sayHola(). Caller name: " + context.getCallerPrincipal().getName());

        if (context.isCallerInRole("JBossAdmin")) {
            log.info("User is in role!!");
        }

        String name = getName();
        return "Hola " + name + "!";
    }

    private String getName() {
        return "Fred";
    }

}
