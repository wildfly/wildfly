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
    STRUCTURE,
    VALIDATE,
    PARSE,
    DEPENDENCIES,
    MODULARIZE,
    POST_MODULE,
    INSTALL,
    CLEANUP,
    ;
    private Phase() {}

    // STRUCTURE
    public static final int NESTED_JAR_INLINE_PROCESSOR                 = 0x100;
    public static final int RA_NESTED_JAR_INLINE_PROCESSOR              = 0x200;
    public static final int WAR_DEPLOYMENT_INITIALIZING_PROCESSOR       = 0x300;

    // VALIDATE
    public static final int WAR_STRUCTURE_DEPLOYMENT_PROCESSOR          = 0x100;

    // PARSE
    public static final int MANIFEST_ATTACHMENT_PROCESSOR               = 0x0100;
    public static final int OSGI_MANIFEST_ATTACHMENT_PROCESSOR          = 0x0200;
    public static final int OSGI_BUNDLE_INFO_ATTACHMENT_PROCESSOR       = 0x0300;
    public static final int ANNOTATION_INDEX_PROCESSOR                  = 0x0400;
    public static final int WAR_ANNOTATION_INDEX_PROCESSOR              = 0x0500;
    public static final int SERVICE_ACTIVATION_DEPENDENCY_PROCESSOR     = 0x0600;
    public static final int WEB_PARSING_DEPLOYMENT_PROCESSOR            = 0x0700;
    public static final int WEB_FRAGMENT_PARSING_DEPLOYMENT_PROCESSOR   = 0x0800;
    public static final int JBOSS_WEB_PARSING_DEPLOYMENT_PROCESSOR      = 0x0900;
    public static final int TLD_PARSING_DEPLOYMENT_PROCESSOR            = 0x0A00;
    public static final int RA_DEPLOYMENT_PARSING_PROCESSOR             = 0x0B00;
    public static final int SERVICE_DEPLOYMENT_PARSING_PROCESSOR        = 0x0C00;
    public static final int MC_BEAN_DEPLOYMENT_PARSING_PROCESSOR        = 0x0D00;
    public static final int IRON_JACAMAR_DEPLOYMENT_PARSING_PROCESSOR   = 0x0E00;
    public static final int RESOURCE_ADAPTERS_ATTACHING_PROCESSOR       = 0x0F00;
    public static final int DATA_SOURCES_ATTACHMENT_PROCESSOR           = 0x1000;
    public static final int ARQUILLIAN_RUNWITH_ANNOTATION_PROCESSOR     = 0x1100;

    // DEPENDENCIES
    public static final int MODULE_DEPENDENCY_PROCESSOR                 = 0x100;
    public static final int DS_DEPENDENCY_PROCESSOR                     = 0x200;
    public static final int RAR_CONFIG_PROCESSOR                        = 0x300;
    public static final int MANAGED_BEAN_DEPENDENCY_PROCESSOR           = 0x400;
    public static final int SAR_MODULE_DEPENDENCY_PROCESSOR             = 0x500;
    public static final int WAR_CLASSLOADING_DEPENDENCY_PROCESSOR       = 0x600;
    public static final int ARQUILLIAN_DEPENDENCY_PROCESSOR             = 0x700;

    // MODULARIZE
    public static final int WAR_MODULE_CONFIG_PROCESSOR                 = 0x100;
    public static final int MODULE_CONFIG_PROCESSOR                     = 0x200;
    public static final int DEPLOYMENT_MODULE_LOADER_PROCESSOR          = 0x300;
    public static final int MODULE_DEPLOYMENT_PROCESSOR                 = 0x400;

    // POST_MODULE
    public static final int MANAGED_BEAN_ANNOTATION_PROCESSOR           = 0x100;
    public static final int WAR_ANNOTATION_DEPLOYMENT_PROCESSOR         = 0x200;
    public static final int ARQUILLIAN_JUNIT_ANNOTATION_PROCESSOR       = 0x300;

    // INSTALL
    public static final int MODULE_CONTEXT_PROCESSOR                    = 0x100;
    public static final int SERVICE_ACTIVATOR_PROCESSOR                 = 0x200;
    public static final int OSGI_ATTACHMENTS_DEPLOYMENT_PROCESSOR       = 0x300;
    public static final int WAR_META_DATA_PROCESSOR                     = 0x400;
    public static final int PARSED_RA_DEPLOYMENT_PROCESSOR              = 0x500;
    public static final int PARSED_SERVICE_DEPLOYMENT_PROCESSOR         = 0x600;
    public static final int PARSED_MC_BEAN_DEPLOYMENT_PROCESSOR         = 0x700;
    public static final int RA_XML_DEPLOYMENT_PROCESSOR                 = 0x800;
    public static final int DS_DEPLOYMENT_PROCESSOR                     = 0x900;
    public static final int MANAGED_BEAN_DEPLOYMENT_PROCESSOR           = 0xA00;
    public static final int SERVLET_CONTAINER_INITIALIZER_DEPLOYMENT_PROCESSOR  = 0xB00;
    public static final int WAR_DEPLOYMENT_PROCESSOR                    = 0xC00;
    public static final int ARQUILLIAN_DEPLOYMENT_PROCESSOR             = 0xD00;

    // CLEANUP
    // (none)
}
