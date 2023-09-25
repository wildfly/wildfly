/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class EjbElytronDomainSetup implements ServerSetupTask {

    private static final String SUBSYSTEM_NAME = "elytron";
    private static final String DEFAULT_SECURITY_DOMAIN_NAME = "elytron-tests";

    private PathAddress saslAuthenticationAddress;

    private PathAddress remotingConnectorAddress;

    private PathAddress ejbDomainAddress;

    private final String securityDomainName;

    private String saslAuthenticationFactoryValue = null;

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
                .append(SUBSYSTEM, SUBSYSTEM_NAME)
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

        ModelNode saslAuthenticationFactoryNode = Utils.applyRead(managementClient.getControllerClient() ,Util.getReadAttributeOperation(remotingConnectorAddress, "sasl-authentication-factory"), false);
        if(saslAuthenticationFactoryNode.isDefined()){
            saslAuthenticationFactoryValue = saslAuthenticationFactoryNode.asString();
        }

        ModelNode updateRemotingConnector = Util.getWriteAttributeOperation(remotingConnectorAddress, "sasl-authentication-factory", getSaslAuthenticationName());
        steps.add(updateRemotingConnector);

        // /subsystem=ejb3/application-security-domain=ejb3-tests:add(security-domain=ApplicationDomain)
        ModelNode addEjbDomain = Util.createAddOperation(ejbDomainAddress);
        addEjbDomain.get("security-domain").set(getSecurityDomainName());
        steps.add(addEjbDomain);

        applyUpdate(managementClient.getControllerClient(), compositeOp, false);
        // TODO: add {"allow-resource-service-restart" => true} to ejbDomainAddress write-attribute operation once WFLY-8793 / JBEAP-10955 is fixed
        //       and remove this reload
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) {
        try {
            ModelNode restoreSaslOperation;
            if (saslAuthenticationFactoryValue != null) {
                restoreSaslOperation = Util.getWriteAttributeOperation(remotingConnectorAddress, "sasl-authentication-factory", saslAuthenticationFactoryValue);
            } else {
                restoreSaslOperation = Util.getUndefineAttributeOperation(remotingConnectorAddress, "sasl-authentication-factory");
            }

            applyUpdate(managementClient.getControllerClient(), restoreSaslOperation, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        applyRemoveAllowReload(managementClient.getControllerClient(), ejbDomainAddress, false);
        // TODO: add {"allow-resource-service-restart" => true} to ejbDomainAddress write-attribute operation once WFLY-8793 / JBEAP-10955 is fixed
        //       and remove this reload
        try {
            ServerReload.reloadIfRequired(managementClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        applyRemoveAllowReload(managementClient.getControllerClient(), saslAuthenticationAddress, false);
    }

}
