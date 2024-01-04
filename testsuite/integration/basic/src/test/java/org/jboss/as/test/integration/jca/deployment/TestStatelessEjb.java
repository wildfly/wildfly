/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import jakarta.resource.ConnectionFactoryDefinition;
import jakarta.resource.ConnectionFactoryDefinitions;

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
