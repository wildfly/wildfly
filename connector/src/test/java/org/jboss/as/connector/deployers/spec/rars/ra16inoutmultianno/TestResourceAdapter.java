/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars.ra16inoutmultianno;

import org.jboss.as.connector.deployers.spec.rars.BaseResourceAdapter;

import jakarta.resource.spi.AdministeredObject;
import jakarta.resource.spi.AuthenticationMechanism;
import jakarta.resource.spi.AuthenticationMechanism.CredentialInterface;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.SecurityPermission;
import jakarta.resource.spi.TransactionSupport;

/**
 * TestResourceAdapter
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
@Connector(description = { "Test RA" }, displayName = { "displayName" }, smallIcon = { "smallIcon" }, largeIcon = {
        "largeIcon" }, vendorName = "Red Hat Inc", eisType = "Test RA", version = "0.1", licenseDescription = {
                "licenseDescription" }, licenseRequired = true, reauthenticationSupport = true, authMechanisms = {
                        @AuthenticationMechanism(credentialInterface = CredentialInterface.PasswordCredential) }, securityPermissions = {
                                @SecurityPermission(permissionSpec = "permissionSpec") }, transactionSupport = TransactionSupport.TransactionSupportLevel.LocalTransaction)
// TODO API has not been updated requiredWorkContexts = null
@AdministeredObject(adminObjectInterfaces = TestAdminObjectInterface.class)
public class TestResourceAdapter extends BaseResourceAdapter {
    @ConfigProperty(type = String.class, defaultValue = "JCA")
    private String myStringProperty;

    /**
     * @param myStringProperty the myStringProperty to set
     */
    public void setMyStringProperty(String myStringProperty) {
        this.myStringProperty = myStringProperty;
    }

    /**
     * @return the myStringProperty
     */
    public String getMyStringProperty() {
        return myStringProperty;
    }
}
