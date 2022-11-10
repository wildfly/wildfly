/*
 * Copyright 2016-2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
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
package org.jboss.as.test.layers;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jdenise@redhat.com
 */
public class LayersTestCase {
    // Packages that are provisioned by the test-standalone-reference installation
    // but not used in the test-all-layers installation.
    // This is the expected set of not provisioned modules when all layers are provisioned.
    private static final String[] NOT_USED_COMMON = {
            // test-all-layers installation is non-ha and does not include layers that provide jgroups
            "org.jboss.as.clustering.jgroups",
            // TODO we need to add an agroal layer
            "org.wildfly.extension.datasources-agroal",
            "io.agroal",
            // Legacy subsystems for which we will not provide layers
            "org.wildfly.extension.picketlink",
            "org.jboss.as.jacorb",
            "org.jboss.as.jsr77",
            "org.jboss.as.messaging",
            "org.jboss.as.web",
            "org.keycloak.keycloak-adapter-subsystem",
            "org.jboss.as.security",
            // end legacy subsystems ^^^
            // TODO nothing references this
            "org.wildfly.security.http.sfbasic",
            // TODO Legacy Seam integration. Does it even work with EE 10?
            "org.jboss.integration.ext-content",
            // TODO move eclipse link support to an external feature pack
            "org.eclipse.persistence",
            // RA not associated with any layer
            "org.jboss.genericjms",
            // Appclient support is not provided by a layer
            "org.jboss.as.appclient",
            "org.jboss.metadata.appclient",
            // TODO WFLY-16576 -- cruft?
            "org.bouncycastle",
            // This was brought in as part an RFE, WFLY-10632 & WFLY-10636. While the module is currently marked as private,
            // for now we should keep this module.
            "org.jboss.resteasy.resteasy-rxjava2",
            // TODO these implement SPIs from RESTEasy or JBoss WS but I don't know how they integrate
            // as there is no ref to them in any module.xml nor any in WF java code.
            // Perhaps via deployment descriptor? In any case, no layer provides them
            "org.wildfly.security.jakarta.client.resteasy",
            "org.wildfly.security.jakarta.client.webservices",
            // This is added in the jaxrs subsystem to deployments if the MP config capability is met. The package is
            // added in the microprofile-rest-client as well.
            "org.jboss.resteasy.microprofile.config",
            // Temporarily provided to not break the wildfly-extras gRPC feature pack
            // until it provides it itself.
            "io.grpc",
            "com.google.protobuf",
            // Alternative messaging protocols besides the std Artemis core protocol
            // Use of these depends on an attribute value setting
            "org.apache.activemq.artemis.protocol.amqp",
            "org.apache.qpid.proton",
            "org.apache.activemq.artemis.protocol.hornetq",
            "org.apache.activemq.artemis.protocol.stomp",
            // Legacy client not associated with any layer
            "org.hornetq.client",
            // TODO we need to add an rts layer
            "org.wildfly.extension.rts",
            "org.jboss.narayana.rts",
            // TODO we need to add an xts layer
            "org.jboss.as.xts",
            // TODO we need to add the elytron-oidc-client layer to the config
            "org.wildfly.extension.elytron-oidc-client",
            "org.wildfly.security.elytron-http-oidc",
            "org.wildfly.security.elytron-jose-jwk",
            "org.wildfly.security.elytron-jose-util",
            // TODO WFLY-16586 microprofile-reactive-streams-operators layer should provision this
            "org.wildfly.reactive.dep.jts",
            // TODO should an undertow layer specify this?
            "org.wildfly.event.logger",
            // TODO test-all-layers uses microprofile-opentracing instead of opentelemetry
            "org.wildfly.extension.opentelemetry",
            "org.wildfly.extension.opentelemetry-api",
            "io.opentelemetry.trace",
            // JGroups external protocols - AWS
            "com.amazon.aws.core",
            "com.amazon.aws.jmespath",
            "com.amazon.aws.kms",
            "com.amazon.aws.s3",
            "com.amazon.ion",
            "com.fasterxml.jackson.dataformat.jackson-dataformat-cbor",
            "org.jgroups.aws",
    };
    private static final String[] NOT_USED;
    // Packages that are not referenced from the module graph but needed.
    // This is the expected set of un-referenced modules found when scanning
    // the default configuration.
    private static final String[] NOT_REFERENCED_COMMON = {
            // May be needed by deployments if running on IBM JDK.
            "ibm.jdk",
            // injected by ee
            "org.eclipse.yasson",
            // injected by ee
            "org.wildfly.naming",
            // Injected by jaxrs
            "org.jboss.resteasy.resteasy-json-binding-provider",
            // The console ui content is not part of the kernel nor is it provided by an extension
            "org.jboss.as.console",
            // tooling
            "org.jboss.as.domain-add-user",
            // injected by server in UndertowHttpManagementService
            "org.jboss.as.domain-http-error-context",
            // injected by jsf
            "org.jboss.as.jsf-injection",
            // Brought by galleon FP config
            "org.jboss.as.product",
            // Brought by galleon FP config
            "org.jboss.as.standalone",
            // injected by logging
            "org.jboss.logging.jul-to-slf4j-stub",
            "org.jboss.resteasy.resteasy-client-microprofile",
            // Webservices tooling
            "org.jboss.ws.tools.common",
            "org.jboss.ws.tools.wsconsume",
            "org.jboss.ws.tools.wsprovide",
            "gnu.getopt",
            // Elytron tooling
            "org.wildfly.security.elytron-tool",
            // bootable jar runtime
            "org.wildfly.bootable-jar",
            // Extension not included in the default config
            "org.wildfly.extension.clustering.singleton",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.health-smallrye",
            "org.eclipse.microprofile.health.api",
            "io.smallrye.health",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.openapi-smallrye",
            "org.eclipse.microprofile.openapi.api",
            "io.smallrye.openapi",
            "com.fasterxml.jackson.dataformat.jackson-dataformat-yaml",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.reactive-messaging-smallrye",
            // Extension not included in the default config
            "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye",
            "org.wildfly.reactive.mutiny.reactive-streams-operators.cdi-provider",
            "io.vertx.client",
            // Dynamically added by ee-security and mp-jwt-smallrye DUPs but not referenced by subsystems.
            "org.wildfly.security.jakarta.security",
            // injected by sar
            "org.jboss.as.system-jmx",
            // Loaded reflectively by the jboss fork impl of jakarta.xml.soap.FactoryFinder
            "org.jboss.ws.saaj-impl",
            // TODO just a testsuite utility https://wildfly.zulipchat.com/#narrow/stream/174184-wildfly-developers/topic/org.2Ejboss.2Ews.2Ecxf.2Ests.20module
            "org.jboss.ws.cxf.sts",
    };
    private static final String[] NOT_REFERENCED;

    /**
     * A HashMap to configure a banned module.
     * They key is the banned module name, the value is an optional List with the installation names that are allowed to
     * provision the banned module. This installations will be ignored.
     *
     * Notice the allowed installation names does not distinguish between different parent names, e.g test-all-layers here means
     * allowing root/test-all-layers and servletRoot/test-all-layers.
     */
    private static final HashMap<String, List<String>> BANNED_MODULES_CONF = new HashMap<String, List<String>>(){{
        put("org.jboss.as.security", Arrays.asList("test-all-layers-jpa-distributed", "test-all-layers", "legacy-security", "test-standalone-reference"));
    }};

    static {
        if (AssumeTestGroupUtil.isWildFlyPreview()) {
            NOT_USED = ArrayUtils.addAll(
                    NOT_USED_COMMON,
                    // WFP standard config uses Micrometer instead of WF Metrics
                    "io.smallrye.metrics",
                    "org.wildfly.extension.metrics",
                    "org.wildfly.extension.microprofile.metrics-smallrye",
                    // MP Fault Tolerance has a dependency on MP Metrics
                    "io.smallrye.fault-tolerance",
                    "org.eclipse.microprofile.fault-tolerance.api",
                    "org.wildfly.extension.microprofile.fault-tolerance-smallrye",
                    "org.wildfly.microprofile.fault-tolerance-smallrye.executor",
                    // Used by Hibernate Search but only in preview TODO this doesn't seem right; NOT_REFERENCED should suffice
                    "org.hibernate.search.mapper.orm.coordination.outboxpolling"
            );

            NOT_REFERENCED = ArrayUtils.addAll(
                    NOT_REFERENCED_COMMON,
                    "org.wildfly.extension.metrics",
                    // Used by the hibernate search that's injected by jpa
                    "org.hibernate.search.mapper.orm.coordination.outboxpolling"
            );
        } else {
            NOT_USED = ArrayUtils.addAll(
                    NOT_USED_COMMON,
                    // Messaging broker not included in the messaging-activemq layer
                    "org.jboss.xnio.netty.netty-xnio-transport",
                    // No patching modules in layers
                    "org.jboss.as.patching",
                    "org.jboss.as.patching.cli",
                    // Misc alternative variants of JPA things that we don't provide via layers
                    "org.jboss.as.jpa.hibernate:4",
                    "org.hibernate:5.0",
                    "org.hibernate.jipijapa-hibernate5",
                    "org.jboss.as.jpa.openjpa",
                    "org.apache.openjpa",
                    // TODO WFLY-16583 -- cruft
                    "javax.management.j2ee.api",
                    // Used by outboxpolling TODO this doesn't seem right
                    "org.apache.avro"
            );
            NOT_REFERENCED = ArrayUtils.addAll(
                    NOT_REFERENCED_COMMON,
                    // Standard configs don't include various MP subsystems
                    "org.wildfly.extension.microprofile.fault-tolerance-smallrye",
                    "org.wildfly.extension.microprofile.metrics-smallrye",
                    "io.jaegertracing",
                    "io.netty.netty-codec-dns",
                    "io.netty.netty-codec-http2",
                    "io.netty.netty-resolver-dns",
                    "io.reactivex.rxjava2.rxjava",
                    "io.smallrye.common.vertx-context",
                    "io.smallrye.reactive.messaging",
                    "io.smallrye.reactive.messaging.connector",
                    "io.smallrye.reactive.messaging.connector.kafka",
                    "io.smallrye.reactive.messaging.connector.kafka.api",
                    "io.smallrye.reactive.mutiny",
                    "io.smallrye.reactive.mutiny.reactive-streams-operators",
                    "io.smallrye.reactive.mutiny.vertx-core",
                    "io.smallrye.reactive.mutiny.vertx-kafka-client",
                    "io.smallrye.reactive.mutiny.vertx-runtime",
                    "io.vertx.client.kafka",
                    "io.vertx.core",
                    "org.apache.kafka.client",
                    "org.eclipse.microprofile.reactive-messaging.api",
                    "org.eclipse.microprofile.reactive-streams-operators.api",
                    "org.eclipse.microprofile.reactive-streams-operators.core",
                    "org.wildfly.reactive.messaging.common",
                    "org.wildfly.reactive.messaging.config",
                    "org.wildfly.reactive.messaging.kafka",
                    // injected by logging
                    "org.apache.logging.log4j.api",
                    // injected by logging
                    "org.jboss.logmanager.log4j2",
//                    "org.jboss.as.product:wildfly-web",
                    // injected by ee
                    "javax.json.bind.api",
                    // injected by jpa
                    "org.hibernate.search.orm",
                    "org.hibernate.search.backend.elasticsearch",
                    "org.hibernate.search.backend.lucene",
                    // Used by the hibernate search that's injected by jpa
                    "org.elasticsearch.client.rest-client",
                    "com.google.code.gson",
                    "com.carrotsearch.hppc",
                    "org.apache.lucene",
                    "org.apache.avro",
                    // Brought by galleon ServerRootResourceDefinition
                    "wildflyee.api"
                    );
        }
    }

    public static String root;

    @BeforeClass
    public static void setUp() {
        root = System.getProperty("layers.install.root");
    }

    @AfterClass
    public static void cleanUp() {
        Boolean delete = Boolean.getBoolean("layers.delete.installations");
        if(delete) {
            File[] installations = new File(root).listFiles(File::isDirectory);
            for(File f : installations) {
                LayersTest.recursiveDelete(f.toPath());
            }
        }
    }

    @Test
    public void test() throws Exception {
        LayersTest.test(root, new HashSet<>(Arrays.asList(NOT_REFERENCED)),
                new HashSet<>(Arrays.asList(NOT_USED)));
    }

    @Test
    public void checkBannedModules() throws Exception {
        final HashMap<String, String> results = LayersTest.checkBannedModules(root, BANNED_MODULES_CONF);
        Assert.assertTrue("The following banned modules were provisioned " + results.toString(), results.isEmpty());
    }
}
