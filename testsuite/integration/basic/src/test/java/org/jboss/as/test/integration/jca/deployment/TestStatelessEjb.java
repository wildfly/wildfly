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
import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;

/**
 * This is the impl for a stateless ejb.
 */

@ConnectionFactoryDefinition(interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
        name = "java:app/rardeployment/AppCF",
        resourceAdapter = "eis.rar")
@ConnectionFactoryDefinitions({
        @ConnectionFactoryDefinition(
                interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
                name = "java:comp/rardeployment/CompCF",
                resourceAdapter = "eis"),
        @ConnectionFactoryDefinition(
                interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
                name = "java:module/rardeployment/ModuleCF",
                resourceAdapter = "eis.rar"),
        @ConnectionFactoryDefinition(
                interfaceName = "org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1",
                name = "java:global/rardeployment/GlobalCF",
                resourceAdapter = "eis")
})
@Stateless
public class TestStatelessEjb implements ITestStatelessEjb {


    public boolean validateConnectorResource(String jndiName) {
        boolean rval = false;
        try {
            InitialContext ctx = new InitialContext();
            Object obj = ctx.lookup(jndiName);
            rval = obj != null;
        } catch (Exception e) {
            debug("Fail to access connector resource: " + jndiName);
            e.printStackTrace();
        }

        return rval;
    }


    private void debug(String str) {
        //System.out.println(str);
    }

}
