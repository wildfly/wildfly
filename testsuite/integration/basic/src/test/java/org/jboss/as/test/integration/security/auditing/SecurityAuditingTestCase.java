/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.auditing;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.security.Constants;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;
import org.jboss.security.auth.spi.UsersRolesLoginModule;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertTrue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.FLAG;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

/**
 * This class tests Security auditing functionality
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@ServerSetup(SecurityAuditingTestCase.SecurityAuditingTestCaseSetup.class)
public class SecurityAuditingTestCase {

    static class SecurityAuditingTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        protected void doSetup(final ManagementClient managementClient) throws Exception {
            final List<ModelNode> updates = new ArrayList<ModelNode>();

            ModelNode op;

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("periodic-rotating-file-handler", "AUDIT");
            op.get("level").set("TRACE");
            op.get("append").set("true");
            op.get("suffix").set(".yyyy-MM-dd");
            ModelNode file = new ModelNode();
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set("audit.log");
            op.get("file").set(file);
            op.get("formatter").set("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n");
            updates.add(op);

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "logging");
            op.get(OP_ADDR).add("logger", "org.jboss.security.audit.providers.LogAuditProvider");
            op.get("level").set("TRACE");
            ModelNode list = op.get("handlers");
            list.add("AUDIT");
            updates.add(op);

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, "form-auth");
            updates.add(op);

            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "security");
            op.get(OP_ADDR).add(SECURITY_DOMAIN, "form-auth");
            op.get(OP_ADDR).add(Constants.AUTHENTICATION, Constants.CLASSIC);

            ModelNode loginModule = op.get(Constants.LOGIN_MODULES).add();
            loginModule.get(ModelDescriptionConstants.CODE).set(UsersRolesLoginModule.class.getName());
            loginModule.get(FLAG).set("required");
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);

            ModelNode moduleOptions = loginModule.get("module-options");
            moduleOptions.get("usersProperties").set("users.properties");
            moduleOptions.get("rolesProperties").set("roles.properties");

            updates.add(op);
            executeOperations(updates);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

        }
    }


    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "form-auth.war");
        war.setWebXML("org/jboss/as/test/integration/security/auditing/form-auth/web.xml");
        war.addAsWebInfResource("org/jboss/as/test/integration/security/auditing/form-auth/jboss-web.xml");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/index.jsp");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/login.jsp");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/loginerror.jsp");
        war.addAsWebResource("org/jboss/as/test/integration/security/auditing/form-auth/logout.jsp");
        war.addAsResource("org/jboss/as/test/integration/security/auditing/form-auth/users.properties", "/users.properties");
        war.addAsResource("org/jboss/as/test/integration/security/auditing/form-auth/roles.properties", "/roles.properties");
        return war;
    }

    @Ignore("AS7-3346")
    @RunAsClient
    @Test
    public void test(@ArquillianResource URL url) throws Exception {

        File auditLog = new File(System.getProperty("jbossas.ts.submodule.dir"), "target/jbossas/standalone/log/audit.log");


        assertTrue("Audit log file has not been created (" + auditLog.getAbsolutePath() + ")", auditLog.exists());
        assertTrue("Audit log file is closed for reading (" + auditLog.getAbsolutePath() + ")", auditLog.canRead());

        BufferedReader reader = new BufferedReader(new FileReader(auditLog));

        String line;
        while (null != (line = reader.readLine())) {
            // we need to get trough all old records (if any)
        }


        Utils.makeCall(url.toString(), "anil", "anil", 200);

        Pattern successPattern = Pattern.compile("TRACE.+org.jboss.security.audit.providers.LogAuditProvider.+Success");

        // we'll be actively waiting for a given INTERVAL for the record to appear
        final long INTERVAL = 5000;
        long startTime = System.currentTimeMillis();
        while (true) {
            boolean recordFound = false;
            //some new line were added -> go trough those and check whether our record is present
            while (null != (line = reader.readLine())) {
                if (successPattern.matcher(line).matches()) {
                    recordFound = true;
                }
            }
            if (recordFound) {
                break; // we are done
            }
            //record not written to log yet -> continue checking if the time has not yet expired
            if (System.currentTimeMillis() > startTime + INTERVAL) {
                //time expired
                throw new AssertionError("Login record has not been written into audit log! (time expired)");
            }
        }

    }


}
