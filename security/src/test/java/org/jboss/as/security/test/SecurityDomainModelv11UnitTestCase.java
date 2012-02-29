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
package org.jboss.as.security.test;

import java.io.IOException;

import org.jboss.as.security.SecurityExtension;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

public class SecurityDomainModelv11UnitTestCase extends AbstractSubsystemBaseTest {

    public SecurityDomainModelv11UnitTestCase() {
        super(SecurityExtension.SUBSYSTEM_NAME, new SecurityExtension());
    }

    @Test
    public void testParseAndMarshalModel() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("securitysubsystemv11.xml");

        KernelServices servicesA = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);
        //Get the model and the persisted xml from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        servicesA.shutdown();

        System.out.println(marshalled);

        //Install the persisted xml from the first controller into a second controller
        KernelServices servicesB = super.installInController(AdditionalInitialization.MANAGEMENT, marshalled);
        ModelNode modelB = servicesB.readWholeModel();

        //Make sure the models from the two controllers are identical
        super.compare(modelA, modelB);

        assertRemoveSubsystemResources(servicesA);
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("securitysubsystemv11.xml");
        /*return "<subsystem xmlns=\"urn:jboss:domain:security:1.1\">" +
                " <security-domains> " +
                  " <security-domain name=\"other\" cache-type=\"default\">" +
                  "  <authentication>" +
                   "     <login-module code=\"Remoting\" flag=\"optional\">" +
                    "        <module-option name=\"password-stacking\" value=\"useFirstPass\"/>" +
                     "   </login-module>" +
                      "  <login-module code=\"RealmUsersRoles\" flag=\"required\">" +
                       "     <module-option name=\"usersProperties\" value=\"${jboss.server.config.dir}/application-users.properties\"/>" +
                        "    <module-option name=\"rolesProperties\" value=\"${jboss.server.config.dir}/application-roles.properties\"/>" +
                         "   <module-option name=\"realm\" value=\"ApplicationRealm\"/>" +
                          "  <module-option name=\"password-stacking\" value=\"useFirstPass\"/> " +
                       " </login-module>" +
                    "</authentication>" +
                "</security-domain>" +
                "<security-domain name=\"jboss-web-policy\" cache-type=\"default\">" +
                 "   <authorization>" +
                  "      <policy-module code=\"Delegating\" flag=\"required\"/>" +
                   " </authorization>" +
                "</security-domain>" +
                "<security-domain name=\"jboss-ejb-policy\" cache-type=\"default\">" +
                 "   <authorization>" +
                  "      <policy-module code=\"Delegating\" flag=\"required\"/>" +
                   " </authorization>" +
                "</security-domain>" +
                "<security-domain name=\"DsRealm\" cache-type=\"default\">" +
                     "<authentication>" +
                           "<login-module code=\"ConfiguredIdentity\" flag=\"required\">" +
                               "<module-option name=\"userName\" value=\"sa\"/>" +
                               "<module-option name=\"principal\" value=\"sa\"/>" +
                               "<module-option name=\"password\" value=\"sa\"/>" +
                            "</login-module>" +
                     "</authentication>" +
                "</security-domain>" +
            "</security-domains>" +
        "</subsystem>";*/
    }
}