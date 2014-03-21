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

import java.util.Properties;


public enum ModelTestControllerVersion {
    //AS releases
    MASTER (CurrentVersion.VERSION, false, null),
    V7_1_2_FINAL ("7.1.2.Final", false, "7.1.2"),
    V7_1_3_FINAL ("7.1.3.Final", false, "7.1.2"),
    V7_2_0_FINAL ("7.2.0.Final", false, "7.2.0"),

    //EAP releases
    EAP_6_0_0 ("7.1.2.Final-redhat-1", true, "7.1.2"),
    EAP_6_0_1 ("7.1.3.Final-redhat-4", true, "7.1.2"),
    EAP_6_1_0 ("7.2.0.Final-redhat-8", true, "7.2.0"),
    EAP_6_1_1 ("7.2.1.Final-redhat-10", true, "7.2.0"),
    EAP_6_2_0 ("7.3.0.Final-redhat-14", true, null)
    ;

    private final String mavenGavVersion;
    private final String testControllerVersion;
    private final boolean eap;
    private ModelTestControllerVersion(String mavenGavVersion, boolean eap, String testControllerVersion) {
        this.mavenGavVersion = mavenGavVersion;
        this.testControllerVersion = testControllerVersion;
        this.eap = eap;
    }

    public String getMavenGavVersion() {
        return mavenGavVersion;
    }

    public String getTestControllerVersion() {
        return testControllerVersion;
    }

    public boolean isEap() {
        return eap;
    }

    public interface CurrentVersion {
        String VERSION = VersionLocator.VERSION;
    }

    static final class VersionLocator {
        private static String VERSION;

        static {
            try {
                Properties props = new Properties();
                props.load(ModelTestControllerVersion.class.getResourceAsStream("version.properties"));
                VERSION = props.getProperty("as.version");
            } catch (Exception e) {
                VERSION = "8.0.0.Beta2-SNAPSHOT";
                e.printStackTrace();
            }
        }

        static String getCurrentVersion() {
            return VERSION;
        }
    }}
