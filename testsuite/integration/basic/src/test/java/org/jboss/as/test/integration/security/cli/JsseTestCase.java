/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.cli;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JsseTestCase extends AbstractCliTestBase {


    @BeforeClass
    public static void initSecurityDomains() throws Exception {
        AbstractCliTestBase.initCLI();
        cli.sendLine("/subsystem=security/security-domain=empty-jsse:remove()", true);
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-valid:remove()", true);
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-missing-pwd:remove()", true);
        cli.sendLine("/subsystem=security/security-domain=empty-jsse:add()");
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-valid:add()");
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-missing-pwd:add()");
    }

    @AfterClass
    public static void cleanSecurityDomains() throws Exception {
        cli.sendLine("/subsystem=security/security-domain=empty-jsse:remove(){allow-resource-service-restart=true}", true);
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-valid:remove(){allow-resource-service-restart=true}", true);
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-missing-pwd:remove(){allow-resource-service-restart=true}", true);
        AbstractCliTestBase.closeCLI();
    }


    @Test
    public void addMissingPassword() throws IOException {
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-missing-pwd/jsse=classic:add(server-alias=silent.planet,keystore={type=JKS})", true);
        CLIOpResult result = cli.readAllAsOpResult();
        assertThat(result, is(notNullValue()));
        assertThat(result.getFromResponse(OUTCOME).toString(), is("failed"));
    }

    @Test
    public void addValid() throws Exception {
        cli.sendLine("/subsystem=security/security-domain=empty-jsse-valid/jsse=classic:add(server-alias=silent.planet)");
        CLIOpResult result = cli.readAllAsOpResult();
        assertThat(result, is(notNullValue()));
        assertThat(result.getFromResponse(OUTCOME).toString(), is("success"));
    }

    @Test
    public void addEmpty() throws Exception {
        cli.sendLine("/subsystem=security/security-domain=empty-jsse/jsse=classic:add()");
        CLIOpResult result = cli.readAllAsOpResult();
        assertThat(result, is(notNullValue()));
        assertThat(result.getFromResponse(OUTCOME).toString(), is("success"));
    }
}
