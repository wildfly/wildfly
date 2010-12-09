/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.deployment;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Phase {

    STRUCTURE(null),
    VALIDATE(null),
    PARSE(null),
    DEPENDENCIES(null),
    MODULARIZE(null),
    POST_MODULE(null),
    INSTALL(null),
    CLEANUP(null),
    ;
    private final AttachmentKey<?> phaseKey;

    private Phase(final AttachmentKey<?> key) {
        phaseKey = key;
    }

    /**
     * Get the next phase, or {@code null} if none.
     *
     * @return the next phase, or {@code null} if there is none
     */
    public Phase next() {
        final int ord = ordinal() + 1;
        final Phase[] phases = Phase.values();
        return ord == phases.length ? null : phases[ord];
    }

    /**
     * Get the attachment key of the {@code DeploymentUnit} attachment that represents the result value
     * of this phase.
     *
     * @return the key
     */
    public AttachmentKey<?> getPhaseKey() {
        return phaseKey;
    }

    // STRUCTURE
    public static final int STRUCTURE_MOUNT                             = 0x000;
    public static final int STRUCTURE_NESTED_JAR                        = 0x100;
    public static final int STRUCTURE_NESTED_JAR_RA                     = 0x200;
    public static final int STRUCTURE_WAR_DEPLOYMENT_INIT               = 0x300;
    public static final int STRUCTURE_WAR_DEPLOYMENT                    = 0x400;

    // VALIDATE
    // (empty)

    // PARSE
    public static final int PARSE_MANIFEST                              = 0x0100;
    public static final int PARSE_MANIFEST_OSGI                         = 0x0200;
    public static final int PARSE_OSGI_BUNDLE_INFO                      = 0x0300;
    public static final int PARSE_ANNOTATION_INDEX                      = 0x0400;
    public static final int PARSE_ANNOTATION_INDEX_WAR                  = 0x0500;
    public static final int PARSE_DEPENDENCY_SERVICE_ACTIVATION         = 0x0600;
    public static final int PARSE_WEB_DEPLOYMENT                        = 0x0700;
    public static final int PARSE_WEB_DEPLOYMENT_FRAGMENT               = 0x0800;
    public static final int PARSE_JBOSS_WEB_DEPLOYMENT                  = 0x0900;
    public static final int PARSE_TLD_DEPLOYMENT                        = 0x0A00;
    public static final int PARSE_RA_DEPLOYMENT                         = 0x0B00;
    public static final int PARSE_SERVICE_DEPLOYMENT                    = 0x0C00;
    public static final int PARSE_MC_BEAN_DEPLOYMENT                    = 0x0D00;
    public static final int PARSE_IRON_JACAMAR_DEPLOYMENT               = 0x0E00;
    public static final int PARSE_RESOURCE_ADAPTERS                     = 0x0F00;
    public static final int PARSE_DATA_SOURCES                          = 0x1000;
    public static final int PARSE_ARQUILLIAN_RUNWITH                    = 0x1100;

    // DEPENDENCIES
    public static final int DEPENDENCIES_MODULE                         = 0x100;
    public static final int DEPENDENCIES_DS                             = 0x200;
    public static final int DEPENDENCIES_RAR_CONFIG                     = 0x300;
    public static final int DEPENDENCIES_MANAGED_BEAN                   = 0x400;
    public static final int DEPENDENCIES_SAR_MODULE                     = 0x500;
    public static final int DEPENDENCIES_WAR_MODULE                     = 0x600;
    public static final int DEPENDENCIES_ARQUILLIAN                     = 0x700;

    // MODULARIZE
    public static final int MODULARIZE_WAR                              = 0x100;
    public static final int MODULARIZE_CONFIG                           = 0x200;
    public static final int MODULARIZE_DEPLOYMENT_MODULE_LOADER         = 0x300;
    public static final int MODULARIZE_DEPLOYMENT                       = 0x400;

    // POST_MODULE
    public static final int POST_MODULE_ANNOTATION_MANAGED_BEAN         = 0x100;
    public static final int POST_MODULE_ANNOTATION_WAR                  = 0x200;
    public static final int POST_MODULE_ANNOTATION_ARQUILLIAN_JUNIT     = 0x300;

    // INSTALL
    public static final int INSTALL_MODULE_CONTEXT                      = 0x100;
    public static final int INSTALL_SERVICE_ACTIVATOR                   = 0x200;
    public static final int INSTALL_OSGI_ATTACHMENTS                    = 0x300;
    public static final int INSTALL_WAR_METADATA                        = 0x400;
    public static final int INSTALL_RA_DEPLOYMENT                       = 0x500;
    public static final int INSTALL_SERVICE_DEPLOYMENT                  = 0x600;
    public static final int INSTALL_MC_BEAN_DEPLOYMENT                  = 0x700;
    public static final int INSTALL_RA_XML_DEPLOYMENT                   = 0x800;
    public static final int INSTALL_DS_DEPLOYMENT                       = 0x900;
    public static final int INSTALL_MANAGED_BEAN_DEPLOYMENT             = 0xA00;
    public static final int INSTALL_SERVLET_INIT_DEPLOYMENT             = 0xB00;
    public static final int INSTALL_WAR_DEPLOYMENT                      = 0xC00;
    public static final int INSTALL_ARQUILLIAN_DEPLOYMENT               = 0xD00;

    // CLEANUP
    // (none)
}
