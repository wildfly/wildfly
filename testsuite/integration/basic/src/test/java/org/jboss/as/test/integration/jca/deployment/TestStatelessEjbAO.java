/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.deployment;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import jakarta.resource.AdministeredObjectDefinition;
import jakarta.resource.AdministeredObjectDefinitions;

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
