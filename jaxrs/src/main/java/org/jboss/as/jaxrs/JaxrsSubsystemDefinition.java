/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class JaxrsSubsystemDefinition extends SimpleResourceDefinition {
    public static final String RESTEASY_ATOM = "org.jboss.resteasy.resteasy-atom-provider";
    public static final String RESTEASY_CDI = "org.jboss.resteasy.resteasy-cdi";
    public static final String RESTEASY_CLIENT_MICROPROFILE = "org.jboss.resteasy.resteasy-client-microprofile";
    public static final String RESTEASY_CRYPTO = "org.jboss.resteasy.resteasy-crypto";
    public static final String RESTEASY_VALIDATOR = "org.jboss.resteasy.resteasy-validator-provider";
    public static final String RESTEASY_CLIENT = "org.jboss.resteasy.resteasy-client";
    public static final String RESTEASY_CLIENT_API = "org.jboss.resteasy.resteasy-client-api";
    public static final String RESTEASY_CORE = "org.jboss.resteasy.resteasy-core";
    public static final String RESTEASY_CORE_SPI = "org.jboss.resteasy.resteasy-core-spi";
    public static final String RESTEASY_JAXB = "org.jboss.resteasy.resteasy-jaxb-provider";
    public static final String RESTEASY_JACKSON2 = "org.jboss.resteasy.resteasy-jackson2-provider";
    public static final String RESTEASY_JSON_P_PROVIDER = "org.jboss.resteasy.resteasy-json-p-provider";
    public static final String RESTEASY_JSON_B_PROVIDER = "org.jboss.resteasy.resteasy-json-binding-provider";
    public static final String RESTEASY_JSAPI = "org.jboss.resteasy.resteasy-jsapi";
    public static final String RESTEASY_MULTIPART = "org.jboss.resteasy.resteasy-multipart-provider";

    public static final String RESTEASY_TRACING = "org.jboss.resteasy.resteasy-tracing-api";

    public static final String JACKSON_DATATYPE_JDK8 = "com.fasterxml.jackson.datatype.jackson-datatype-jdk8";
    public static final String JACKSON_DATATYPE_JSR310 = "com.fasterxml.jackson.datatype.jackson-datatype-jsr310";

    public static final String JAXB_API = "jakarta.xml.bind.api";
    public static final String JSON_API = "jakarta.json.api";
    public static final String JAXRS_API = "jakarta.ws.rs.api";

    public static final String MP_REST_CLIENT = "org.eclipse.microprofile.restclient";
    static final JaxrsSubsystemDefinition INSTANCE = new JaxrsSubsystemDefinition();

    private JaxrsSubsystemDefinition() {
         super(new Parameters(JaxrsExtension.SUBSYSTEM_PATH, JaxrsExtension.getResolver())
                 .setAddHandler(new JaxrsSubsystemAdd())
                 .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                 .setAdditionalPackages(RuntimePackageDependency.passive(RESTEASY_CDI),
                         RuntimePackageDependency.passive(RESTEASY_VALIDATOR),
                         RuntimePackageDependency.passive(RESTEASY_JSON_B_PROVIDER),
                         RuntimePackageDependency.required(JAXRS_API),
                         RuntimePackageDependency.required(JAXB_API),
                         RuntimePackageDependency.required(JSON_API),
                         RuntimePackageDependency.optional(RESTEASY_ATOM),
                         RuntimePackageDependency.required(RESTEASY_CORE),
                         RuntimePackageDependency.required(RESTEASY_CORE_SPI),
                         // The deprecated module should be activated if present for cases when other modules depend on this
                         RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-jaxrs"),
                         RuntimePackageDependency.optional(RESTEASY_JAXB),
                         RuntimePackageDependency.optional(RESTEASY_JACKSON2),
                         RuntimePackageDependency.optional(RESTEASY_JSON_P_PROVIDER),
                         RuntimePackageDependency.optional(RESTEASY_JSAPI),
                         RuntimePackageDependency.optional(RESTEASY_MULTIPART),
                         RuntimePackageDependency.optional(RESTEASY_TRACING),
                         RuntimePackageDependency.optional(RESTEASY_CRYPTO),
                         RuntimePackageDependency.optional(JACKSON_DATATYPE_JDK8),
                         RuntimePackageDependency.optional(JACKSON_DATATYPE_JSR310),
                         // The following ones are optional dependencies located in org.jboss.as.jaxrs module.xml
                         // To be provisioned, they need to be explicitly added as optional packages.
                         RuntimePackageDependency.optional("org.jboss.resteasy.resteasy-spring"))
         );
    }

    @Override
    public void registerAttributes(final ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attributeDefinition : JaxrsAttribute.ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attributeDefinition, null, ReloadRequiredWriteAttributeHandler.INSTANCE);
        }
    }
}
