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

package org.jboss.as.osgi.service;

/**
 * System packages definition
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Aug-2012
 */
interface SystemPackagesIntegration  {

    String[] DEFAULT_SYSTEM_MODULES = new String[] {
        "javax.api",
        "javax.inject.api",
        "org.apache.xerces",
        "org.jboss.as.configadmin",
        "org.jboss.as.controller-client",
        "org.jboss.as.osgi",
        "org.jboss.dmr",
        "org.jboss.logging",
        "org.jboss.modules",
        "org.jboss.msc",
        "org.jboss.osgi.framework",
        "org.jboss.osgi.repository",
        "org.jboss.osgi.resolver"
    };

    String[] DEFAULT_INTEGRATION_PACKAGES = new String[] {
        "javax.inject",
        "org.apache.xerces.jaxp",
        "org.jboss.as.configadmin",
        "org.jboss.as.controller.client",
        "org.jboss.as.controller.client.helpers",
        "org.jboss.as.controller.client.helpers.domain",
        "org.jboss.as.controller.client.helpers.standalone",
        "org.jboss.dmr;version=1.1.1",
        "org.jboss.logging;version=3.1.0",
        "org.jboss.osgi.repository;version=1.0",
        "org.osgi.service.repository;version=1.0"
    };

    String[] DEFAULT_CAPABILITIES = new String[] {
        "org.osgi.enterprise",
        "javax.annotation.api"
    };

    // Keep in sync with module javax.api
    String[] JAVAX_API_PACKAGES = new String[] {
        "javax.accessibility",
        "javax.activity",
        "javax.crypto",
        "javax.crypto.interfaces",
        "javax.crypto.spec",
        "javax.imageio",
        "javax.imageio.event",
        "javax.imageio.metadata",
        "javax.imageio.plugins.bmp",
        "javax.imageio.plugins.jpeg",
        "javax.imageio.spi",
        "javax.imageio.stream",
        "javax.lang.model",
        "javax.lang.model.element",
        "javax.lang.model.type",
        "javax.lang.model.util",
        "javax.management",
        "javax.management.loading",
        "javax.management.modelmbean",
        "javax.management.monitor",
        "javax.management.openmbean",
        "javax.management.relation",
        "javax.management.remote",
        "javax.management.remote.rmi",
        "javax.management.timer",
        "javax.naming",
        "javax.naming.directory",
        "javax.naming.event",
        "javax.naming.ldap",
        "javax.naming.spi",
        "javax.net",
        "javax.net.ssl",
        "javax.print",
        "javax.print.attribute",
        "javax.print.attribute.standard",
        "javax.print.event",
        "javax.rmi.ssl",
        "javax.script",
        "javax.security.auth",
        "javax.security.auth.callback",
        "javax.security.auth.kerberos",
        "javax.security.auth.login",
        "javax.security.auth.spi",
        "javax.security.auth.x500",
        "javax.security.cert",
        "javax.security.sasl",
        "javax.sound.midi",
        "javax.sound.midi.spi",
        "javax.sound.sampled",
        "javax.sound.sampled.spi",
        "javax.sql",
        "javax.sql.rowset",
        "javax.sql.rowset.serial",
        "javax.sql.rowset.spi",
        "javax.swing",
        "javax.swing.border",
        "javax.swing.colorchooser",
        "javax.swing.event",
        "javax.swing.filechooser",
        "javax.swing.plaf",
        "javax.swing.plaf.basic",
        "javax.swing.plaf.metal",
        "javax.swing.plaf.multi",
        "javax.swing.plaf.nimbus",
        "javax.swing.plaf.synth",
        "javax.swing.table",
        "javax.swing.text",
        "javax.swing.text.html",
        "javax.swing.text.html.parser",
        "javax.swing.text.rtf",
        "javax.swing.tree",
        "javax.swing.undo",
        "javax.tools",
        "javax.xml",
        "javax.xml.crypto",
        "javax.xml.crypto.dom",
        "javax.xml.crypto.dsig",
        "javax.xml.crypto.dsig.dom",
        "javax.xml.crypto.dsig.keyinfo",
        "javax.xml.crypto.dsig.spec",
        "javax.xml.datatype",
        "javax.xml.namespace",
        "javax.xml.parsers",
        "javax.xml.stream",
        "javax.xml.stream.events",
        "javax.xml.stream.util",
        "javax.xml.transform",
        "javax.xml.transform.dom",
        "javax.xml.transform.sax",
        "javax.xml.transform.stax",
        "javax.xml.transform.stream",
        "javax.xml.validation",
        "javax.xml.xpath",
        "org.ietf.jgss",
        "org.w3c.dom",
        "org.w3c.dom.bootstrap",
        "org.w3c.dom.css",
        "org.w3c.dom.events",
        "org.w3c.dom.html",
        "org.w3c.dom.ranges",
        "org.w3c.dom.stylesheets",
        "org.w3c.dom.traversal",
        "org.w3c.dom.ls",
        "org.w3c.dom.views",
        "org.w3c.dom.xpath",
        "org.xml.sax",
        "org.xml.sax.ext",
        "org.xml.sax.helpers"
    };
}
