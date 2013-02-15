/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.model.test;



public enum ModelTestControllerVersion {
    MASTER (CurrentVersion.VERSION, null),
    V7_1_2_FINAL ("7.1.2.Final", "7.1.2"),
    V7_1_3_FINAL ("7.1.3.Final", "7.1.2");

    String mavenGavVersion;
    String testControllerVersion;
    private ModelTestControllerVersion(String mavenGavVersion, String testControllerVersion) {
        this.mavenGavVersion = mavenGavVersion;
        this.testControllerVersion = testControllerVersion;
    }

    public String getMavenGavVersion() {
        return mavenGavVersion;
    }

    public String getTestControllerVersion() {
        return testControllerVersion;
    }

    public interface CurrentVersion {
        String VERSION = VersionLocator.VERSION;
    }

    private static final class VersionLocator {
        static String VERSION = "${project.version}"; //is going to be replaced by maven during build

        static {
            if (VERSION.contains("${")) {
                VERSION = "8.0.0.Alpha1-SNAPSHOT"; //to make it work from IDE
            }
        }
    }
}