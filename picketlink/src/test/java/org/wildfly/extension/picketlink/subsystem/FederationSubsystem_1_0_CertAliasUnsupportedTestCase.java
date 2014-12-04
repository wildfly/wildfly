/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.FederationExtension;

/**
 * @author Pedro Igor
 */
public class FederationSubsystem_1_0_CertAliasUnsupportedTestCase extends AbstractSubsystemTest {

    public FederationSubsystem_1_0_CertAliasUnsupportedTestCase() {
        super(FederationExtension.SUBSYSTEM_NAME, new FederationExtension());
    }

    @Test
    public void failAddTrustDomainWithCertAlias() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("federation-subsystem-1.0.xml");

        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml)
                .build();
        PathAddress address = PathAddress.pathAddress(
                FederationExtension.SUBSYSTEM_PATH)
                .append(ModelElement.FEDERATION.getName(), "federation-without-signatures")
                .append(ModelElement.IDENTITY_PROVIDER.getName(), "idp.war")
                .append(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN.getName(), "invalid-trust-domain");

        ModelNode operation = Util.createAddOperation(address);

        operation.get(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN_CERT_ALIAS.getName()).set("servercert");

        servicesA.executeForFailure(operation);
    }

    @Test
    public void failWriteTrustDomainWithCertAlias() throws Exception {
        //Parse the subsystem xml and install into the first controller
        String subsystemXml = readResource("federation-subsystem-1.0.xml");

        KernelServices servicesA = createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT)
                .setSubsystemXml(subsystemXml)
                .build();

        PathAddress address = PathAddress.pathAddress(
                FederationExtension.SUBSYSTEM_PATH)
                .append(ModelElement.FEDERATION.getName(), "federation-without-signatures")
                .append(ModelElement.IDENTITY_PROVIDER.getName(), "idp.war")
                .append(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN.getName(), "invalid-trust-domain");

        ModelNode operation = Util.getWriteAttributeOperation(address, ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN_CERT_ALIAS
                .getName(), new ModelNode("servercert"));

        servicesA.executeForFailure(operation);
    }
}
