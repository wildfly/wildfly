/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
