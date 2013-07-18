/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.Constants.CHECK_INTERVAL;
import static org.wildfly.extension.undertow.Constants.DISABLED;
import static org.wildfly.extension.undertow.Constants.DISPLAY_SOURCE_FRAGMENT;
import static org.wildfly.extension.undertow.Constants.DUMP_SMAP;
import static org.wildfly.extension.undertow.Constants.ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE;
import static org.wildfly.extension.undertow.Constants.GENERATE_STRINGS_AS_CHAR_ARRAYS;
import static org.wildfly.extension.undertow.Constants.JAVA_ENCODING;
import static org.wildfly.extension.undertow.Constants.KEEP_GENERATED;
import static org.wildfly.extension.undertow.Constants.MAPPED_FILE;
import static org.wildfly.extension.undertow.Constants.MODIFICATION_TEST_INTERVAL;
import static org.wildfly.extension.undertow.Constants.RECOMPILE_ON_FAIL;
import static org.wildfly.extension.undertow.Constants.SCRATCH_DIR;
import static org.wildfly.extension.undertow.Constants.SMAP;
import static org.wildfly.extension.undertow.Constants.SOURCE_VM;
import static org.wildfly.extension.undertow.Constants.TAG_POOLING;
import static org.wildfly.extension.undertow.Constants.TARGET_VM;
import static org.wildfly.extension.undertow.Constants.TRIM_SPACES;
import static org.wildfly.extension.undertow.Constants.X_POWERED_BY;

import io.undertow.servlet.api.ServletInfo;
import org.apache.jasper.servlet.JspServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class JSPService implements Service<JSPService> {
    private final ModelNode config;
    private ServletInfo servletInfo;

    private final InjectedValue<ServletContainerService> servletContainerServiceInjectedValue = new InjectedValue<>();


    public JSPService(ModelNode config) {
        this.config = config;
    }

    @Override
    public void start(StartContext context) throws StartException {
        this.servletInfo = configureServletInfo();
    }

    @Override
    public void stop(StopContext context) {
        this.servletInfo = null;
    }

    @Override
    public JSPService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ServletContainerService> getServletContainerServiceInjectedValue() {
        return servletContainerServiceInjectedValue;
    }

    /**
     * Add the jsp servlet
     */
    private ServletInfo configureServletInfo() {
        boolean disabled = config.get(DISABLED).asBoolean(false);
        if (disabled) {
            return null;
        }

        final io.undertow.servlet.api.ServletInfo jspServlet = new ServletInfo("Default JSP Servlet", JspServlet.class)
                .addMapping("*.jsp")
                .addMapping("*.jspx");


        jspServlet.addInitParam("development", Boolean.toString(servletContainerServiceInjectedValue.getValue().isDevelopmentMode()));
        jspServlet.addInitParam("keepgenerated", config.require(KEEP_GENERATED).asString());
        jspServlet.addInitParam("trimSpaces", config.require(TRIM_SPACES).asString());
        jspServlet.addInitParam("enablePooling", config.require(TAG_POOLING).asString());
        jspServlet.addInitParam("mappedfile", config.require(MAPPED_FILE).asString());
        jspServlet.addInitParam("checkInterval", config.require(CHECK_INTERVAL).asString());
        jspServlet.addInitParam("modificationTestInterval", config.require(MODIFICATION_TEST_INTERVAL).asString());
        jspServlet.addInitParam("recompileOnFail", config.require(RECOMPILE_ON_FAIL).asString());
        jspServlet.addInitParam("suppressSmap", Boolean.toString(!config.require(SMAP).asBoolean()));
        jspServlet.addInitParam("dumpSmap", config.require(DUMP_SMAP).asString());
        jspServlet.addInitParam("genStringAsCharArray", config.require(GENERATE_STRINGS_AS_CHAR_ARRAYS).asString());
        jspServlet.addInitParam("errorOnUseBeanInvalidClassAttribute", config.require(ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE).asString());

        if (config.hasDefined(SCRATCH_DIR)) {
            jspServlet.addInitParam("scratchdir", config.require(SCRATCH_DIR).asString());
        }
        // jasper will find the right defaults.
        jspServlet.addInitParam("compilerSourceVM", config.require(SOURCE_VM).asString());
        jspServlet.addInitParam("compilerTargetVM", config.require(TARGET_VM).asString());
        jspServlet.addInitParam("javaEncoding", config.require(JAVA_ENCODING).asString());
        jspServlet.addInitParam("xpoweredBy", config.require(X_POWERED_BY).asString());
        jspServlet.addInitParam("displaySourceFragment", config.require(DISPLAY_SOURCE_FRAGMENT).asString());


        return jspServlet;
    }

    public ServletInfo getJSPServletInfo() {
        return servletInfo;
    }
}
