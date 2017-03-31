/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.servlet.api.ServletInfo;
import org.apache.jasper.servlet.JspServlet;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class JSPConfig {
    private final ServletInfo servletInfo;


    public JSPConfig(final boolean developmentMode,
                     final boolean disabled,
                     final boolean keepGenerated, final boolean trimSpaces, final boolean tagPooling,
                     final boolean mappedFile, final int checkInterval, int modificationTestInterval,
                     final boolean recompileOnFail, boolean smap, boolean dumpSmap,
                     boolean genStringAsCharArray, boolean errorOnUseBeanInvalidClassAttribute,
                     String scratchDir, String sourceVm, String targetVm, String javaEncoding,
                     boolean xPoweredBy, boolean displaySourceFragment, boolean optimizeScriptlets) {
        if (disabled) {
            servletInfo = null;
        } else {

            final io.undertow.servlet.api.ServletInfo jspServlet = new ServletInfo("jsp", JspServlet.class);
            jspServlet.setRequireWelcomeFileMapping(true);

            jspServlet.addInitParam("development", Boolean.toString(developmentMode));
            jspServlet.addInitParam("keepgenerated", Boolean.toString(keepGenerated));
            jspServlet.addInitParam("trimSpaces", Boolean.toString(trimSpaces));
            jspServlet.addInitParam("enablePooling", Boolean.toString(tagPooling));
            jspServlet.addInitParam("mappedfile", Boolean.toString(mappedFile));
            jspServlet.addInitParam("checkInterval", Integer.toString(checkInterval));
            jspServlet.addInitParam("modificationTestInterval", Integer.toString(modificationTestInterval));
            jspServlet.addInitParam("recompileOnFail", Boolean.toString(recompileOnFail));
            jspServlet.addInitParam("suppressSmap", Boolean.toString(!smap));
            jspServlet.addInitParam("dumpSmap", Boolean.toString(dumpSmap));
            jspServlet.addInitParam("genStringAsCharArray", Boolean.toString(genStringAsCharArray));
            jspServlet.addInitParam("errorOnUseBeanInvalidClassAttribute", Boolean.toString(errorOnUseBeanInvalidClassAttribute));
            jspServlet.addInitParam("optimizeScriptlets", Boolean.toString(optimizeScriptlets));
            if (scratchDir != null) {
                jspServlet.addInitParam("scratchdir", scratchDir);
            }
            // jasper will find the right defaults.
            jspServlet.addInitParam("compilerSourceVM", sourceVm);
            jspServlet.addInitParam("compilerTargetVM", targetVm);
            jspServlet.addInitParam("javaEncoding", javaEncoding);
            jspServlet.addInitParam("xpoweredBy", Boolean.toString(xPoweredBy));
            jspServlet.addInitParam("displaySourceFragment", Boolean.toString(displaySourceFragment));
            this.servletInfo = jspServlet;
        }
    }

    public ServletInfo createJSPServletInfo() {
        if(servletInfo == null) {
            return null;
        }
        return servletInfo.clone();
    }
}
