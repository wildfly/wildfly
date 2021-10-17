/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2009, Red Hat Inc, and individual contributors
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
package org.jboss.as.connector.deployers.spec.rars.ra16inoutmultianno;

import org.jboss.as.connector.deployers.spec.rars.BaseResourceAdapter;

import javax.resource.spi.AdministeredObject;
import javax.resource.spi.AuthenticationMechanism;
import javax.resource.spi.AuthenticationMechanism.CredentialInterface;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.Connector;
import javax.resource.spi.SecurityPermission;
import javax.resource.spi.TransactionSupport;

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
