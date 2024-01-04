/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.AuthenticationMechanism;
import jakarta.resource.spi.AuthenticationMechanism.CredentialInterface;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.SecurityPermission;
import jakarta.resource.spi.TransactionSupport;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.resource.spi.work.HintsContext;
import jakarta.resource.spi.work.TransactionContext;
import javax.transaction.xa.XAResource;

/**
 * AnnoResourceAdapter
 *
 * @version $Revision: $
 */
@Connector(description = {"first", "second"}, displayName = {"disp1",
        "disp2"}, smallIcon = {"s1", "", "s3", ""}, largeIcon = {"l1",
        "l2", "", ""}, vendorName = "vendor", eisType = "type", version = "1.a", licenseRequired = true, licenseDescription = {
        "lic1", "lic2"}, reauthenticationSupport = true, transactionSupport = TransactionSupport.TransactionSupportLevel.LocalTransaction, authMechanisms = {
        @AuthenticationMechanism(credentialInterface = CredentialInterface.PasswordCredential),
        @AuthenticationMechanism(credentialInterface = CredentialInterface.GenericCredential, authMechanism = "AuthMechanism", description = {
                "desc1", "desc2"})}, securityPermissions = {
        @SecurityPermission(permissionSpec = "spec1"),
        @SecurityPermission(permissionSpec = "spec2", description = {"d1",
                "d2"})}, requiredWorkContexts = {TransactionContext.class,
        HintsContext.class})
public class AnnoResourceAdapter implements ResourceAdapter,
        java.io.Serializable {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("AnnoResourceAdapter");

    /**
     * The activations by activation spec
     */
    private ConcurrentHashMap<AnnoActivationSpec, AnnoActivation> activations;

    /**
     * first
     */
    @ConfigProperty(defaultValue = "A", description = {"1st", "first"}, ignore = true, supportsDynamicUpdates = false, confidential = true)
    private String first;

    /**
     * second
     */
    private Integer second;

    /**
     * Default constructor
     */
    public AnnoResourceAdapter() {
        this.activations = new ConcurrentHashMap<AnnoActivationSpec, AnnoActivation>();

    }

    /**
     * Set first
     *
     * @param first The value
     */
    public void setFirst(String first) {
        this.first = first;
    }

    /**
     * Get first
     *
     * @return The value
     */
    public String getFirst() {
        return first;
    }

    /**
     * Set second
     *
     * @param second The value
     */
    @ConfigProperty(defaultValue = "5", description = {"2nd", "second"}, ignore = false, supportsDynamicUpdates = true, confidential = false)
    public void setSecond(Integer second) {
        this.second = second;
    }

    /**
     * Get second
     *
     * @return The value
     */
    public Integer getSecond() {
        return second;
    }

    /**
     * This is called during the activation of a message endpoint.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     * @throws ResourceException generic exception
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory,
                                   ActivationSpec spec) throws ResourceException {
        AnnoActivation activation = new AnnoActivation(this, endpointFactory,
                (AnnoActivationSpec) spec);
        activations.put((AnnoActivationSpec) spec, activation);
        activation.start();

        log.trace("endpointActivation()");
    }

    /**
     * This is called when a message endpoint is deactivated.
     *
     * @param endpointFactory A message endpoint factory instance.
     * @param spec            An activation spec JavaBean instance.
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory,
                                     ActivationSpec spec) {
        AnnoActivation activation = activations.remove(spec);
        if (activation != null) { activation.stop(); }

        log.trace("endpointDeactivation()");
    }

    /**
     * This is called when a resource adapter instance is bootstrapped.
     *
     * @param ctx A bootstrap context containing references
     * @throws ResourceAdapterInternalException indicates bootstrap failure.
     */
    public void start(BootstrapContext ctx)
            throws ResourceAdapterInternalException {
        log.trace("start()");
    }

    /**
     * This is called when a resource adapter instance is undeployed or during
     * application server shutdown.
     */
    public void stop() {
        log.trace("stop()");
    }

    /**
     * This method is called by the application server during crash recovery.
     *
     * @param specs An array of ActivationSpec JavaBeans
     * @return An array of XAResource objects
     * @throws ResourceException generic exception
     */
    public XAResource[] getXAResources(ActivationSpec[] specs)
            throws ResourceException {
        log.trace("getXAResources()");
        return null;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return A hash code value for this object.
     */
    @Override
    public int hashCode() {
        int result = 17;
        if (first != null) { result += 31 * result + 7 * first.hashCode(); } else { result += 31 * result + 7; }
        if (second != null) { result += 31 * result + 7 * second.hashCode(); } else { result += 31 * result + 7; }
        return result;
    }

    /**
     * Indicates whether some other object is equal to this one.
     *
     * @param other The reference object with which to compare.
     * @return true if this object is the same as the obj argument, false
     * otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof AnnoResourceAdapter)) { return false; }
        boolean result = true;
        AnnoResourceAdapter obj = (AnnoResourceAdapter) other;
        if (result) {
            if (first == null) { result = obj.getFirst() == null; } else { result = first.equals(obj.getFirst()); }
        }
        if (result) {
            if (second == null) { result = obj.getSecond() == null; } else { result = second.equals(obj.getSecond()); }
        }
        return result;
    }

}
