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
public final class Phase {
    private Phase() {}

    public static final int STRUCTURE       = 0x00100000;

    public static final int NESTED_JAR_INLINE_PROCESSOR                 = STRUCTURE + 0x100;
    public static final int RA_NESTED_JAR_INLINE_PROCESSOR              = STRUCTURE + 0x200;
    public static final int WAR_DEPLOYMENT_INITIALIZING_PROCESSOR       = STRUCTURE + 0x300;

    public static final int VALIDATE        = 0x00200000;

    public static final int WAR_STRUCTURE_DEPLOYMENT_PROCESSOR          = VALIDATE + 0x100;

    public static final int PARSE           = 0x00300000;

    public static final int MANIFEST_ATTACHMENT_PROCESSOR               = PARSE + 0x100;
    public static final int OSGI_MANIFEST_DEPLOYMENT_PROCESSOR          = PARSE + 0x200;
    public static final int ANNOTATION_INDEX_PROCESSOR                  = PARSE + 0x300;
    public static final int WAR_ANNOTATION_INDEX_PROCESSOR              = PARSE + 0x400;
    public static final int SERVICE_ACTIVATION_DEPENDENCY_PROCESSOR     = PARSE + 0x500;
    public static final int WEB_PARSING_DEPLOYMENT_PROCESSOR            = PARSE + 0x600;
    public static final int WEB_FRAGMENT_PARSING_DEPLOYMENT_PROCESSOR   = PARSE + 0x700;
    public static final int JBOSS_WEB_PARSING_DEPLOYMENT_PROCESSOR      = PARSE + 0x800;
    public static final int TLD_PARSING_DEPLOYMENT_PROCESSOR            = PARSE + 0x900;
    public static final int RA_DEPLOYMENT_PARSING_PROCESSOR             = PARSE + 0xA00;
    public static final int SERVICE_DEPLOYMENT_PARSING_PROCESSOR        = PARSE + 0xB00;
    public static final int IRON_JACAMAR_DEPLOYMENT_PARSING_PROCESSOR   = PARSE + 0xC00;
    public static final int RESOURCE_ADAPTERS_ATTACHING_PROCESSOR       = PARSE + 0xD00;
    public static final int DATA_SOURCES_ATTACHMENT_PROCESSOR           = PARSE + 0xE00;

    public static final int DEPENDENCIES    = 0x00400000;

    public static final int MODULE_DEPENDENCY_PROCESSOR                 = DEPENDENCIES + 0x100;
    public static final int DS_DEPENDENCY_PROCESSOR                     = DEPENDENCIES + 0x200;
    public static final int RAR_CONFIG_PROCESSOR                        = DEPENDENCIES + 0x300;
    public static final int MANAGED_BEAN_DEPENDENCY_PROCESSOR           = DEPENDENCIES + 0x400;
    public static final int SAR_MODULE_DEPENDENCY_PROCESSOR             = DEPENDENCIES + 0x500;
    public static final int WAR_CLASSLOADING_DEPENDENCY_PROCESSOR       = DEPENDENCIES + 0x600;

    public static final int MODULARIZE      = 0x00500000;

    public static final int WAR_MODULE_CONFIG_PROCESSOR                 = MODULARIZE + 0x100;
    public static final int MODULE_CONFIG_PROCESSOR                     = MODULARIZE + 0x200;
    public static final int DEPLOYMENT_MODULE_LOADER_PROCESSOR          = MODULARIZE + 0x300;
    public static final int MODULE_DEPLOYMENT_PROCESSOR                 = MODULARIZE + 0x400;

    public static final int POST_MODULE     = 0x00600000;

    public static final int MANAGED_BEAN_ANNOTATION_PROCESSOR           = POST_MODULE + 0x100;
    public static final int WAR_ANNOTATION_DEPLOYMENT_PROCESSOR         = POST_MODULE + 0x200;

    public static final int INSTALL         = 0x00700000;

    public static final int MODULE_CONTEXT_PROCESSOR                    = INSTALL + 0x100;
    public static final int SERVICE_ACTIVATOR_PROCESSOR                 = INSTALL + 0x200;
    public static final int OSGI_ATTACHMENTS_DEPLOYMENT_PROCESSOR       = INSTALL + 0x300;
    public static final int WAR_META_DATA_PROCESSOR                     = INSTALL + 0x400;
    public static final int PARSED_RA_DEPLOYMENT_PROCESSOR              = INSTALL + 0x500;
    public static final int PARSED_SERVICE_DEPLOYMENT_PROCESSOR         = INSTALL + 0x600;
    public static final int RA_XML_DEPLOYMENT_PROCESSOR                 = INSTALL + 0x700;
    public static final int DS_DEPLOYMENT_PROCESSOR                     = INSTALL + 0x800;
    public static final int MANAGED_BEAN_DEPLOYMENT_PROCESSOR           = INSTALL + 0x900;
    public static final int SERVLET_CONTAINER_INITIALIZER_DEPLOYMENT_PROCESSOR  = INSTALL + 0xA00;
    public static final int WAR_DEPLOYMENT_PROCESSOR                    = INSTALL + 0xB00;

    public static final int CLEANUP         = 0x00800000;
}
