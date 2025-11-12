/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Package containing tests pertaining to SSL/TLS-secured transport configuration in JGroups.
 *
 * For debugging TLS, append this to ${server.jvm.args} in testsuite/integration/clustering/pom.xml:
 *
 * -Djavax.net.debug=ssl:handshake:verbose:keymanager:trustmanager
 *
 * Inspecting generated key stores by the server setup tasks:
 *
 * keytool -list -v -keystore target/server.keystore.pkcs12
 * keytool -list -v -keystore target/server.truststore.pkcs12
 *
 * @author Radoslav Husar
 */
package org.jboss.as.test.clustering.cluster.jgroups.tls;
