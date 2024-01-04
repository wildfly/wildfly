/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering;

import static org.jboss.as.test.clustering.cluster.AbstractClusteringTestCase.DB_PORT;

import java.sql.SQLException;

import org.h2.tools.Server;

/**
 * Simple test utility to start/stop an external H2 database, e.g. to be used in clustering tests which use a shared database.
 * Database files are stored in ${project.build.directory}/target/h2 directory.
 *
 * @author Radoslav Husar
 */
public class ClusterDatabaseTestUtil {

    public static void startH2() throws SQLException {
        Server.createTcpServer("-tcpPort", DB_PORT, "-tcpAllowOthers", "-ifNotExists", "-tcpPassword", "sa", "-baseDir", "./target/h2").start();
    }

    public static void stopH2() throws SQLException {
        Server.shutdownTcpServer("tcp://localhost:" + DB_PORT, "sa", true, true);
    }

    private ClusterDatabaseTestUtil() {
        // Utility class
    }
}
