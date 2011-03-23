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
    public static final int STRUCTURE_MOUNT                             = 0x0000;
    public static final int STRUCTURE_MANIFEST                          = 0x0100;
    public static final int STRUCTURE_OSGI_MANIFEST                     = 0x0200;
    public static final int STRUCTURE_RAR                               = 0x0300;
    public static final int STRUCTURE_WAR_DEPLOYMENT_INIT               = 0x0400;
    public static final int STRUCTURE_WAR                               = 0x0500;
    public static final int STRUCTURE_EAR_DEPLOYMENT_INIT               = 0x0600;
    public static final int STRUCTURE_EAR_APP_XML_PARSE                 = 0x0700;
    public static final int STRUCTURE_EAR_JBOSS_APP_XML_PARSE           = 0x0800;
    public static final int STRUCTURE_EAR                               = 0x0900;
    public static final int STRUCTURE_SERVICE_MODULE_LOADER             = 0x0A00;
    public static final int STRUCTURE_ANNOTATION_INDEX                  = 0x0B00;
    public static final int STRUCTURE_EJB_JAR_IN_EAR                    = 0x0C00;
    public static final int STRUCTURE_MANAGED_BEAN_JAR_IN_EAR           = 0x0C01;
    public static final int STRUCTURE_SAR_SUB_DEPLOY_CHECK              = 0x0D00;
    public static final int STRUCTURE_ADDITIONAL_MANIFEST               = 0x0E00;
    public static final int STRUCTURE_SUB_DEPLOYMENT                    = 0x0F00;
    public static final int STRUCTURE_MODULE_IDENTIFIERS                = 0x1000;
    public static final int STRUCTURE_EE_MODULE_INIT                    = 0x1100;

    // PARSE
    public static final int PARSE_EE_MODULE_NAME                        = 0x0100;
    public static final int PARSE_STRUCTURE_DESCRIPTOR                  = 0x0200;
    public static final int PARSE_COMPOSITE_ANNOTATION_INDEX            = 0x0300;
    public static final int PARSE_EAR_LIB_CLASS_PATH                    = 0x0400;
    public static final int PARSE_ADDITIONAL_MODULES                    = 0x0500;
    public static final int PARSE_CLASS_PATH                            = 0x0600;
    public static final int PARSE_EXTENSION_LIST                        = 0x0700;
    public static final int PARSE_EXTENSION_NAME                        = 0x0800;
    public static final int PARSE_OSGI_BUNDLE_INFO                      = 0x0900;
    public static final int PARSE_OSGI_PROPERTIES                       = 0x0A00;
    // create and attach EJB metadata for EJB deployments
    public static final int PARSE_EJB_DEPLOYMENT                        = 0x0B00;
    // create and attach the component description out of EJB annotations
    public static final int PARSE_EJB_ANNOTATION                        = 0x0C00;
    public static final int PARSE_MESSAGE_DRIVEN_ANNOTATION             = 0x0C01;
    public static final int PARSE_EJB_TRANSACTION_MANAGEMENT            = 0x0C02;
    public static final int PARSE_EJB_LOCAL_VIEW_ANNOTATION             = 0x0C03;
    public static final int PARSE_EJB_NO_INTERFACE_VIEW_ANNOTATION      = 0x0C04;
    public static final int PARSE_EJB_STARTUP_ANNOTATION                = 0x0C05;
    public static final int PARSE_EJB_CONCURRENCY_MANAGEMENT_ANNOTATION = 0x0C06;
    // should be after ConcurrencyManagement annotation processor
    public static final int PARSE_EJB_LOCK_ANNOTATION                   = 0x0C07;
    // should be after ConcurrencyManagement annotation processor
    public static final int PARSE_EJB_ACCESS_TIMEOUT_ANNOTATION         = 0x0C08;
    public static final int PARSE_EJB_INJECTION_ANNOTATION              = 0x0C09;
    // should be after all views are known
    public static final int PARSE_EJB_TRANSACTION_ATTR_ANNOTATION       = 0x0C0A;
    public static final int PARSE_EJB_RESOURCE_ADAPTER_ANNOTATION       = 0x0C0B;
    public static final int PARSE_WEB_DEPLOYMENT                        = 0x0D00;
    public static final int PARSE_WEB_DEPLOYMENT_FRAGMENT               = 0x0E00;
    public static final int PARSE_ANNOTATION_WAR                        = 0x0F00;
    public static final int PARSE_JBOSS_WEB_DEPLOYMENT                  = 0x1000;
    public static final int PARSE_TLD_DEPLOYMENT                        = 0x1100;
    public static final int PARSE_RA_DEPLOYMENT                         = 0x1200;
    public static final int PARSE_SERVICE_LOADER_DEPLOYMENT             = 0x1300;
    public static final int PARSE_SERVICE_DEPLOYMENT                    = 0x1400;
    public static final int PARSE_MC_BEAN_DEPLOYMENT                    = 0x1500;
    public static final int PARSE_IRON_JACAMAR_DEPLOYMENT               = 0x1600;
    public static final int PARSE_RESOURCE_ADAPTERS                     = 0x1700;
    public static final int PARSE_DATA_SOURCES                          = 0x1800;
    public static final int PARSE_ARQUILLIAN_RUNWITH                    = 0x1900;
    public static final int PARSE_MANAGED_BEAN_ANNOTATION               = 0x1A00;
    public static final int PARSE_JAXRS_ANNOTATIONS                     = 0x1B00;
    public static final int PARSE_WELD_DEPLOYMENT                       = 0x1C00;
    public static final int PARSE_WEBSERVICES_XML                       = 0x1D00;
    public static final int PARSE_DATA_SOURCE_DEFINITION                = 0x1E00;
    public static final int PARSE_EJB_CONTEXT_BINDING                   = 0x1F00;
    public static final int PARSE_PERSISTENCE_UNIT                      = 0x2000;
    public static final int PARSE_WEB_COMPONENTS                        = 0x2010;
    public static final int PARSE_PERSISTENCE_ANNOTATION                = 0x2100;
    public static final int PARSE_BEAN_INTERCEPTOR_ANNOTATION           = 0x2600;
    public static final int PARSE_LIEFCYCLE_ANNOTATION                  = 0x2700;
    public static final int PARSE_AROUNDINVOKE_ANNOTATION               = 0x2800;
    public static final int PARSE_RESOURCE_INJECTION_ANNOTATION         = 0x2900;
    public static final int PARSE_WEB_SERVICE_INJECTION_ANNOTATION      = 0x2B00;

    // DEPENDENCIES
    public static final int DEPENDENCIES_MODULE                         = 0x100;
    public static final int DEPENDENCIES_DS                             = 0x200;
    public static final int DEPENDENCIES_RAR_CONFIG                     = 0x300;
    public static final int DEPENDENCIES_MANAGED_BEAN                   = 0x400;
    public static final int DEPENDENCIES_SAR_MODULE                     = 0x500;
    public static final int DEPENDENCIES_WAR_MODULE                     = 0x600;
    public static final int DEPENDENCIES_ARQUILLIAN                     = 0x700;
    public static final int DEPENDENCIES_CLASS_PATH                     = 0x800;
    public static final int DEPENDENCIES_EXTENSION_LIST                 = 0x900;
    public static final int DEPENDENCIES_WELD                           = 0xA00;
    public static final int DEPENDENCIES_NAMING                         = 0xB00;
    public static final int DEPENDENCIES_WS                             = 0xC00;
    public static final int DEPENDENCIES_JAXRS                          = 0xD00;
    public static final int DEPENDENCIES_SUB_DEPLOYMENTS                = 0xE00;
    // Sets up appropriate module dependencies for EJB deployments
    public static final int DEPENDENCIES_EJB                            = 0xF00;
    public static final int DEPENDENCIES_JPA                            = 0x1000;

    // CONFIGURE_MODULE
    public static final int CONFIGURE_MODULE_SPEC                       = 0x100;

    // POST_MODULE
    public static final int POST_MODULE_WELD_WEB_INTEGRATION            = 0x0100;
    public static final int POST_MODULE_INSTALL_EXTENSION               = 0x0200;
    public static final int POST_MODULE_VALIDATOR_FACTORY               = 0x0300;
    public static final int POST_MODULE_EAR_DEPENDENCY                  = 0x0400;
    public static final int POST_MODULE_WELD_BEAN_ARCHIVE               = 0x0500;
    public static final int POST_MODULE_WELD_PORTABLE_EXTENSIONS        = 0x0600;
    public static final int POST_MODULE_EJB_JNDI_BINDINGS               = 0x0700;

    public static final int POST_INITIALIZE_IN_ORDER                    = 0x0800;


    // INSTALL
    public static final int INSTALL_EAR_AGGREGATE_COMPONENT_INDEX       = 0x0000;
    public static final int INSTALL_REFLECTION_INDEX                    = 0x0100;
    public static final int INSTALL_APP_CONTEXT                         = 0x0200;
    public static final int INSTALL_MODULE_CONTEXT                      = 0x0300;
    public static final int INSTALL_SERVICE_ACTIVATOR                   = 0x0400;
    public static final int INSTALL_OSGI_DEPLOYMENT                     = 0x0500;
    public static final int INSTALL_WAR_METADATA                        = 0x0600;
    public static final int INSTALL_RA_DEPLOYMENT                       = 0x0700;
    public static final int INSTALL_SERVICE_DEPLOYMENT                  = 0x0800;
    public static final int INSTALL_MC_BEAN_DEPLOYMENT                  = 0x0900;
    public static final int INSTALL_RA_XML_DEPLOYMENT                   = 0x0A00;
    public static final int INSTALL_WELD_DEPLOYMENT                     = 0x0B00;
    public static final int INSTALL_WELD_BEAN_MANAGER                   = 0x0C00;
    public static final int INSTALL_EE_COMP_LAZY_BINDING_SOURCE_HANDLER = 0x0D00;
    public static final int INSTALL_WS_LAZY_BINDING_SOURCE_HANDLER      = 0x0E00;
    public static final int INSTALL_EE_COMPONENT                        = 0x0F00;
    public static final int INSTALL_SERVLET_INIT_DEPLOYMENT             = 0x1000;
    public static final int INSTALL_JAXRS_SCANNING                      = 0x1100;
    public static final int INSTALL_JAXRS_COMPONENT                     = 0x1200;
    public static final int INSTALL_JAXRS_DEPLOYMENT                    = 0x1300;
    public static final int INSTALL_JSF_ANNOTATIONS                     = 0x1400;
    public static final int INSTALL_WAR_DEPLOYMENT                      = 0x1500;
    public static final int INSTALL_ARQUILLIAN_DEPLOYMENT               = 0x1600;
    public static final int INSTALL_JDBC_DRIVER                         = 0x1700;
    public static final int INSTALL_TRANSACTION_BINDINGS                = 0x1800;
    public static final int INSTALL_PERSISTENTUNIT                      = 0x1900;

    // CLEANUP
    public static final int CLEANUP_REFLECTION_INDEX                    = 0x100;
}
