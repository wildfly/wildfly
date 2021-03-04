/*
 * Copyright 2021 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.ws.wsse.usernametoken;

import java.net.URL;
import java.util.ArrayList;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.rt.security.SecurityConstants;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.ws.wsse.ElytronUsernameTokenImpl;
import org.jboss.as.test.integration.ws.wsse.ServiceIface;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.UserWithAttributeValues;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * <p>Test case for SubjectCreatingPolicyInterceptor integrated with elytron
 * security. The username and password are requested by a UsernameToken
 * policy.</p>
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({UsernameTokenElytronTestCase.ElytronDomainSetup.class, UsernameTokenElytronTestCase.EjbElytronDomainSetup.class})
public class UsernameTokenElytronTestCase {

    @ArquillianResource
    private URL serviceURL;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, UsernameTokenElytronTestCase.class.getSimpleName() + ".war");
        archive.setManifest(new StringAsset("Manifest-Version: 1.0\n"
                        + "Dependencies: org.apache.cxf\n"))
                .addClasses(ServiceIface.class, ElytronUsernameTokenImpl.class)
                .addAsWebInfResource(UsernameTokenElytronTestCase.class.getPackage(), "jboss-ejb3-elytron-properties.xml", "jboss-ejb3.xml")
                .addAsWebInfResource(ServiceIface.class.getPackage(), "wsdl/UsernameToken.wsdl", "wsdl/UsernameToken.wsdl")
                .addAsWebInfResource(ServiceIface.class.getPackage(), "wsdl/UsernameToken_schema1.xsd", "wsdl/UsernameToken_schema1.xsd")
                .addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(new ElytronPermission("getSecurityDomain")), "permissions.xml");
        return archive;
    }

    @Test
    public void testBadPassword() throws Exception {
        final QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "UsernameToken");
        final URL wsdlURL = new URL(serviceURL + "UsernameToken/ElytronUsernameTokenImpl?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);

        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.USERNAME, "user1");
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.PASSWORD, "passwordWrong");

        // JBWS024057: Failed Authentication : Subject has not been created
        SOAPFaultException e = Assert.assertThrows(SOAPFaultException.class, () -> proxy.sayHello());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("JBWS024057"));
    }

    @Test
    public void testNoAllowedRole() throws Exception {
        final QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "UsernameToken");
        final URL wsdlURL = new URL(serviceURL + "UsernameToken/ElytronUsernameTokenImpl?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);

        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.USERNAME, "user2");
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.PASSWORD, "password2");

        // WFLYEJB0364: Invocation on method: xxx.sayHello() of bean: ElytronUsernameTokenImpl is not allowed
        SOAPFaultException e = Assert.assertThrows(SOAPFaultException.class, () -> proxy.sayHello());
        MatcherAssert.assertThat(e.getMessage(), CoreMatchers.containsString("WFLYEJB0364"));
    }

    @Test
    public void testOk() throws Exception {
        final QName serviceName = new QName("http://www.jboss.org/jbossws/ws-extensions/wssecuritypolicy", "UsernameToken");
        final URL wsdlURL = new URL(serviceURL + "UsernameToken/ElytronUsernameTokenImpl?wsdl");
        Service service = Service.create(wsdlURL, serviceName);
        ServiceIface proxy = (ServiceIface) service.getPort(ServiceIface.class);

        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.USERNAME, "user1");
        ((BindingProvider) proxy).getRequestContext().put(SecurityConstants.PASSWORD, "password1");

        Assert.assertEquals("Hello user1 with roles and with attributes!", proxy.sayHello());
    }

    /**
     * Elements needed for the elytron configuration: a properties realm with
     * a user (user1/password1), the security domain with that realm and
     * the undertow association.
     */
    public static class ElytronDomainSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            ArrayList<ConfigurableElement> configurableElements = new ArrayList<>();
            // creat a properties builder with users (user1 is allowed and user2 is not)
            configurableElements.add(PropertiesRealm.builder()
                    .withName("PropertiesRealm")
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user1")
                            .withPassword("password1")
                            .withValues("Users", "Role1")
                            .build())
                    .withUser(UserWithAttributeValues.builder()
                            .withName("user2")
                            .withPassword("password2")
                            .withValues("Role2")
                            .build())
                    .build());
            // create the domain with the properties realm
            configurableElements.add(SimpleSecurityDomain.builder()
                    .withName("PropertiesDomain")
                    .withDefaultRealm("PropertiesRealm")
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                            .withRealm("PropertiesRealm")
                            .withRoleDecoder("groups-to-roles")
                            .build())
                    .build());
            // undertow domain
            configurableElements.add(UndertowApplicationSecurityDomain.builder()
                    .withName("PropertiesDomain")
                    .withSecurityDomain("PropertiesDomain")
                    .build());
            return configurableElements.toArray(new ConfigurableElement[0]);
        }
    }

    /**
     * Server task to create the ejb3 domain association.
     * /subsystem=ejb3/application-security-domain=PropertiesDomain:add(security-domain=PropertiesDomain)
     */
    public static class EjbElytronDomainSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            PathAddress ejbDomainAddress = PathAddress.pathAddress()
                .append(ModelDescriptionConstants.SUBSYSTEM, "ejb3")
                .append("application-security-domain", "PropertiesDomain");
            ModelNode addEjbDomain = Util.createAddOperation(ejbDomainAddress);
            addEjbDomain.get("security-domain").set("PropertiesDomain");
            CoreUtils.applyUpdate(addEjbDomain, managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            PathAddress ejbDomainAddress = PathAddress.pathAddress()
                .append(ModelDescriptionConstants.SUBSYSTEM, "ejb3")
                .append("application-security-domain", "PropertiesDomain");
            ModelNode addEjbDomain = Util.createRemoveOperation(ejbDomainAddress);
            CoreUtils.applyUpdate(addEjbDomain, managementClient.getControllerClient());
            ServerReload.reloadIfRequired(managementClient);
        }

    }
}
