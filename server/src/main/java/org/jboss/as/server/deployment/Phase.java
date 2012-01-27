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
    public static final int STRUCTURE_EXPLODED_MOUNT                    = 0x0200;
    public static final int STRUCTURE_MOUNT                             = 0x0300;
    public static final int STRUCTURE_MANIFEST                          = 0x0400;
    // must be before osgi
    public static final int STRUCTURE_JDBC_DRIVER                       = 0x0500;
    public static final int STRUCTURE_OSGI_MANIFEST                     = 0x0600;
    public static final int STRUCTURE_RAR                               = 0x0700;
    public static final int STRUCTURE_WAR_DEPLOYMENT_INIT               = 0x0800;
    public static final int STRUCTURE_WAR                               = 0x0900;
    public static final int STRUCTURE_EAR_DEPLOYMENT_INIT               = 0x0A00;
    public static final int STRUCTURE_EAR_APP_XML_PARSE                 = 0x0B00;
    public static final int STRUCTURE_JBOSS_EJB_CLIENT_XML_PARSE        = 0x0C00;
    public static final int STRUCTURE_EJB_EAR_APPLICATION_NAME          = 0x0D00;
    public static final int STRUCTURE_EAR                               = 0x0E00;
    public static final int STRUCTURE_APP_CLIENT                        = 0x0F00;
    public static final int STRUCTURE_SERVICE_MODULE_LOADER             = 0x1000;
    public static final int STRUCTURE_ANNOTATION_INDEX                  = 0x1100;
    public static final int STRUCTURE_EJB_JAR_IN_EAR                    = 0x1200;
    public static final int STRUCTURE_APPLICATION_CLIENT_IN_EAR         = 0x1300;
    public static final int STRUCTURE_MANAGED_BEAN_JAR_IN_EAR           = 0x1400;
    public static final int STRUCTURE_SAR_SUB_DEPLOY_CHECK              = 0x1500;
    public static final int STRUCTURE_ADDITIONAL_MANIFEST               = 0x1600;
    public static final int STRUCTURE_SUB_DEPLOYMENT                    = 0x1700;
    public static final int STRUCTURE_JBOSS_DEPLOYMENT_STRUCTURE_DESCRIPTOR = 0x1800;
    public static final int STRUCTURE_CLASS_PATH                        = 0x1900;
    public static final int STRUCTURE_MODULE_IDENTIFIERS                = 0x1A00;
    public static final int STRUCTURE_EE_MODULE_INIT                    = 0x1B00;
    public static final int STRUCTURE_EE_RESOURCE_INJECTION_REGISTRY    = 0x1C00;

    // PARSE
    public static final int PARSE_EE_MODULE_NAME                        = 0x0100;
    public static final int PARSE_EAR_SUBDEPLOYMENTS_ISOLATION_DEFAULT  = 0x0200;
    public static final int PARSE_DEPENDENCIES_MANIFEST                 = 0x0300;
    public static final int PARSE_COMPOSITE_ANNOTATION_INDEX            = 0x0301;
    public static final int PARSE_EXTENSION_LIST                        = 0x0700;
    public static final int PARSE_EXTENSION_NAME                        = 0x0800;
    public static final int PARSE_OSGI_BUNDLE_INFO                      = 0x0900;
    public static final int PARSE_OSGI_XSERVICE_PROPERTIES              = 0x0A00;
    public static final int PARSE_OSGI_DEPLOYMENT                       = 0x0A80;
    public static final int PARSE_WEB_DEPLOYMENT                        = 0x0B00;
    public static final int PARSE_WEB_DEPLOYMENT_FRAGMENT               = 0x0C00;
    public static final int PARSE_JSF_VERSION                           = 0x0C50;
    public static final int PARSE_ANNOTATION_WAR                        = 0x0D00;
    public static final int PARSE_ANNOTATION_EJB                        = 0x0D10;
    public static final int PARSE_JBOSS_WEB_DEPLOYMENT                  = 0x0E00;
    public static final int PARSE_TLD_DEPLOYMENT                        = 0x0F00;
    public static final int PARSE_EAR_CONTEXT_ROOT                      = 0x1000;
    // create and attach EJB metadata for EJB deployments
    public static final int PARSE_EJB_DEPLOYMENT                        = 0x1100;
    public static final int PARSE_APP_CLIENT_XML                        = 0x1101;
    public static final int PARSE_EJB_CLIENT_METADATA                   = 0x1102;
    public static final int PARSE_SESSION_BEAN_CREATE_COMPONENT_DESCRIPTIONS     = 0x1150;
    public static final int PARSE_MDB_CREATE_COMPONENT_DESCRIPTIONS     = 0x1151;
    public static final int PARSE_ENTITY_BEAN_CREATE_COMPONENT_DESCRIPTIONS = 0x1152;
    public static final int PARSE_CMP_ENTITY_BEAN_CREATE_COMPONENT_DESCRIPTIONS = 0x1153;
    public static final int PARSE_EJB_SESSION_BEAN_DD                   = 0x1200;
    // create and attach the component description out of EJB annotations
    public static final int PARSE_EJB_APPLICATION_EXCEPTION_ANNOTATION  = 0x1901;
    public static final int PARSE_WEB_COMPONENTS                        = 0x1F00;
    public static final int PARSE_WEB_MERGE_METADATA                    = 0x2000;
    public static final int PARSE_WEBSERVICES_CONTEXT_INJECTION         = 0x2040;
    public static final int PARSE_WEBSERVICES_XML                       = 0x2050;
    public static final int PARSE_JBOSS_WEBSERVICES_XML                 = 0x2051;
    public static final int PARSE_JAXWS_EJB_INTEGRATION                 = 0x2052;
    public static final int PARSE_JAXRPC_POJO_INTEGRATION                = 0x2053;
    public static final int PARSE_JAXRPC_EJB_INTEGRATION                = 0x2054;
    public static final int PARSE_JAXWS_HANDLER_CHAIN_ANNOTATION        = 0x2055;
    public static final int PARSE_WS_JMS_INTEGRATION                    = 0x2056;
    public static final int PARSE_JAXWS_ENDPOINT_CREATE_COMPONENT_DESCRIPTIONS = 0x2057;
    public static final int PARSE_JAXWS_HANDLER_CREATE_COMPONENT_DESCRIPTIONS = 0x2058;
    public static final int PARSE_RA_DEPLOYMENT                         = 0x2100;
    public static final int PARSE_SERVICE_LOADER_DEPLOYMENT             = 0x2200;
    public static final int PARSE_SERVICE_DEPLOYMENT                    = 0x2300;
    public static final int PARSE_POJO_DEPLOYMENT                       = 0x2400;
    public static final int PARSE_IRON_JACAMAR_DEPLOYMENT               = 0x2500;
    public static final int PARSE_MANAGED_BEAN_ANNOTATION               = 0x2900;
    public static final int PARSE_EE_ANNOTATIONS                        = 0x2901;
    public static final int PARSE_JAXRS_ANNOTATIONS                     = 0x2A00;
    public static final int PARSE_WELD_DEPLOYMENT                       = 0x2B00;
    public static final int PARSE_WELD_WEB_INTEGRATION                  = 0x2B10;
    public static final int PARSE_DATA_SOURCE_DEFINITION_ANNOTATION     = 0x2D00;
    public static final int PARSE_EJB_CONTEXT_BINDING                   = 0x2E00;
    public static final int PARSE_EJB_TIMERSERVICE_BINDING              = 0x2E01;
    public static final int PARSE_PERSISTENCE_UNIT                      = 0x2F00;
    public static final int PARSE_PERSISTENCE_ANNOTATION                = 0x3000;
    public static final int PARSE_LIEFCYCLE_ANNOTATION                  = 0x3200;
    public static final int PARSE_PASSIVATION_ANNOTATION                = 0x3250;
    public static final int PARSE_AROUNDINVOKE_ANNOTATION               = 0x3300;
    public static final int PARSE_AROUNDTIMEOUT_ANNOTATION              = 0x3400;
    public static final int PARSE_TIMEOUT_ANNOTATION                    = 0x3401;
    public static final int PARSE_EJB_DD_INTERCEPTORS                   = 0x3500;
    public static final int PARSE_EJB_SECURITY_ROLE_REF_DD              = 0x3501;
    public static final int PARSE_EJB_ASSEMBLY_DESC_DD                  = 0x3600;
    public static final int PARSE_DISTINCT_NAME                         = 0x3601;
    // should be after all components are known
    public static final int PARSE_EJB_INJECTION_ANNOTATION              = 0x3700;
    public static final int PARSE_EJB_REMOTE_CLIENT_CONTEXT             = 0x3900;
    public static final int PARSE_JACORB                                = 0x3A00;
    public static final int PARSE_TRANSACTION_ROLLBACK_ACTION           = 0x3B00;
    public static final int PARSE_WEB_INITIALIZE_IN_ORDER               = 0x3C00;
    public static final int PARSE_EAR_MESSAGE_DESTINATIONS              = 0x3D00;
    public static final int PARSE_DSXML_DEPLOYMENT                      = 0x3E00;
    public static final int PARSE_MESSAGING_XML_RESOURCES               = 0x3F00;

    // DEPENDENCIES
    public static final int DEPENDENCIES_EJB                            = 0x0000;
    public static final int DEPENDENCIES_MODULE                         = 0x0100;
    public static final int DEPENDENCIES_RAR_CONFIG                     = 0x0300;
    public static final int DEPENDENCIES_MANAGED_BEAN                   = 0x0400;
    public static final int DEPENDENCIES_SAR_MODULE                     = 0x0500;
    public static final int DEPENDENCIES_WAR_MODULE                     = 0x0600;
    public static final int DEPENDENCIES_CLASS_PATH                     = 0x0800;
    public static final int DEPENDENCIES_EXTENSION_LIST                 = 0x0900;
    public static final int DEPENDENCIES_WELD                           = 0x0A00;
    public static final int DEPENDENCIES_SEAM                           = 0x0A01;
    public static final int DEPENDENCIES_WS                             = 0x0C00;
    public static final int DEPENDENCIES_SECURITY                       = 0x0C50;
    public static final int DEPENDENCIES_JAXRS                          = 0x0D00;
    public static final int DEPENDENCIES_SUB_DEPLOYMENTS                = 0x0E00;
    public static final int DEPENDENCIES_JPA                            = 0x1000;
    public static final int DEPENDENCIES_GLOBAL_MODULES                 = 0x1100;
    public static final int DEPENDENCIES_JDK                            = 0x1200;
    public static final int DEPENDENCIES_JACORB                         = 0x1300;
    public static final int DEPENDENCIES_CMP                            = 0x1500;
    public static final int DEPENDENCIES_JAXR                           = 0x1600;
    public static final int DEPENDENCIES_DRIVERS                        = 0x1700;
    //these must be last, and in this specific order
    public static final int DEPENDENCIES_APPLICATION_CLIENT             = 0x2000;
    public static final int DEPENDENCIES_VISIBLE_MODULES                = 0x2100;
    public static final int DEPENDENCIES_EE_CLASS_DESCRIPTIONS          = 0x2200;


    // CONFIGURE_MODULE
    public static final int CONFIGURE_MODULE_SPEC                       = 0x0100;


    // POST_MODULE
    public static final int POST_MODULE_INJECTION_ANNOTATION            = 0x0100;
    public static final int POST_MODULE_REFLECTION_INDEX                = 0x0200;
    public static final int POST_MODULE_TRANSFORMER                     = 0x0201;
    public static final int POST_MODULE_JSF_MANAGED_BEANS               = 0x0300;
    public static final int POST_MODULE_INTERCEPTOR_ANNOTATIONS         = 0x0301;
    public static final int POST_MODULE_EJB_BUSINESS_VIEW_ANNOTATION    = 0x0400;
    public static final int POST_MODULE_EJB_HOME_MERGE                  = 0x0401;
    public static final int POST_MODULE_EJB_DD_METHOD_RESOLUTION        = 0x0402;
    public static final int POST_MODULE_EJB_TIMER_METADATA_MERGE        = 0x0506;
    public static final int POST_MODULE_EJB_SLSB_POOL_NAME_MERGE        = 0x0507;
    public static final int POST_MODULE_EJB_MDB_POOL_NAME_MERGE         = 0x0508;
    public static final int POST_MODULE_EJB_ENTITY_POOL_NAME_MERGE      = 0x0509;
    public static final int POST_MODULE_EJB_DD_INTERCEPTORS             = 0x0600;
    public static final int POST_MODULE_EJB_TIMER_SERVICE               = 0x0601;
    public static final int POST_MODULE_EJB_TRANSACTION_MANAGEMENT      = 0x0602;
    public static final int POST_MODULE_EJB_TX_ATTR_MERGE               = 0x0603;
    public static final int POST_MODULE_EJB_CONCURRENCY_MANAGEMENT_MERGE= 0x0604;
    public static final int POST_MODULE_EJB_CONCURRENCY_MERGE           = 0x0605;
    public static final int POST_MODULE_EJB_RUN_AS_MERGE                = 0x0606;
    public static final int POST_MODULE_EJB_RESOURCE_ADAPTER_MERGE      = 0x0607;
    public static final int POST_MODULE_EJB_REMOVE_METHOD               = 0x0608;
    public static final int POST_MODULE_EJB_STARTUP_MERGE               = 0x0609;
    public static final int POST_MODULE_EJB_SECURITY_DOMAIN             = 0x060A;
    public static final int POST_MODULE_EJB_ROLES                       = 0x060B;
    public static final int POST_MODULE_METHOD_PERMISSIONS              = 0x060C;
    public static final int POST_MODULE_EJB_STATEFUL_TIMEOUT            = 0x060D;
    public static final int POST_MODULE_EJB_ASYNCHRONOUS_MERGE          = 0x060E;
    public static final int POST_MODULE_EJB_SESSION_SYNCHRONIZATION     = 0x060F;
    public static final int POST_MODULE_EJB_INIT_METHOD                 = 0x0610;
    public static final int POST_MODULE_EJB_SESSION_BEAN                = 0x0611;
    public static final int POST_MODULE_EJB_SECURITY_PRINCIPAL_ROLE_MAPPING_MERGE   = 0x0612;
    public static final int POST_MODULE_EJB_CACHE                       = 0x0614;
    public static final int POST_MODULE_EJB_CLUSTERED                   = 0x0615;
    public static final int POST_MODULE_WELD_COMPONENT_INTEGRATION      = 0x0800;
    public static final int POST_MODULE_INSTALL_EXTENSION               = 0x0A00;
    public static final int POST_MODULE_VALIDATOR_FACTORY               = 0x0B00;
    public static final int POST_MODULE_EAR_DEPENDENCY                  = 0x0C00;
    public static final int POST_MODULE_WELD_BEAN_ARCHIVE               = 0x0D00;
    public static final int POST_MODULE_WELD_PORTABLE_EXTENSIONS        = 0x0E00;
    // should come before ejb jndi bindings processor
    public static final int POST_MODULE_EJB_IMPLICIT_NO_INTERFACE_VIEW  = 0x1000;
    public static final int POST_MODULE_EJB_JNDI_BINDINGS               = 0x1100;
    public static final int POST_MODULE_EJB_APPLICATION_EXCEPTIONS      = 0x1200;
    public static final int POST_INITIALIZE_IN_ORDER                    = 0x1300;
    public static final int POST_MODULE_ENV_ENTRY                       = 0x1400;
    public static final int POST_MODULE_EJB_REF                         = 0x1500;
    public static final int POST_MODULE_PERSISTENCE_REF                 = 0x1600;
    public static final int POST_MODULE_PERSISTENCE_CLASS_FILE_TRANSFORMER = 0x1620;
    public static final int POST_MODULE_DATASOURCE_REF                  = 0x1700;
    public static final int POST_MODULE_WS_REF_DESCRIPTOR               = 0x1800;
    public static final int POST_MODULE_WS_REF_ANNOTATION               = 0x1801;
    public static final int POST_MODULE_JAXRS_SCANNING                  = 0x1A00;
    public static final int POST_MODULE_JAXRS_COMPONENT                 = 0x1B00;
    public static final int POST_MODULE_JAXRS_CDI_INTEGRATION           = 0x1C00;
    public static final int POST_MODULE_LOCAL_HOME                      = 0x1E00;
    public static final int POST_MODULE_APPLICATION_CLIENT_MANIFEST     = 0x1F00;
    public static final int POST_MODULE_APPLICATION_CLIENT_ACTIVE       = 0x2000;
    public static final int POST_MODULE_APP_CLIENT_METHOD_RESOLUTION    = 0x2020;
    public static final int POST_MODULE_EJB_ORB_BIND                    = 0x2100;
    public static final int POST_MODULE_EJB_CLIENT_CONTEXT_SETUP        = 0x2200;
    public static final int POST_MODULE_CMP_PARSE                       = 0x2300;
    public static final int POST_MODULE_CMP_ENTITY_METADATA             = 0x2400;
    public static final int POST_MODULE_CMP_STORE_MANAGER               = 0x2500;
    public static final int POST_MODULE_EJB_IIOP                        = 0x2600;
    public static final int POST_MODULE_POJO                            = 0x2700;
    public static final int POST_MODULE_NAMING_CONTEXT                  = 0x2800;
    public static final int POST_MODULE_APP_NAMING_CONTEXT              = 0x2900;
    public static final int POST_MODULE_CACHED_CONNECTION_MANAGER       = 0x2A00;

    // INSTALL
    public static final int INSTALL_JNDI_DEPENDENCY_SETUP               = 0x0100;
    public static final int INSTALL_JPA_INTERCEPTORS                    = 0x0200;
    public static final int INSTALL_JACC_POLICY                         = 0x0350;
    public static final int INSTALL_COMPONENT_AGGREGATION               = 0x0400;
    public static final int INSTALL_RESOLVE_MESSAGE_DESTINATIONS        = 0x0403;
    public static final int INSTALL_EJB_JACC_PROCESSING                 = 0x0403;
    public static final int INSTALL_SERVICE_ACTIVATOR                   = 0x0500;
    public static final int INSTALL_OSGI_DEPLOYMENT                     = 0x0600;
    public static final int INSTALL_OSGI_MODULE                         = 0x0650;
    public static final int INSTALL_RA_NATIVE                           = 0x0800;
    public static final int INSTALL_RA_DEPLOYMENT                       = 0x0801;
    public static final int INSTALL_SERVICE_DEPLOYMENT                  = 0x0900;
    public static final int INSTALL_POJO_DEPLOYMENT                     = 0x0A00;
    public static final int INSTALL_RA_XML_DEPLOYMENT                   = 0x0B00;
    public static final int INSTALL_EE_MODULE_CONFIG                    = 0x1101;
    public static final int INSTALL_MODULE_JNDI_BINDINGS                = 0x1200;
    public static final int INSTALL_DEPENDS_ON_ANNOTATION               = 0x1210;
    public static final int INSTALL_PERSISTENCE_PROVIDER                = 0x1215;   // before INSTALL_PERSISTENTUNIT
    public static final int INSTALL_PERSISTENTUNIT                      = 0x1220;
    public static final int INSTALL_EE_COMPONENT                        = 0x1230;
    public static final int INSTALL_SERVLET_INIT_DEPLOYMENT             = 0x1300;
    public static final int INSTALL_JAXRS_DEPLOYMENT                    = 0x1500;
    public static final int INSTALL_JSF_ANNOTATIONS                     = 0x1600;
    public static final int INSTALL_JDBC_DRIVER                         = 0x1800;
    public static final int INSTALL_TRANSACTION_BINDINGS                = 0x1900;
    public static final int INSTALL_BUNDLE_CONTEXT_BINDING              = 0x1A00;
    public static final int INSTALL_WELD_DEPLOYMENT                     = 0x1B00;
    public static final int INSTALL_WELD_BEAN_MANAGER                   = 0x1C00;
    public static final int INSTALL_JNDI_DEPENDENCIES                   = 0x1C01;
    public static final int INSTALL_WS_UNIVERSAL_META_DATA_MODEL        = 0x1C10;
    public static final int INSTALL_WS_DEPLOYMENT_ASPECTS               = 0x1C11;
    // IMPORTANT: WS integration installs deployment aspects dynamically
    // so consider INSTALL 0x1C10 - 0x1CFF reserved for WS subsystem!
    public static final int INSTALL_WAR_DEPLOYMENT                      = 0x1D00;
    public static final int INSTALL_DEPLOYMENT_REPOSITORY               = 0x1E00;
    public static final int INSTALL_EJB_MANAGEMENT_RESOURCES            = 0x1F00;
    public static final int INSTALL_APPLICATION_CLIENT                  = 0x2000;
    public static final int INSTALL_DSXML_DEPLOYMENT                    = 0x2010;
    public static final int INSTALL_MESSAGING_XML_RESOURCES             = 0x2011;

    // CLEANUP
    public static final int CLEANUP_REFLECTION_INDEX                    = 0x0100;
    public static final int CLEANUP_EE                                  = 0x0200;
    public static final int CLEANUP_EJB                                 = 0x0300;
}
