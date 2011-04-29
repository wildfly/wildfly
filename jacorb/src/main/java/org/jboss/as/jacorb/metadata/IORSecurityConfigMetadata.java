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

package org.jboss.as.jacorb.metadata;

import java.io.Serializable;

/**
 * <p>
 * The {@code IORSecurityConfigMetadata} is a holder of CSIv2 security configuration. This information usually comes from
 * the configuration of EJB beans with enabled IIOP security.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@SuppressWarnings("unused")
public class IORSecurityConfigMetadata implements Serializable {

    private static final long serialVersionUID = -3341898910508715334L;

    /**
     * the root element for security between the end points.
     */
    private TransportConfig transportConfig;

    /**
     * as-context (CSIv2 authentication service) is the element describing the authentication
     * mechanism that will be used to authenticate the client. If specified it will be the
     * username-password mechanism.
     */
    private AsContext asContext;

    /**
     * sas-context (related to CSIv2 security attribute service) element describes the sas-context fields.
     */
    private SasContext sasContext;

    /**
     * <p>
     * Creates a default security configuration:
     * <ul>
     * <li>TransportConfig[integrity=supported, confidentiality=supported, establish-trust-in-target=supported,
     * establish-trust-in-client=supported, detect-misordering=supported, detect-replay=supported]</li>
     * <li>AsContext[auth-method=USERNAME_PASSWORD, realm=default, required=false]</li>
     * <li>SasContext[caller-propagation=NONE]</li>
     * </ul>
     * </p>
     */
    public IORSecurityConfigMetadata() {
        transportConfig = new TransportConfig();
        asContext = new AsContext();
        sasContext = new SasContext();
    }

    /**
     * <p>
     * Gets the transport config metadata.
     * </p>
     *
     * @return the {@code TransportConfig} instance that holds the transport configuration metadata.
     */
    public TransportConfig getTransportConfig() {
        return transportConfig;
    }

    /**
     * <p>
     * Sets the transport config metadata.
     * </p>
     *
     * @param config the {@code TransportConfig} instance containing the transport metadata.
     */
    public void setTransportConfig(TransportConfig config) {
        this.transportConfig = config;
    }

    /**
     * <p>
     * Gets the authentication service context.
     * </p>
     *
     * @return the {@code ASContext} instance that holds the authentication service metadata.
     */
    public AsContext getAsContext() {
        return asContext;
    }

    /**
     * <p>
     * Sets the authentication service context.
     * </p>
     *
     * @param context the {@code ASContext} instance containing the authentication service metadata.
     */
    public void setAsContext(AsContext context) {
        this.asContext = context;
    }

    /**
     * <p>
     * Gets the security attribute service context.
     * </p>
     *
     * @return the {@code SasContext} instance that holds the security attribute service metadata.
     */
    public SasContext getSasContext() {
        return sasContext;
    }

    /**
     * <p>
     * Sets the security attribute service context.
     * </p>
     *
     * @param context the {@code SasContext} containing the security attribute service metadata.
     */
    public void setSasContext(SasContext context) {
        this.sasContext = context;
    }

    @Override
    public String toString() {
        return
                "[transport-config=" + transportConfig +
                        ", as-context=" + asContext +
                        ", sas-context=" + sasContext + "]";
    }

    /**
     * <p>
     * The root element for security between the end points.
     * </p>
     */
    public class TransportConfig {
        public static final String INTEGRITY_NONE = "NONE";
        public static final String INTEGRITY_SUPPORTED = "SUPPORTED";
        public static final String INTEGRITY_REQUIRED = "REQUIRED";

        public static final String CONFIDENTIALITY_NONE = "NONE";
        public static final String CONFIDENTIALITY_SUPPORTED = "SUPPORTED";
        public static final String CONFIDENTIALITY_REQUIRED = "REQUIRED";

        public static final String DETECT_MISORDERING_NONE = "NONE";
        public static final String DETECT_MISORDERING_SUPPORTED = "SUPPORTED";
        public static final String DETECT_MISORDERING_REQUIRED = "REQUIRED";

        public static final String DETECT_REPLAY_NONE = "NONE";
        public static final String DETECT_REPLAY_SUPPORTED = "SUPPORTED";
        public static final String DETECT_REPLAY_REQUIRED = "REQUIRED";

        public static final String ESTABLISH_TRUST_IN_TARGET_NONE = "NONE";
        public static final String ESTABLISH_TRUST_IN_TARGET_SUPPORTED = "SUPPORTED";

        public static final String ESTABLISH_TRUST_IN_CLIENT_NONE = "NONE";
        public static final String ESTABLISH_TRUST_IN_CLIENT_SUPPORTED = "SUPPORTED";
        public static final String ESTABLISH_TRUST_IN_CLIENT_REQUIRED = "REQUIRED";

        /**
         * integrity element indicates if the server (target) supports integrity protected messages.
         * The valid values are NONE, SUPPORTED or REQUIRED.
         * Required element.
         */
        private final String integrity;

        /**
         * confidentiality element indicates if the server (target) supports privacy protected
         * messages. The values are NONE, SUPPORTED or REQUIRED.
         * Required element.
         */
        private final String confidentiality;

        /**
         * detect-misordering indicates if the server (target) supports detection
         * of message sequence errors. The values are NONE, SUPPORTED or REQUIRED.
         * Optional element.
         */
        private final String detectMisordering;

        /**
         * detect-replay indicates if the server (target) supports detection
         * of message replay attempts. The values are NONE, SUPPORTED or REQUIRED.
         * Optional element.
         */
        private final String detectReplay;

        /**
         * establish-trust-in-target element indicates if the target is capable of authenticating to a client.
         * The values are NONE or SUPPORTED.
         * Required element.
         */
        private final String establishTrustInTarget;

        /**
         * establish-trust-in-client element indicates if the target is capable of authenticating a client. The
         * values are NONE, SUPPORTED or REQUIRED.
         * Required element.
         */
        private final String establishTrustInClient;

        /**
         * <p>
         * Creates a new {@code TransportConfig}.
         * </p>
         */
        private TransportConfig() {
            integrity = INTEGRITY_SUPPORTED;
            confidentiality = CONFIDENTIALITY_SUPPORTED;
            establishTrustInTarget = ESTABLISH_TRUST_IN_TARGET_SUPPORTED;
            establishTrustInClient = ESTABLISH_TRUST_IN_CLIENT_SUPPORTED;
            this.detectMisordering = DETECT_MISORDERING_SUPPORTED;
            this.detectReplay = DETECT_REPLAY_SUPPORTED;
        }

        /**
         * <p>
         * Creates a new {@code TransportConfig} with the specified properties.
         * </p>
         *
         * @param integrity              a {@code String} representing the transport integrity level.
         * @param confidentiality        a {@code String} representing the transport integrity level.
         * @param establishTrustInTarget a {@code String} that indicates if target trust must be established or not.
         * @param establishTrustInClient a {@code String} that indicates if client trust must be established or not.
         * @param detectMisordering      a {@code String} representing the transport detect-misordering level.
         * @param detectReplay           a {@code String} representing the transport detect-replay level.
         */
        public TransportConfig(String integrity, String confidentiality, String establishTrustInTarget,
                               String establishTrustInClient, String detectMisordering, String detectReplay) {
            // set the transport integrity property.
            if (integrity == null)
                throw new IllegalArgumentException("Null integrity");
            if (INTEGRITY_NONE.equalsIgnoreCase(integrity))
                this.integrity = INTEGRITY_NONE;
            else if (INTEGRITY_SUPPORTED.equalsIgnoreCase(integrity))
                this.integrity = INTEGRITY_SUPPORTED;
            else if (INTEGRITY_REQUIRED.equalsIgnoreCase(integrity))
                this.integrity = INTEGRITY_REQUIRED;
            else
                throw new IllegalArgumentException("Unknown transport integrity: " + integrity);

            // set the transport confidentiality property.
            if (confidentiality == null)
                throw new IllegalArgumentException("Null confidentiality");
            if (CONFIDENTIALITY_NONE.equalsIgnoreCase(confidentiality))
                this.confidentiality = CONFIDENTIALITY_NONE;
            else if (CONFIDENTIALITY_SUPPORTED.equalsIgnoreCase(confidentiality))
                this.confidentiality = CONFIDENTIALITY_SUPPORTED;
            else if (CONFIDENTIALITY_REQUIRED.equalsIgnoreCase(confidentiality))
                this.confidentiality = CONFIDENTIALITY_REQUIRED;
            else
                throw new IllegalArgumentException("Unknown transport confidentiality: " + confidentiality);

            // set the transport establish-trust-in-target property.
            if (establishTrustInTarget == null)
                throw new IllegalArgumentException("Null establishTrustInTarget");
            if (ESTABLISH_TRUST_IN_TARGET_NONE.equalsIgnoreCase(establishTrustInTarget))
                this.establishTrustInTarget = ESTABLISH_TRUST_IN_TARGET_NONE;
            else if (ESTABLISH_TRUST_IN_TARGET_SUPPORTED.equalsIgnoreCase(establishTrustInTarget))
                this.establishTrustInTarget = ESTABLISH_TRUST_IN_TARGET_SUPPORTED;
            else
                throw new IllegalArgumentException("Unknown transport establishTrustInTarget: " + establishTrustInTarget);

            // set the transport establish-trust-in-client property.
            if (establishTrustInClient == null)
                throw new IllegalArgumentException("Null establishTrustInClient");
            if (ESTABLISH_TRUST_IN_CLIENT_NONE.equalsIgnoreCase(establishTrustInClient))
                this.establishTrustInClient = ESTABLISH_TRUST_IN_CLIENT_NONE;
            else if (ESTABLISH_TRUST_IN_CLIENT_SUPPORTED.equalsIgnoreCase(establishTrustInClient))
                this.establishTrustInClient = ESTABLISH_TRUST_IN_CLIENT_SUPPORTED;
            else if (ESTABLISH_TRUST_IN_CLIENT_REQUIRED.equalsIgnoreCase(establishTrustInClient))
                this.establishTrustInClient = ESTABLISH_TRUST_IN_CLIENT_REQUIRED;
            else
                throw new IllegalArgumentException("Unknown transport establishTrustInClient: " + establishTrustInClient);

            // set the transport detect-misordering optional property - default value is SUPPORTED.
            if (detectMisordering == null || DETECT_MISORDERING_SUPPORTED.equalsIgnoreCase(detectMisordering))
                this.detectMisordering = DETECT_MISORDERING_SUPPORTED;
            else if (DETECT_MISORDERING_NONE.equalsIgnoreCase(detectMisordering))
                this.detectMisordering = DETECT_MISORDERING_NONE;
            else if (DETECT_MISORDERING_REQUIRED.equalsIgnoreCase(detectMisordering))
                this.detectMisordering = DETECT_MISORDERING_REQUIRED;
            else
                throw new IllegalArgumentException("Unknown transport detectMisordering: " + detectMisordering);

            // set the transport detect-replay optional property - default value is SUPPORTED.
            if (detectReplay == null || DETECT_REPLAY_SUPPORTED.equalsIgnoreCase(detectReplay))
                this.detectReplay = DETECT_REPLAY_SUPPORTED;
            else if (DETECT_REPLAY_NONE.equalsIgnoreCase(detectReplay))
                this.detectReplay = DETECT_REPLAY_NONE;
            else if (DETECT_REPLAY_REQUIRED.equalsIgnoreCase(detectReplay))
                this.detectReplay = DETECT_REPLAY_REQUIRED;
            else
                throw new IllegalArgumentException("Unknown transport detectReplay: " + detectReplay);
        }

        /**
         * <p>
         * Gets the transport integrity mode.
         * </p>
         *
         * @return the transport integrity mode. Can be "NONE", "SUPPORTED" or "REQUIRED".
         */
        public String getIntegrity() {
            return integrity;
        }

        /**
         * <p>
         * Gets the transport confidentiality mode.
         * </p>
         *
         * @return the transport confidentiality mode. Can be "NONE", "SUPPORTED" or "REQUIRED".
         */
        public String getConfidentiality() {
            return confidentiality;
        }

        /**
         * <p>
         * Gets the transport detect misordering mode.
         * </p>
         *
         * @return the transport detect misordering mode. Can be "NONE", "SUPPORTED" or "REQUIRED".
         */
        public String getDetectMisordering() {
            return detectMisordering;
        }

        /**
         * <p>
         * Gets the transport detect replay mode.
         * </p>
         *
         * @return the transport detect replay mode. Can be "NONE", "SUPPORTED" or "REQUIRED".
         */
        public String getDetectReplay() {
            return detectReplay;
        }

        /**
         * <p>
         * Gets the transport establish trust in target mode.
         * </p>
         *
         * @return the transport establish trust in target mode. Can be "NONE" or "SUPPORTED".
         */
        public String getEstablishTrustInTarget() {
            return establishTrustInTarget;
        }

        /**
         * <p>
         * Gets the transport establish trust in client mode.
         * </p>
         *
         * @return the transport establish trust in client mode. Can be "NONE", "SUPPORTED" or "REQUIRED".
         */
        public String getEstablishTrustInClient() {
            return establishTrustInClient;
        }

        /**
         * <p>
         * Indicates whether establish trust in target is supported or not.
         * </p>
         *
         * @return {@code true} if it is supported; {@code false} otherwise.
         */
        public boolean isEstablishTrustInTargetSupported() {
            return ESTABLISH_TRUST_IN_TARGET_SUPPORTED.equalsIgnoreCase(establishTrustInTarget);
        }

        @Override
        public String toString() {
            return
                    "[integrity=" + integrity +
                            ", confidentiality=" + confidentiality +
                            ", establish-trust-in-target=" + establishTrustInTarget +
                            ", establish-trust-in-client=" + establishTrustInClient +
                            ", detect-misordering=" + detectMisordering +
                            ", detect-replay=" + detectReplay + "]";
        }
    }

    /**
     * <p>
     * as-context (CSIv2 authentication service) is the element describing the authentication mechanism that will be
     * used to authenticate the client. It can be either the username-password mechanism, or none (default).
     * </p>
     */
    public class AsContext {
        public static final String AUTH_METHOD_USERNAME_PASSWORD = "USERNAME_PASSWORD";
        public static final String AUTH_METHOD_NONE = "NONE";

        /**
         * auth-method element describes the authentication method. The only supported values are USERNAME_PASSWORD and
         * NONE. Required element.
         */
        private final String authMethod;

        /**
         * realm element describes the realm in which the user is authenticated. Must be a valid realm that is registered
         * in server configuration. Required element.
         */
        private final String realm;

        /**
         * required element specifies if the authentication method specified is required to be used for client authentication.
         * If so the EstablishTrustInClient bit will be set in the target_requires field of the AS_Context. The element value
         * is either true or false. Required element.
         */
        private final boolean required;

        /**
         * <p>
         * Creates a new {@code AsContext}.
         * </p>
         */
        private AsContext() {
            authMethod = AUTH_METHOD_USERNAME_PASSWORD;
            realm = "default";
            required = false;
        }

        /**
         * <p>
         * Creates a new {@code AsContext} with the specified properties.
         * </p>
         *
         * @param authMethod the authentication method to be used.
         * @param realm      the realm in which the user is authenticated.
         * @param required   a {@code boolean} that specifies if the authentication method is required to be used for
         *                   client authentication.
         */
        public AsContext(String authMethod, String realm, boolean required) {
            // set the auth method.
            if (authMethod == null)
                throw new IllegalArgumentException("Null authMethod");
            if (AUTH_METHOD_NONE.equalsIgnoreCase(authMethod))
                this.authMethod = AUTH_METHOD_NONE;
            else if (AUTH_METHOD_USERNAME_PASSWORD.equalsIgnoreCase(authMethod))
                this.authMethod = AUTH_METHOD_USERNAME_PASSWORD;
            else
                throw new IllegalArgumentException("Unknown ascontext authMethod: " + authMethod);

            // set the realm.
            if (realm == null)
                throw new IllegalArgumentException("Null realm");
            this.realm = realm;

            this.required = required;
        }

        /**
         * <p>
         * Gets the authentication method.
         * </p>
         *
         * @return the authentication method. Can be "NONE" or "USERNAME_PASSWORD".
         */
        public String getAuthMethod() {
            return authMethod;
        }

        /**
         * <p>
         * Gets the authentication realm name.
         * </p>
         *
         * @return a {@code String} representing the authentication realm.
         */
        public String getRealm() {
            return realm;
        }

        /**
         * <p>
         * Indicates whether the authentication method is required or not.
         * </p>
         *
         * @return {@code true} if the authentication method is required; {@code false} otherwise.
         */
        public boolean isRequired() {
            return required;
        }

        @Override
        public String toString() {
            return
                    "[auth-method=" + authMethod +
                            ", realm=" + realm +
                            ", required=" + required + "]";
        }
    }

    /**
     * <p>
     * sas-context (related to CSIv2 security attribute service) element describes the sas-context fields.
     * </p>
     */
    public class SasContext {
        public static final String CALLER_PROPAGATION_NONE = "NONE";
        public static final String CALLER_PROPAGATION_SUPPORTED = "SUPPORTED";

        /**
         * caller-propagation element indicates if the target will accept propagated caller identities The values are
         * NONE or SUPPORTED. Required element.
         */
        private final String callerPropagation;

        /**
         * <p>
         * Creates a new {@code SasContext}.
         * </p>
         */
        private SasContext() {
            callerPropagation = CALLER_PROPAGATION_NONE;
        }

        /**
         * <p>
         * Creates a new {@code SasContext} with the specified properties.
         * </p>
         *
         * @param callerPropagation indicates if the target will accept propagated caller identities or not.
         */
        public SasContext(String callerPropagation) {
            if (callerPropagation == null)
                throw new IllegalArgumentException("Null callerPropagation");
            if (CALLER_PROPAGATION_NONE.equalsIgnoreCase(callerPropagation))
                this.callerPropagation = CALLER_PROPAGATION_NONE;
            else if (CALLER_PROPAGATION_SUPPORTED.equalsIgnoreCase(callerPropagation))
                this.callerPropagation = CALLER_PROPAGATION_SUPPORTED;
            else
                throw new IllegalArgumentException("Unknown sascontext callerPropagtion: " + callerPropagation);
        }

        /**
         * <p>
         * Gets the caller propagation mode.
         * </p>
         *
         * @return the caller propagation mode. Can be "NONE" or "SUPPORTED".
         */
        public String getCallerPropagation() {
            return callerPropagation;
        }

        /**
         * <p>
         * Indicates whether caller propagation is supported or not.
         * </p>
         *
         * @return {@code true} if caller propagation is supported; {@code false} otherwise.
         */
        public boolean isCallerPropagationSupported() {
            return CALLER_PROPAGATION_SUPPORTED.equalsIgnoreCase(callerPropagation);
        }

        @Override
        public String toString() {
            return "[caller-propagation=" + callerPropagation + "]";
        }
    }
}
