/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.as.test.multinode.security.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.MechanismRealmConfiguration;

/**
 * @author bmaxwell
 *
 */
public class ServerConfigUtil {

    private ManagementClient managementClient;
    private String containerId;

    /**
     *
     */
    public ServerConfigUtil(ManagementClient managementClient, String containerId) {
        this.managementClient = managementClient;
        this.containerId = containerId;
    }

    // /core-service=management/security-realm=ApplicationRealm/authentication=local:remove()
    /**
     * "address" => [ ("core-service" => "management"), ("security-realm" => "ApplicationRealm"), ("authentication" => "local")
     * ], "operation" => "remove"
     */
    private static ModelNode removeLocalAuthenticationFromApplicationRealmOperation() {
        final ModelNode address = new ModelNode();
        address.add("core-service", "management");
        address.add("security-realm", "ApplicationRealm");
        address.add("authentication", "local");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    // /core-service=management/security-realm=ApplicationRealm/authentication=local:add()
    /*
     * "address" => [ ("core-service" => "management"), ("security-realm" => "ApplicationRealm"), ("authentication" => "local")
     * ], "operation" => "add"
     */
    private static ModelNode addLocalAuthenticationFromApplicationRealmOperation() {
        final ModelNode address = new ModelNode();
        address.add("core-service", "management");
        address.add("security-realm", "ApplicationRealm");
        address.add("authentication", "local");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    // echo "configure ejb3 subsystem to use the elytron ApplicationDomain for the other"
    // /subsystem=ejb3/application-security-domain=other:add(security-domain=ApplicationDomain)
    /**
     * "address" => [ ("subsystem" => "ejb3"), ("application-security-domain" => "other") ], "operation" => "add",
     * "security-domain" => "ApplicationDomain"
     */
    private static ModelNode configureEjb3SubsystemToUseElytronApplicationDomainForOther() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("application-security-domain", "other");
        address.add("authentication", "local");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("security-domain").set("ApplicationDomain");
        return operation;
    }

    /*
     * /subsystem=ejb3/application-security-domain=other:remove() /subsystem=ejb3/application-security-domain=other:remove()
     *
     * "address" => [ ("subsystem" => "ejb3"), ("application-security-domain" => "other") ], "operation" => "remove"
     */
    private static ModelNode unconfigureEjb3SubsystemToUseElytronApplicationDomainForOther() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("application-security-domain", "other");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    // if (outcome == success) of /subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:read-resource()
    // echo "reconfigure the application-sasl-authentication to use PLAIN instead of DIGEST"
    // /subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:remove()
    // end-if
    /**
     * "address" => [ ("subsystem" => "elytron"), ("sasl-authentication-factory" => "application-sasl-authentication") ],
     * "operation" => "remove"
     */

    private static ModelNode removeAppSaslAuth() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "elytron");
        address.add("sasl-authentication-factory", "application-sasl-authentication");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        operation.get(OP_ADDR).set(address);
        return operation;
    }

    // echo "configure elytron application-sasl-authentication"
    /// subsystem=elytron/sasl-authentication-factory=application-sasl-authentication:add(sasl-server-factory=configured,
    // security-domain=ApplicationDomain, mechanism-configurations=[{mechanism-name=PLAIN,
    // mechanism-realm-configurations=[{realm-name=ApplicationRealm}]}])
    /**
     * "address" => [ ("subsystem" => "elytron"), ("sasl-authentication-factory" => "application-sasl-authentication") ],
     * "operation" => "add", "sasl-server-factory" => "configured", "security-domain" => "ApplicationDomain",
     * "mechanism-configurations" => [{ "mechanism-name" => "PLAIN", "mechanism-realm-configurations" => [{"realm-name" =>
     * "ApplicationRealm"}] }]
     */

    private static ModelNode configureAppSaslAuthToUsePlain() {
        return configureAppSaslAuthToUse("PLAIN");
    }

    /**
     * "address" => [ ("subsystem" => "elytron"), ("sasl-authentication-factory" => "application-sasl-authentication") ],
     * "operation" => "add", "sasl-server-factory" => "configured", "security-domain" => "ApplicationDomain",
     * "mechanism-configurations" => [{ "mechanism-name" => "DIGEST", "mechanism-realm-configurations" => [{"realm-name" =>
     * "ApplicationRealm"}] }]
     */
    private static ModelNode configureAppSaslAuthToUseDigest() {
        return configureAppSaslAuthToUse("DIGEST");
    }

    private static ModelNode configureAppSaslAuthToUse(String mechanismName) {
        MechanismRealmConfiguration mechanismRealmConfiguration = MechanismRealmConfiguration.builder()
                .withRealmName("ApplicationRealm").build();
        MechanismConfiguration config = MechanismConfiguration.builder().withMechanismName(mechanismName)
                .addMechanismRealmConfiguration(mechanismRealmConfiguration).build();
        final ModelNode address = new ModelNode();
        address.add("subsystem", "ejb3");
        address.add("sasl-authentication-factory", "application-sasl-authentication");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get(OP_ADDR).set(address);
        operation.get("sasl-server-factory").set("configured");
        operation.get("security-domain").set("ApplicationDomain");
        operation.get("mechanism-configurations").set(config.toModelNode());
        return operation;
    }

    // echo "configure remoting http-connector to use application-sasl-authentication"
    /// subsystem=remoting/http-connector=http-remoting-connector:write-attribute(name=sasl-authentication-factory,
    // value=application-sasl-authentication)
    /**
     * "address" => [ ("subsystem" => "remoting"), ("http-connector" => "http-remoting-connector") ], "operation" =>
     * "write-attribute", "name" => "sasl-authentication-factory", "value" => "application-sasl-authentication"
     */
    private static ModelNode configureRemotingHttpConnectoToUseAppSaslAuth() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "remoting");
        address.add("http-connector", "http-remoting-connector");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get("name").set("sasl-authentication-factory");
        operation.get("value").set("application-sasl-authentication");
        return operation;
    }

    // /subsystem=remoting/http-connector=http-remoting-connector:undefine-attribute(name=sasl-authentication-factory)
    // "address" => [
    // ("subsystem" => "remoting"),
    // ("http-connector" => "http-remoting-connector")
    // ],
    // "operation" => "undefine-attribute",
    // "name" => "sasl-authentication-factory"
    private static ModelNode unconfigureRemotingHttpConnectoToUseAppSaslAuth() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "remoting");
        address.add("http-connector", "http-remoting-connector");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("undefine-attribute");
        operation.get("name").set("sasl-authentication-factory");
        return operation;
    }

    /**
     * Configure Server side
     */
    public void configureServerSide() {
        removeLocalAuthenticationFromApplicationRealmOperation();
        configureEjb3SubsystemToUseElytronApplicationDomainForOther();
        removeAppSaslAuth();
        configureAppSaslAuthToUsePlain();
        configureRemotingHttpConnectoToUseAppSaslAuth();
    }

    /**
     * Unconfigure Server side
     */
    public void unconfigureServerSide() {
        unconfigureRemotingHttpConnectoToUseAppSaslAuth();
        removeAppSaslAuth();
        configureAppSaslAuthToUseDigest();
        unconfigureEjb3SubsystemToUseElytronApplicationDomainForOther();
        addLocalAuthenticationFromApplicationRealmOperation();
    }

    public void configureClientSide() {
        removeLocalAuthenticationFromApplicationRealmOperation();
        addElytronAuthConfigForwardIt();
        // addElytronAuthCtxForwardCtx();
        configureDefaultAuthContext();
        configureUndertowAppSecDomain();
    }

    public void unconfigureClientSide() {
        unconfigureUndertowAppSecDomain();
        unconfigureDefaultAuthContext();
        // removeElytronAuthCtxForwardCtx();
        removeElytronAuthConfigForwardIt();
        addLocalAuthenticationFromApplicationRealmOperation();
    }

    // client side

    // echo "add elytron authentication configuration forwardit"
    // /subsystem=elytron/authentication-configuration=forwardit:add(security-domain=ApplicationDomain,
    // sasl-mechanism-selector=#ALL)
    // "address" => [
    // ("subsystem" => "elytron"),
    // ("authentication-configuration" => "forwardit")
    // ],
    // "operation" => "add",
    // "security-domain" => "ApplicationDomain",
    // "sasl-mechanism-selector" => "#ALL"
    private static ModelNode addElytronAuthConfigForwardIt() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "elytron");
        address.add("authentication-configuration", "forwardit");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get("security-domain").set("ApplicationDomain");
        operation.get("sasl-mechanism-selector").set("#ALL");
        return operation;

    }

    private static ModelNode removeElytronAuthConfigForwardIt() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "elytron");
        address.add("authentication-configuration", "forwardit");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        return operation;
    }

    // echo "add elytron authentication context forwardctx using forwardit"
    // /subsystem=elytron/authentication-context=forwardctx:add(match-rules=[{match-no-user=true,
    // authentication-configuration=forwardit}])
    // "address" => [
    // ("subsystem" => "elytron"),
    // ("authentication-context" => "forwardctx")
    // ],
    // "operation" => "add",
    // "match-rules" => [{
    // "match-no-user" => "true",
    // "authentication-configuration" => "forwardit"
    // }]
    // private static ModelNode addElytronAuthCtxForwardCtx() {
    // final ModelNode address = new ModelNode();
    // address.add("subsystem", "elytron");
    // address.add("authentication-context", "forwardctx");
    // final ModelNode operation = new ModelNode();
    // operation.get(OP).set("add");
    //
    //
    // MatchRules matchRules = MatchRules.builder().withMatchNoUser(true).withAuthenticationConfiguration("forwardit").build();;
    // operation.get("match-rules").add(matchRules.
    // return operation;
    //
    // }
    // private static ModelNode removeElytronAuthCtxForwardCtx() {
    //
    // }

    // echo "configure default-authentication-context"
    // /subsystem=elytron:write-attribute(name=default-authentication-context, value=forwardctx)
    // "address" => [("subsystem" => "elytron")],
    // "operation" => "write-attribute",
    // "name" => "default-authentication-context",
    // "value" => "forwardctx"
    private static ModelNode configureDefaultAuthContext() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "elytron");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("write-attribute");
        operation.get("name").set("default-authentication-context");
        operation.get("value").set("forwardctx");
        return operation;
    }

    private static ModelNode unconfigureDefaultAuthContext() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "elytron");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("undefine-attribute");
        operation.get("name").set("default-authentication-context");
        return operation;
    }

    // echo "configure undertow application-security-domain other to use the application-http-authentication"
    // /subsystem=undertow/application-security-domain=other:add(http-authentication-factory=application-http-authentication)
    // "address" => [
    // ("subsystem" => "undertow"),
    // ("application-security-domain" => "other")
    // ],
    // "operation" => "add",
    // "http-authentication-factory" => "application-http-authentication"
    private static ModelNode configureUndertowAppSecDomain() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "undertow");
        address.add("application-security-domain", "other");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("add");
        operation.get("http-authentication-factory").set("application-http-authentication");
        return operation;
    }

    private static ModelNode unconfigureUndertowAppSecDomain() {
        final ModelNode address = new ModelNode();
        address.add("subsystem", "undertow");
        address.add("application-security-domain", "other");
        final ModelNode operation = new ModelNode();
        operation.get(OP).set("remove");
        return operation;
    }
}