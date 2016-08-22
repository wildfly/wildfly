/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.subsystem.EJB3SubsystemModel;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@ServerSetup(EjbMethodNameRegexTestCase.RegexServerSetup.class)
public class EjbMethodNameRegexTestCase {

    public static class RegexServerSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode node = new ModelNode();
            node.get(ADDRESS).set(PathAddress.parseCLIStyleAddress("/subsystem=ejb3").toModelNode());
            node.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            node.get(NAME).set(EJB3SubsystemModel.ALLOW_EJB_NAME_REGEX);
            node.get(VALUE).set(true);
            ModelNode result = managementClient.getControllerClient().execute(node);
            System.out.println(result);

        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode node = new ModelNode();
            node.get(ADDRESS).set(PathAddress.parseCLIStyleAddress("/subsystem=ejb3").toModelNode());
            node.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            node.get(NAME).set(EJB3SubsystemModel.ALLOW_EJB_NAME_REGEX);
            managementClient.getControllerClient().execute(node);
        }
    }

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testregex.war");
        war.addPackage(EjbMethodNameRegexTestCase.class.getPackage());
        war.addAsWebInfResource(EjbMethodNameRegexTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testInterceptors() throws NamingException {
        final InitialContext ctx = new InitialContext();
        TestEjb bean = (TestEjb) ctx.lookup("java:module/Test1");
        Assert.assertEquals("test-regex", bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Test2");
        Assert.assertEquals("test-regex", bean.test());
        bean = (TestEjb) ctx.lookup("java:module/Production");
        Assert.assertEquals("test", bean.test());
    }

}
