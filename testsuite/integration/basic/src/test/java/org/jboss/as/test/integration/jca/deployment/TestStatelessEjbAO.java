/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.deployment;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;

/**
 * This is the impl for a stateless ejb.
 */

@AdministeredObjectDefinition(className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
                              name = "java:app/rardeployment/AppAdmin",
                              resourceAdapter = "#inside-eis.rar")
@AdministeredObjectDefinitions({
    @AdministeredObjectDefinition(
        className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
        name = "java:comp/rardeployment/CompAdmin",
        resourceAdapter = "#inside-eis"),
    @AdministeredObjectDefinition(
        className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
        name = "java:module/rardeployment/ModuleAdmin",
        resourceAdapter = "#inside-eis.rar"),
    @AdministeredObjectDefinition(
        className = "org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl",
        name = "java:global/rardeployment/GlobalAdmin",
        resourceAdapter = "#inside-eis")
})
@Stateless
public class TestStatelessEjbAO implements ITestStatelessEjbAO {


    public boolean validateConnectorResource(String jndiName) {
        boolean rval = false;
        try {
            InitialContext ctx = new InitialContext();
            Object obj = ctx.lookup(jndiName);
            if (obj == null) {
                rval = false;
            } else {
                rval = true;
            }
        } catch (Exception e) {
            debug("Fail to access connector resource: "+jndiName);
            e.printStackTrace();
        }

        return rval;
    }


    private void debug(String str) {
        System.out.println(str);
    }

}
