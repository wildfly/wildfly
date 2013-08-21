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

/**
 * @author Emanuel Muckenhuber
 */
public class Constants {

    public static final String MODULES = "modules";
    public static final String BUNDLES = "bundles";
    public static final String MISC = "misc";

    public static final String PATCH = "patch";
    public static final String PATCH_ID = "patch-id";
    public static final String ROLLBACK = "rollback";

    public static final String OVERRIDE_MODULES = "override-modules";
    public static final String OVERRIDE_ALL = "override-all";
    public static final String OVERRIDE = "override";
    public static final String PRESERVE = "preserve";

    public static final String TIMESTAMP = "timestamp";
    public static final String APPLIED_AT = "applied-at";
}
