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

package org.jboss.as.patching;

import org.jboss.msc.service.ServiceName;

/**
 * @author Emanuel Muckenhuber
 */
public class Constants {

    public static final String ALL_PATCHES = "installed-patches";
    public static final String BUNDLES = "bundles";
    public static final String CONFLICTS = "conflicts";
    public static final String CUMULATIVE = "cumulative-patch-id";
    public static final String MESSAGE = "message";
    public static final String MISC = "misc";
    public static final String MODULES = "modules";
    public static final String NAME = "name";
    public static final String VERSION = "version";

    public static final String PATCH = "patch";
    public static final String PATCH_ID = "patch-id";
    public static final String RESET_CONFIGURATION = "reset-configuration";
    public static final String ROLLBACK = "rollback";
    public static final String ROLLBACK_LAST = "rollback-last";
    public static final String ROLLBACK_TO = "rollback-to";
    /** The default if no patches are active. */
    public static final String BASE = "base";
    public static final String NOT_PATCHED = BASE;

    public static final String OVERRIDE_MODULES = "override-modules";
    public static final String OVERRIDE_ALL = "override-all";
    public static final String OVERRIDE = "override";
    public static final String PRESERVE = "preserve";

    public static final String APPLIED_AT = "applied-at";
    public static final String TIMESTAMP = "timestamp";
    public static final String TYPE = "type";

    // Directories
    public static final String APP_CLIENT = "appclient";
    public static final String CONFIGURATION = "configuration";
    public static final String DOMAIN = "domain";
    public static final String INSTALLATION = ".installation";
    public static final String METADATA = ".metadata";
    public static final String RESTORED_CONFIGURATION = "restored-configuration";
    public static final String PATCHES = "patches";
    public static final String OVERLAYS = ".overlays";
    public static final String STANDALONE = "standalone";

    public static final String LAYERS = "layers";
    public static final String ADD_ONS = "add-ons";
    public static final String DEFAULT_LAYERS_PATH = "system/layers";
    public static final String DEFAULT_ADD_ONS_PATH = "system/add-ons";
    public static final String EXCLUDE_LAYER_BASE = "exclude.base.layer";

    // Files
    public static final String IDENTITY_METADATA = "identity.conf";    // .installation/identity.conf
    public static final String INSTALLATION_METADATA = "layer.conf";   // .installation/layer/xxx/layer.conf
    public static final String LAYERS_CONF = "layers.conf";            // modules/layers.conf
    public static final String ROLLBACK_XML = "rollback.xml";

    // Service names
    public static final ServiceName JBOSS_AS = ServiceName.JBOSS.append("as");
    public static final ServiceName JBOSS_PATCHING = ServiceName.JBOSS.append("patching");
    public static final ServiceName JBOSS_PRODUCT_CONFIG_SERVICE = JBOSS_AS.append("product-config");
    public static final String SYSTEM = "system";

}
