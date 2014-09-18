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
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.FederationExtension;
import org.wildfly.extension.picketlink.federation.Namespace;

import java.io.IOException;

/**
 * @author Pedro Igor
 */
public class FederationSubsystem_1_0_CertAliasUnsupportedTestCase extends AbstractSubsystemBaseTest {

    public FederationSubsystem_1_0_CertAliasUnsupportedTestCase() {
        super(FederationExtension.SUBSYSTEM_NAME, new FederationExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("federation-subsystem-1.0.xml");
    }

    @Test
    public void failAddTrustDomainWithCertAlias() throws Exception {
        KernelServices mainServices = createKernelServicesBuilder(new AdditionalInitialization() {
            @Override
            protected ProcessType getProcessType() {
                return ProcessType.STANDALONE_SERVER;
            }
        }).setSubsystemXml(getSubsystemXml()).build();

        PathAddress address = PathAddress.pathAddress(
            FederationExtension.SUBSYSTEM_PATH)
            .append(ModelElement.FEDERATION.getName(), "federation-without-signatures")
            .append(ModelElement.IDENTITY_PROVIDER.getName(), "idp.war")
            .append(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN.getName(), "invalid-trust-domain");

        ModelNode operation = Util.createAddOperation(address);

        operation.get(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN_CERT_ALIAS.getName()).set("servercert");

        mainServices.executeForFailure(operation);
    }

    @Test
    public void failWriteTrustDomainWithCertAlias() throws Exception {
        KernelServices mainServices = createKernelServicesBuilder(new AdditionalInitialization() {
            @Override
            protected ProcessType getProcessType() {
                return ProcessType.STANDALONE_SERVER;
            }
        }).setSubsystemXml(getSubsystemXml()).build();

        PathAddress address = PathAddress.pathAddress(
            FederationExtension.SUBSYSTEM_PATH)
            .append(ModelElement.FEDERATION.getName(), "federation-without-signatures")
            .append(ModelElement.IDENTITY_PROVIDER.getName(), "idp.war")
            .append(ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN.getName(), "invalid-trust-domain");

        ModelNode operation = Util.getWriteAttributeOperation(address, ModelElement.IDENTITY_PROVIDER_TRUST_DOMAIN_CERT_ALIAS
            .getName(), new ModelNode("servercert"));

        mainServices.executeForFailure(operation);
    }

    @Override
    protected String normalizeXML(String xml) throws Exception {
        return super.normalizeXML(xml).replace(Namespace.PICKETLINK_FEDERATION_1_0.getUri(), Namespace.PICKETLINK_FEDERATION_2_0.getUri());
    }
}
