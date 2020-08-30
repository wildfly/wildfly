/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
        Server.createTcpServer("-tcpPort", DB_PORT, "-tcpAllowOthers", "-baseDir", "./target/h2").start();
    }

    public static void stopH2() throws SQLException {
        Server.shutdownTcpServer("tcp://localhost:" + DB_PORT, "", true, true);
    }

    private ClusterDatabaseTestUtil() {
        // Utility class
    }
}
