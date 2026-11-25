/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Package containing tests pertaining to SSL/TLS-secured transport configuration in JGroups.
 *
 * <h2>Test classes</h2>
 *
 * <h3>Positive tests (nodes must form a cluster)</h3>
 * <ul>
 *     <li>{@link org.jboss.as.test.clustering.cluster.jgroups.tls.TLSWebFailoverTestCase} &ndash;
 *         web session failover over TLS-secured TCP transport with 3 nodes</li>
 * </ul>
 *
 * <h3>Negative tests (nodes must NOT form a cluster)</h3>
 * <ul>
 *     <li>{@link org.jboss.as.test.clustering.cluster.jgroups.tls.UntrustedCertTLSCommandDispatcherTestCase} &ndash;
 *         TLS-secured TCP transport where each node has its own untrusted certificate; nodes must reject each other</li>
 * </ul>
 *
 * <h2>Expected log warnings and errors</h2>
 *
 * The following log messages are expected during test execution and do not indicate a problem:
 * <ul>
 *     <li>{@code WFLYELY00023: KeyStore file '...server-node-X.keystore.pkcs12' does not exist. Used blank.} &ndash;
 *         Elytron key-store resources are added before {@code generate-key-pair} populates them;
 *         the warning is emitted during first resource creation before the key is generated and stored.</li>
 *     <li>{@code JGRP000006: failed accepting connection from peer SSLSocket[...]: Socket is closed} &ndash;
 *         expected in negative/untrusted-certificate tests where nodes with non-preshared keys correctly reject each other.</li>
 *     <li>{@code failed sending graceful close: closing inbound before receiving peer's close_notify} &ndash;
 *         occurs during TLS connection teardown when a node is stopped or undeployed during failover testing;
 *         the peer closes before the TLS {@code close_notify} is received.</li>
 * </ul>
 *
 * <h2>Debugging</h2>
 *
 * For debugging TLS, append this to {@code ${server.jvm.args}} in {@code testsuite/integration/clustering/pom.xml}:
 * <pre>-Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager</pre>
 *
 * Inspecting generated key stores by the server setup tasks:
 * <pre>
 * keytool -list -v -keystore target/server-node-1.keystore.pkcs12
 * keytool -list -v -keystore target/server-node-2.keystore.pkcs12
 * keytool -list -v -keystore target/server.truststore.pkcs12
 * </pre>
 * Password is empty string.
 * Make sure to disable ServerSetupTask teardown after running the tests before inspecting the keystores manually.
 *
 * @author Radoslav Husar
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;
