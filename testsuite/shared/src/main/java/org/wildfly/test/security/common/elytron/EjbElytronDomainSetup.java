/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.security.common.elytron;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.test.security.common.elytron.Utils.applyRemoveAllowReload;
import static org.wildfly.test.security.common.elytron.Utils.applyUpdate;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.elytron.ElytronExtension;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class EjbElytronDomainSetup implements ServerSetupTask {

    private static final String DEFAULT_SECURITY_DOMAIN_NAME = "elytron-tests";

    private PathAddress saslAuthenticationAddress;

    private PathAddress remotingConnectorAddress;

    private PathAddress ejbDomainAddress;

    private final String securityDomainName;

    public EjbElytronDomainSetup() {
        this(DEFAULT_SECURITY_DOMAIN_NAME);
    }

    public EjbElytronDomainSetup(final String securityDomainName) {
        this.securityDomainName = securityDomainName;
    }

    protected String getSecurityDomainName() {
        return securityDomainName;
    }

    protected String getSecurityRealmName() {
        return getSecurityDomainName() + "-ejb3-UsersRoles";
    }


    protected String getEjbDomainName() {
        return getSecurityDomainName();
    }

    protected String getSaslAuthenticationName() {
        return getSecurityDomainName();
    }

    protected String getRemotingConnectorName() {
        return "http-remoting-connector";
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        saslAuthenticationAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("sasl-authentication-factory", getSaslAuthenticationName());

        remotingConnectorAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, "remoting")
                .append("http-connector", getRemotingConnectorName());

        ejbDomainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, "ejb3")
                .append("application-security-domain", getEjbDomainName());

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(ModelDescriptionConstants.COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        // /subsystem=elytron/sasl-authentication-factory=ejb3-tests-auth-fac:add(sasl-server-factory=configured,security-domain=EjbDomain,mechanism-configurations=[{mechanism-name=BASIC}])
        ModelNode addSaslAuthentication = Util.createAddOperation(saslAuthenticationAddress);
        addSaslAuthentication.get("sasl-server-factory").set("configured");
        addSaslAuthentication.get("security-domain").set(getSecurityDomainName());
        addSaslAuthentication.get("mechanism-configurations").get(0).get("mechanism-name").set("JBOSS-LOCAL-USER");
        addSaslAuthentication.get("mechanism-configurations").get(0).get("realm-mapper").set("local");
        addSaslAuthentication.get("mechanism-configurations").get(1).get("mechanism-name").set("DIGEST-MD5");
        addSaslAuthentication.get("mechanism-configurations").get(1).get("mechanism-realm-configurations").get(0).get("realm-name").set(getSecurityRealmName());
        steps.add(addSaslAuthentication);

        // remoting connection with sasl-authentication-factory
        ModelNode updateRemotingConnector = Util.getWriteAttributeOperation(remotingConnectorAddress, "sasl-authentication-factory", getSaslAuthenticationName());
        steps.add(updateRemotingConnector);

        // /subsystem=ejb3/application-security-domain=ejb3-tests:add(security-domain=ApplicationDomain)
        ModelNode addEjbDomain = Util.createAddOperation(ejbDomainAddress);
        addEjbDomain.get("security-domain").set(getSecurityDomainName());
        steps.add(addEjbDomain);

        applyUpdate(managementClient.getControllerClient(), compositeOp, false);
        // TODO: add {"allow-resource-service-restart" => true} to ejbDomainAddress write-attribute operation once WFLY-8793 / JBEAP-10955 is fixed
        //       and remove this reload
        ServerReload.reloadIfRequired(managementClient.getControllerClient());
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) {
        try {
            applyUpdate(managementClient.getControllerClient(), Util.getWriteAttributeOperation(remotingConnectorAddress, "sasl-authentication-factory", "application-sasl-authentication"), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        applyRemoveAllowReload(managementClient.getControllerClient(), ejbDomainAddress, false);
        // TODO: add {"allow-resource-service-restart" => true} to ejbDomainAddress write-attribute operation once WFLY-8793 / JBEAP-10955 is fixed
        //       and remove this reload
        try {
            ServerReload.reloadIfRequired(managementClient.getControllerClient());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        applyRemoveAllowReload(managementClient.getControllerClient(), saslAuthenticationAddress, false);
    }

}
