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

package org.jboss.as.server.deployment;

/**
 * An enumeration of the phases of a deployment unit's processing cycle.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum Phase {

    /* == TEMPLATE ==
     * Upon entry, this phase performs the following actions:
     * <ul>
     * <li></li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following phase attachments:
     * <ul>
     * <li>{@link Attachments#BLAH} - description here</li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following deployment unit attachments, in addition to those defined
     * for the previous phase:
     * <ul>
     * <li>{@link Attachments#BLAH} - description here</li>
     * </ul>
     * <p>
     * In this phase, these phase attachments may be modified:
     * <ul>
     * <li>{@link Attachments#BLAH} - description here</li>
     * </ul>
     * <p>
     */

    /**
     * This phase creates the initial root structure.  Depending on the service for this phase will ensure that the
     * deployment unit's initial root structure is available and accessible.
     * <p>
     * Upon entry, this phase performs the following actions:
     * <ul>
     * <li>The primary deployment root is mounted (during {@link #STRUCTURE_MOUNT})</li>
     * <li>Other internal deployment roots are mounted (during {@link #STRUCTURE_NESTED_JAR})</li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following phase attachments:
     * <ul>
     * <li><i>N/A</i></li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following deployment unit attachments:
     * <ul>
     * <li>{@link Attachments#DEPLOYMENT_ROOT} - the mounted deployment root for this deployment unit</li>
     * </ul>
     * <p>
     * In this phase, these phase attachments may be modified:
     * <ul>
     * </ul>
     * <p>
     */
    STRUCTURE(null),
    /**
     * This phase assembles information from the root structure to prepare for adding and processing additional external
     * structure, such as from class path entries and other similar mechanisms.
     * <p>
     * Upon entry, this phase performs the following actions:
     * <ul>
     * <li>The root content's MANIFEST is read and made available during {@link #PARSE_MANIFEST}.</li>
     * <li>The annotation index for the root structure is calculated during {@link #STRUCTURE_ANNOTATION_INDEX}.</li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following phase attachments:
     * <ul>
     * <li>{@link Attachments#MANIFEST} - the parsed manifest of the root structure</li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following deployment unit attachments, in addition to those defined
     * for the previous phase:
     * <ul>
     * <li><i>N/A</i></li>
     * </ul>
     * <p>
     * In this phase, these phase attachments may be modified:
     * <ul>
     * <li>{@link Attachments#CLASS_PATH_ENTRIES} - class path entries found in the manifest and elsewhere.</li>
     * <li>{@link Attachments#EXTENSION_LIST_ENTRIES} - extension-list entries found in the manifest and elsewhere.</li>
     * </ul>
     * <p>
     */
    PARSE(null),
    /**
     * In this phase, the full structure of the deployment unit is made available and module dependencies may be assembled.
     * <p>
     * Upon entry, this phase performs the following actions:
     * <ul>
     * <li>Any additional external structure is mounted during {@link #XXX}</li>
     * <li></li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following phase attachments:
     * <ul>
     * <li>{@link Attachments#BLAH} - description here</li>
     * </ul>
     * <p>
     * Processors in this phase have access to the following deployment unit attachments, in addition to those defined
     * for the previous phase:
     * <ul>
     * <li>{@link Attachments#BLAH} - description here</li>
     * </ul>
     * <p>
     * In this phase, these phase attachments may be modified:
     * <ul>
     * <li>{@link Attachments#BLAH} - description here</li>
     * </ul>
     * <p>

     */
    DEPENDENCIES(null),
    CONFIGURE_MODULE(null),
    MODULARIZE(null),
    POST_MODULE(null),
    INSTALL(null),
    CLEANUP(null),
    ;

    /**
     * This is the key for the attachment to use as the phase's "value".  The attachment is taken from
     * the deployment unit.  If a phase doesn't have a single defining "value", {@code null} is specified.
     */
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
    public static final int STRUCTURE_RAR                               = 0x100;
    public static final int STRUCTURE_WAR_DEPLOYMENT_INIT               = 0x200;
    public static final int STRUCTURE_WAR                               = 0x300;
    public static final int STRUCTURE_EAR_DEPLOYMENT_INIT               = 0x400;
    public static final int STRUCTURE_EAR_APP_XML_PARSE                 = 0x500;
    public static final int STRUCTURE_EAR                               = 0x600;
    public static final int STRUCTURE_DEPLOYMENT_MODULE_LOADER          = 0x700;
    public static final int STRUCTURE_ANNOTATION_INDEX                  = 0x800;
    public static final int STRUCTURE_MANAGED_BEAN_SUB_DEPLOY_CHECK     = 0x900;
    public static final int STRUCTURE_SAR_SUB_DEPLOY_CHECK              = 0xA00;
    public static final int STRUCTURE_SUB_DEPLOYMENT                    = 0xB00;

    // PARSE
    public static final int PARSE_MANIFEST                              = 0x0100;
    public static final int PARSE_CLASS_PATH                            = 0x0200;
    public static final int PARSE_EXTENSION_LIST                        = 0x0300;
    public static final int PARSE_OSGI_MANIFEST                         = 0x0400;
    public static final int PARSE_OSGI_BUNDLE_INFO                      = 0x0500;
    public static final int PARSE_OSGI_XSERVICE_PROPERTIES              = 0x0550;
    public static final int PARSE_WEB_DEPLOYMENT                        = 0x0600;
    public static final int PARSE_WEB_DEPLOYMENT_FRAGMENT               = 0x0700;
    public static final int PARSE_JBOSS_WEB_DEPLOYMENT                  = 0x0800;
    public static final int PARSE_TLD_DEPLOYMENT                        = 0x0900;
    public static final int PARSE_RA_DEPLOYMENT                         = 0x0A00;
    public static final int PARSE_SERVICE_DEPLOYMENT                    = 0x0B00;
    public static final int PARSE_MC_BEAN_DEPLOYMENT                    = 0x0C00;
    public static final int PARSE_IRON_JACAMAR_DEPLOYMENT               = 0x0D00;
    public static final int PARSE_RESOURCE_ADAPTERS                     = 0x0E00;
    public static final int PARSE_DATA_SOURCES                          = 0x0F00;
    public static final int PARSE_ARQUILLIAN_RUNWITH                    = 0x1000;

    // DEPENDENCIES
    public static final int DEPENDENCIES_MODULE                         = 0x100;
    public static final int DEPENDENCIES_DS                             = 0x200;
    public static final int DEPENDENCIES_RAR_CONFIG                     = 0x300;
    public static final int DEPENDENCIES_MANAGED_BEAN                   = 0x400;
    public static final int DEPENDENCIES_SAR_MODULE                     = 0x500;
    public static final int DEPENDENCIES_WAR_MODULE                     = 0x600;
    public static final int DEPENDENCIES_ARQUILLIAN                     = 0x700;

    // CONFIGURE_MODULE
    public static final int CONFIGURE_MODULE_WAR                        = 0x100;
    public static final int CONFIGURE_MODULE_SPEC                       = 0x200;

    // MODULARIZE
    public static final int MODULARIZE_DEPLOYMENT                       = 0x100;

    // POST_MODULE
    public static final int POST_MODULE_ANNOTATION_MANAGED_BEAN         = 0x100;
    public static final int POST_MODULE_ANNOTATION_WAR                  = 0x200;
    public static final int POST_MODULE_ANNOTATION_ARQUILLIAN_JUNIT     = 0x300;

    // INSTALL
    public static final int INSTALL_REFLECTION_INDEX                    = 0x0100;
    public static final int INSTALL_APP_CONTEXT                         = 0x0200;
    public static final int INSTALL_MODULE_CONTEXT                      = 0x0300;
    public static final int INSTALL_SERVICE_ACTIVATOR                   = 0x0400;
    public static final int INSTALL_OSGI_ATTACHMENTS                    = 0x0500;
    public static final int INSTALL_WAR_METADATA                        = 0x0600;
    public static final int INSTALL_RA_DEPLOYMENT                       = 0x0700;
    public static final int INSTALL_SERVICE_DEPLOYMENT                  = 0x0800;
    public static final int INSTALL_MC_BEAN_DEPLOYMENT                  = 0x0900;
    public static final int INSTALL_RA_XML_DEPLOYMENT                   = 0x0A00;
    public static final int INSTALL_DS_DEPLOYMENT                       = 0x0B00;
    public static final int INSTALL_MANAGED_BEAN_DEPLOYMENT             = 0x0C00;
    public static final int INSTALL_BEAN_CONTAINER                      = 0x0D00;
    public static final int INSTALL_SERVLET_INIT_DEPLOYMENT             = 0x0E00;
    public static final int INSTALL_WAR_DEPLOYMENT                      = 0x0F00;
    public static final int INSTALL_ARQUILLIAN_DEPLOYMENT               = 0x1000;

    // CLEANUP
    public static final int CLEANUP_REFLECTION_INDEX                    = 0x100;
}
