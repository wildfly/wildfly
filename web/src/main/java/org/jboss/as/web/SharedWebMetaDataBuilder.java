/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.web;

import static org.jboss.as.web.Constants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.spec.MimeMappingMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.metadata.web.spec.ServletsMetaData;
import org.jboss.metadata.web.spec.SessionConfigMetaData;
import org.jboss.metadata.web.spec.WebMetaData;
import org.jboss.metadata.web.spec.WelcomeFileListMetaData;

/**
 * Internal helper creating a shared web.xml based on the domain configuration.
 *
 * @author Emanuel Muckenhuber
 */
class SharedWebMetaDataBuilder {

    static final List<String> welcomeFiles = new ArrayList<String>();
    static final List<MimeMappingMetaData> mimeMappings = new ArrayList<MimeMappingMetaData>();

    static {
        // Create the default mappings
        createMappings(mimeMappings);
        welcomeFiles.add("index.html");
        welcomeFiles.add("index.htm");
        welcomeFiles.add("index.jsp");
    }

    /** The common container config. */
    private final ModelNode containerConfig;

    SharedWebMetaDataBuilder(final ModelNode containerConfig) {
        this.containerConfig = containerConfig;
        init();
    }

    private void init() {
        if(containerConfig.has(Constants.MIME_MAPPING)) {
            for(final Property mapping : containerConfig.get(Constants.MIME_MAPPING).asPropertyList()) {
                mimeMappings.add(createMimeMapping(mapping.getName(), mapping.getValue().asString()));
            }
        }
        if(containerConfig.has(Constants.WELCOME_FILE)) {
            for(final ModelNode file : containerConfig.get(Constants.WELCOME_FILE).asList()) {
                welcomeFiles.add(file.asString());
            }
        }
    }

    WebMetaData create() {
        final WebMetaData metadata = new WebMetaData();

        metadata.setServlets(new ServletsMetaData());

        // Add DefaultServlet
        enableStaticResouces(metadata);
        // Add JSPServlet
        enableJsp(metadata);

        // Session config
        final SessionConfigMetaData sessionConfig = new SessionConfigMetaData();
        sessionConfig.setSessionTimeout(30);
        metadata.setSessionConfig(sessionConfig);

        // Mime mappings
        metadata.setMimeMappings(Collections.unmodifiableList(mimeMappings));

        // Welcome files
        metadata.setWelcomeFileList(new WelcomeFileListMetaData());
        metadata.getWelcomeFileList().setWelcomeFiles(Collections.unmodifiableList(welcomeFiles));
        return metadata;
    }

    /**
     * Enable resource serving by adding the {@code DefaultServlet}, using the
     * domain resource serving configuration.
     *
     * @param metadata the shared jboss web metadata
     *
     */
    void enableStaticResouces(final WebMetaData metadata) {
        final ModelNode resourcesConfig = containerConfig.get(STATIC_RESOURCES);
        // Check disabled
        if (resourcesConfig.has(DISABLED) && resourcesConfig.get(DISABLED).asBoolean()) {
            return;
        }
        final ServletMetaData servlet = new ServletMetaData();
        servlet.setName("default");
        servlet.setLoadOnStartup("" + 1);
        if (resourcesConfig.has(WEBDAV) && resourcesConfig.get(WEBDAV).asBoolean()) {
            servlet.setServletClass("org.apache.catalina.servlets.WebdavServlet");
        } else {
            servlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        }

        final List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();
        if (resourcesConfig.has(LISTINGS)) {
            initParams.add(createParameter("listings", resourcesConfig.get(LISTINGS).asString()));
        } else {
            initParams.add(createParameter("listings", "false"));
        }
        if (resourcesConfig.has(READ_ONLY)) {
            initParams.add(createParameter("readonly", resourcesConfig.get(READ_ONLY).asString()));
        } else {
            initParams.add(createParameter("readonly", "true"));
        }
        if (resourcesConfig.has(SENDFILE)) {
            initParams.add(createParameter("sendfile", resourcesConfig.get(SENDFILE).asString()));
        } else {
            initParams.add(createParameter("sendfile", "49152"));
        }
        if (resourcesConfig.has(FILE_ENCODING)) {
            initParams.add(createParameter("file-encoding", resourcesConfig.get(FILE_ENCODING).asString()));
        }
        if (resourcesConfig.has(SECRET)) {
            initParams.add(createParameter("secret", resourcesConfig.get(SECRET).asString()));
        }
        if (resourcesConfig.has(MAX_DEPTH)) {
            initParams.add(createParameter("max-depth", resourcesConfig.get(MAX_DEPTH).asString()));
        } else {
            initParams.add(createParameter("max-depth", "3"));
        }
        servlet.setInitParam(initParams);
        metadata.getServlets().add(servlet);
        addServletMapping("default", metadata, "/");
    }


    /**
     * Add the jsp servlet
     *
     * @param metadata the shared jboss.web metadata
     */
    void enableJsp(final WebMetaData metadata) {
        final ModelNode config = containerConfig.get(JSP_CONFIGURATION);
        if (config.get(DISABLED).asBoolean(false)) {
            return;
        }
        final ServletMetaData servlet = new ServletMetaData();
        servlet.setName("jsp");
        servlet.setLoadOnStartup("" + 3);
        servlet.setServletClass("org.apache.jasper.servlet.JspServlet");

        final List<ParamValueMetaData> initParams = new ArrayList<ParamValueMetaData>();

        if (config.has(DEVELOPMENT)) {
            initParams.add(createParameter("development", config.get(DEVELOPMENT).asString()));
        } else {
            initParams.add(createParameter("development", "false"));
        }
        if (config.has(KEEP_GENERATED)) {
            initParams.add(createParameter("keepgenerated", config.get(KEEP_GENERATED).asString()));
        } else {
            initParams.add(createParameter("keepgenerated", "true"));
        }
        if (config.has(TRIM_SPACES)) {
            initParams.add(createParameter("trimSpaces", config.get(TRIM_SPACES).asString()));
        } else {
            initParams.add(createParameter("trimSpaces", "false"));
        }
        if (config.has(TAG_POOLING)) {
            initParams.add(createParameter("enablePooling", config.get(TAG_POOLING).asString()));
        } else {
            initParams.add(createParameter("enablePooling", "true"));
        }
        if (config.has(MAPPED_FILE)) {
            initParams.add(createParameter("mappedfile", config.get(MAPPED_FILE).asString()));
        } else {
            initParams.add(createParameter("mappedfile", "true"));
        }
        if (config.has(CHECK_INTERVAL)) {
            initParams.add(createParameter("checkInterval", config.get(CHECK_INTERVAL).asString()));
        } else {
            initParams.add(createParameter("checkInterval", "0"));
        }
        if (config.has(MODIFICATION_TEST_INTERVAL)) {
            initParams.add(createParameter("modificationTestInterval", config.get(MODIFICATION_TEST_INTERVAL).asString()));
        } else {
            initParams.add(createParameter("modificationTestInterval", "4"));
        }
        if (config.has(RECOMPILE_ON_FAIL)) {
            initParams.add(createParameter("recompileOnFail", config.get(RECOMPILE_ON_FAIL).asString()));
        } else {
            initParams.add(createParameter("recompileOnFail", "false"));
        }
        if (config.has(SMAP)) {
            initParams.add(createParameter("suppressSmap", config.get(SMAP).asString()));
        } else {
            initParams.add(createParameter("suppressSmap", "false"));
        }
        if (config.has(DUMP_SMAP)) {
            initParams.add(createParameter("dumpSmap", config.get(DUMP_SMAP).asString()));
        } else {
            initParams.add(createParameter("dumpSmap", "false"));
        }
        if (config.has(GENERATE_STRINGS_AS_CHAR_ARRAYS)) {
            initParams.add(createParameter("genStringAsCharArray", config.get(GENERATE_STRINGS_AS_CHAR_ARRAYS).asString()));
        } else {
            initParams.add(createParameter("genStringAsCharArray", "false"));
        }
        if (config.has(ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE)) {
            initParams.add(createParameter("errorOnUseBeanInvalidClassAttribute", config.get(ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE).asString()));
        } else {
            initParams.add(createParameter("errorOnUseBeanInvalidClassAttribute", "false"));
        }
        if (config.has(SCRATCH_DIR)) {
            initParams.add(createParameter("scratchdir", config.get(SCRATCH_DIR).asString()));
        }
        // jasper will find the right defaults.
        if (config.has(SOURCE_VM)) {
            initParams.add(createParameter("compilerSourceVM", config.get(SOURCE_VM).asString()));
        } else {
            initParams.add(createParameter("compilerSourceVM", "1.5"));
        }
        if (config.has(TARGET_VM)) {
            initParams.add(createParameter("compilerTargetVM", config.get(TARGET_VM).asString()));
        } else {
            initParams.add(createParameter("compilerTargetVM", "1.5"));
        }
        if (config.has(JAVA_ENCODING)) {
            initParams.add(createParameter("javaEncoding", config.get(JAVA_ENCODING).asString()));
        } else
            initParams.add(createParameter("javaEncoding", "UTF8"));

        if (config.has(X_POWERED_BY)) {
            initParams.add(createParameter("xpoweredBy", config.get(X_POWERED_BY).asString()));
        } else {
            initParams.add(createParameter("xpoweredBy", "true"));
        }
        if (config.has(DISPLAY_SOURCE_FRAGMENT)) {
            initParams.add(createParameter("displaySourceFragment", config.get(DISPLAY_SOURCE_FRAGMENT).asString()));
        } else {
            initParams.add(createParameter("displaySourceFragment", "true"));
        }
        servlet.setInitParam(initParams);
        metadata.getServlets().add(servlet);
        addServletMapping("jsp", metadata, "*.jsp", "*.jspx");
    }

    static ParamValueMetaData createParameter(String name, String value) {
        ParamValueMetaData param = new ParamValueMetaData();
        param.setParamName(name);
        param.setParamValue(value);
        return param;
    }

    static void addServletMapping(final String servlet, final WebMetaData metadata, String... names) {
        List<ServletMappingMetaData> mappings = metadata.getServletMappings();
        if(mappings == null) {
            mappings = new ArrayList<ServletMappingMetaData>();
            metadata.setServletMappings(mappings);
        }
        ServletMappingMetaData mapping = new ServletMappingMetaData();
        mapping.setUrlPatterns(Arrays.asList(names));
        mapping.setServletName(servlet);
        mappings.add(mapping);
    }

    static MimeMappingMetaData createMimeMapping(String extension, String mimeType) {
        MimeMappingMetaData mapping = new MimeMappingMetaData();
        mapping.setExtension(extension);
        mapping.setMimeType(mimeType);
        return mapping;
    }

    static void createMappings(final List<MimeMappingMetaData> mappings) {
        mappings.add(createMimeMapping("abs", "audio/x-mpeg"));
        mappings.add(createMimeMapping("ai", "application/postscript"));
        mappings.add(createMimeMapping("aif", "audio/x-aiff"));
        mappings.add(createMimeMapping("aifc", "audio/x-aiff"));
        mappings.add(createMimeMapping("aiff", "audio/x-aiff"));
        mappings.add(createMimeMapping("aim", "application/x-aim"));
        mappings.add(createMimeMapping("art", "image/x-jg"));
        mappings.add(createMimeMapping("asf", "video/x-ms-asf"));
        mappings.add(createMimeMapping("asx", "video/x-ms-asf"));
        mappings.add(createMimeMapping("au", "audio/basic"));
        mappings.add(createMimeMapping("avi", "video/x-msvideo"));
        mappings.add(createMimeMapping("avx", "video/x-rad-screenplay"));
        mappings.add(createMimeMapping("bcpio", "application/x-bcpio"));
        mappings.add(createMimeMapping("bin", "application/octet-stream"));
        mappings.add(createMimeMapping("bmp", "image/bmp"));
        mappings.add(createMimeMapping("body", "text/html"));
        mappings.add(createMimeMapping("cdf", "application/x-cdf"));
        mappings.add(createMimeMapping("cer", "application/x-x509-ca-cert"));
        mappings.add(createMimeMapping("class", "application/java"));
        mappings.add(createMimeMapping("cpio", "application/x-cpio"));
        mappings.add(createMimeMapping("csh", "application/x-csh"));
        mappings.add(createMimeMapping("css", "text/css"));
        mappings.add(createMimeMapping("dib", "image/bmp"));
        mappings.add(createMimeMapping("doc", "application/msword"));
        mappings.add(createMimeMapping("dtd", "application/xml-dtd"));
        mappings.add(createMimeMapping("dv", "video/x-dv"));
        mappings.add(createMimeMapping("dvi", "application/x-dvi"));
        mappings.add(createMimeMapping("eps", "application/postscript"));
        mappings.add(createMimeMapping("etx", "text/x-setext"));
        mappings.add(createMimeMapping("exe", "application/octet-stream"));
        mappings.add(createMimeMapping("gif", "image/gif"));
        mappings.add(createMimeMapping("gtar", "application/x-gtar"));
        mappings.add(createMimeMapping("gz", "application/x-gzip"));
        mappings.add(createMimeMapping("hdf", "application/x-hdf"));
        mappings.add(createMimeMapping("hqx", "application/mac-binhex40"));
        mappings.add(createMimeMapping("htc", "text/x-component"));
        mappings.add(createMimeMapping("htm", "text/html"));
        mappings.add(createMimeMapping("html", "text/html"));
        mappings.add(createMimeMapping("hqx", "application/mac-binhex40"));
        mappings.add(createMimeMapping("ief", "image/ief"));
        mappings.add(createMimeMapping("jad", "text/vnd.sun.j2me.app-descriptor"));
        mappings.add(createMimeMapping("jar", "application/java-archive"));
        mappings.add(createMimeMapping("java", "text/plain"));
        mappings.add(createMimeMapping("jnlp", "application/x-java-jnlp-file"));
        mappings.add(createMimeMapping("jpe", "image/jpeg"));
        mappings.add(createMimeMapping("jpeg", "image/jpeg"));
        mappings.add(createMimeMapping("jpg", "image/jpeg"));
        mappings.add(createMimeMapping("js", "text/javascript"));
        mappings.add(createMimeMapping("jsf", "text/plain"));
        mappings.add(createMimeMapping("jspf", "text/plain"));
        mappings.add(createMimeMapping("kar", "audio/x-midi"));
        mappings.add(createMimeMapping("latex", "application/x-latex"));
        mappings.add(createMimeMapping("m3u", "audio/x-mpegurl"));
        mappings.add(createMimeMapping("mac", "image/x-macpaint"));
        mappings.add(createMimeMapping("man", "application/x-troff-man"));
        mappings.add(createMimeMapping("mathml", "application/mathml+xml"));
        mappings.add(createMimeMapping("me", "application/x-troff-me"));
        mappings.add(createMimeMapping("mid", "audio/x-midi"));
        mappings.add(createMimeMapping("midi", "audio/x-midi"));
        mappings.add(createMimeMapping("mif", "application/x-mif"));
        mappings.add(createMimeMapping("mov", "video/quicktime"));
        mappings.add(createMimeMapping("movie", "video/x-sgi-movie"));
        mappings.add(createMimeMapping("mp1", "audio/x-mpeg"));
        mappings.add(createMimeMapping("mp2", "audio/x-mpeg"));
        mappings.add(createMimeMapping("mp3", "audio/x-mpeg"));
        mappings.add(createMimeMapping("mp4", "video/mp4"));
        mappings.add(createMimeMapping("mpa", "audio/x-mpeg"));
        mappings.add(createMimeMapping("mpe", "video/mpeg"));
        mappings.add(createMimeMapping("mpeg", "video/mpeg"));
        mappings.add(createMimeMapping("mpega", "audio/x-mpeg"));
        mappings.add(createMimeMapping("mpg", "video/mpeg"));
        mappings.add(createMimeMapping("mpv2", "video/mpeg2"));
        mappings.add(createMimeMapping("ms", "application/x-wais-source"));
        mappings.add(createMimeMapping("nc", "application/x-netcdf"));
        mappings.add(createMimeMapping("oda", "application/oda"));
        mappings.add(createMimeMapping("odb", "application/vnd.oasis.opendocument.database"));
        mappings.add(createMimeMapping("odc", "application/vnd.oasis.opendocument.chart"));
        mappings.add(createMimeMapping("odf", "application/vnd.oasis.opendocument.formula"));
        mappings.add(createMimeMapping("odg", "application/vnd.oasis.opendocument.graphics"));
        mappings.add(createMimeMapping("odi", "application/vnd.oasis.opendocument.image"));
        mappings.add(createMimeMapping("odm", "application/vnd.oasis.opendocument.text-master"));
        mappings.add(createMimeMapping("odp", "application/vnd.oasis.opendocument.presentation"));
        mappings.add(createMimeMapping("ods", "application/vnd.oasis.opendocument.spreadsheet"));
        mappings.add(createMimeMapping("odt", "application/vnd.oasis.opendocument.text"));
        mappings.add(createMimeMapping("otg ", "application/vnd.oasis.opendocument.graphics-template"));
        mappings.add(createMimeMapping("oth", "application/vnd.oasis.opendocument.text-web"));
        mappings.add(createMimeMapping("otp", "application/vnd.oasis.opendocument.presentation-template"));
        mappings.add(createMimeMapping("ots", "application/vnd.oasis.opendocument.spreadsheet-template "));
        mappings.add(createMimeMapping("ott", "application/vnd.oasis.opendocument.text-template"));
        mappings.add(createMimeMapping("ogx", "application/ogg"));
        mappings.add(createMimeMapping("ogv", "video/ogg"));
        mappings.add(createMimeMapping("oga", "audio/ogg"));
        mappings.add(createMimeMapping("ogg", "audio/ogg"));
        mappings.add(createMimeMapping("spx", "audio/ogg"));
        mappings.add(createMimeMapping("flac", "audio/flac"));
        mappings.add(createMimeMapping("anx", "application/annodex"));
        mappings.add(createMimeMapping("axa", "audio/annodex"));
        mappings.add(createMimeMapping("axv", "video/annodex"));
        mappings.add(createMimeMapping("xspf", "application/xspf+xml"));
        mappings.add(createMimeMapping("pbm", "image/x-portable-bitmap"));
        mappings.add(createMimeMapping("pct", "image/pict"));
        mappings.add(createMimeMapping("pdf", "application/pdf"));
        mappings.add(createMimeMapping("pgm", "image/x-portable-graymap"));
        mappings.add(createMimeMapping("pic", "image/pict"));
        mappings.add(createMimeMapping("pict", "image/pict"));
        mappings.add(createMimeMapping("pls", "audio/x-scpls"));
        mappings.add(createMimeMapping("png", "image/png"));
        mappings.add(createMimeMapping("pnm", "image/x-portable-anymap"));
        mappings.add(createMimeMapping("pnt", "image/x-macpaint"));
        mappings.add(createMimeMapping("ppm", "image/x-portable-pixmap"));
        mappings.add(createMimeMapping("ppt", "application/powerpoint"));
        mappings.add(createMimeMapping("ps", "application/postscript"));
        mappings.add(createMimeMapping("psd", "image/x-photoshop"));
        mappings.add(createMimeMapping("qt", "video/quicktime"));
        mappings.add(createMimeMapping("qti", "image/x-quicktime"));
        mappings.add(createMimeMapping("qtif", "image/x-quicktime"));
        mappings.add(createMimeMapping("ras", "image/x-cmu-raster"));
        mappings.add(createMimeMapping("rdf", "application/rdf+xml"));
        mappings.add(createMimeMapping("rgb", "image/x-rgb"));
        mappings.add(createMimeMapping("rm", "application/vnd.rn-realmedia"));
        mappings.add(createMimeMapping("roff", "application/x-troff"));
        mappings.add(createMimeMapping("rtf", "application/rtf"));
        mappings.add(createMimeMapping("rtx", "text/richtext"));
        mappings.add(createMimeMapping("sh", "application/x-sh"));
        mappings.add(createMimeMapping("shar", "application/x-shar"));
        mappings.add(createMimeMapping("smf", "audio/x-midi"));
        mappings.add(createMimeMapping("sit", "application/x-stuffit"));
        mappings.add(createMimeMapping("snd", "audio/basic"));
        mappings.add(createMimeMapping("src", "application/x-wais-source"));
        mappings.add(createMimeMapping("sv4cpio", "application/x-sv4cpio"));
        mappings.add(createMimeMapping("sv4crc", "application/x-sv4crc"));
        mappings.add(createMimeMapping("swf", "application/x-shockwave-flash"));
        mappings.add(createMimeMapping("t", "application/x-troff"));
        mappings.add(createMimeMapping("tar", "application/x-tar"));
        mappings.add(createMimeMapping("tcl", "application/x-tcl"));
        mappings.add(createMimeMapping("tex", "application/x-tex"));
        mappings.add(createMimeMapping("texi", "application/x-texinfo"));
        mappings.add(createMimeMapping("texinfo", "application/x-texinfo"));
        mappings.add(createMimeMapping("tif", "image/tiff"));
        mappings.add(createMimeMapping("tiff", "image/tiff"));
        mappings.add(createMimeMapping("tr", "application/x-troff"));
        mappings.add(createMimeMapping("tsv", "text/tab-separated-values"));
        mappings.add(createMimeMapping("txt", "text/plain"));
        mappings.add(createMimeMapping("ulw", "audio/basic"));
        mappings.add(createMimeMapping("ustar", "application/x-ustar"));
        mappings.add(createMimeMapping("vxml", "application/voicexml+xml"));
        mappings.add(createMimeMapping("xbm", "image/x-xbitmap"));
        mappings.add(createMimeMapping("xht", "application/xhtml+xml"));
        mappings.add(createMimeMapping("xhtml", "application/xhtml+xml"));
        mappings.add(createMimeMapping("xml", "application/xml"));
        mappings.add(createMimeMapping("xpm", "image/x-xpixmap"));
        mappings.add(createMimeMapping("xsl", "application/xml"));
        mappings.add(createMimeMapping("xslt", "application/xslt+xml"));
        mappings.add(createMimeMapping("xul", "application/vnd.mozilla.xul+xml"));
        mappings.add(createMimeMapping("xwd", "image/x-xwindowdump"));
        mappings.add(createMimeMapping("wav", "audio/x-wav"));
        mappings.add(createMimeMapping("svg", "image/svg+xml"));
        mappings.add(createMimeMapping("svgz", "image/svg+xml"));
        mappings.add(createMimeMapping("vsd", "application/x-visio"));
        mappings.add(createMimeMapping("wbmp", "image/vnd.wap.wbmp"));
        mappings.add(createMimeMapping("wml", "text/vnd.wap.wml"));
        mappings.add(createMimeMapping("wmlc", "application/vnd.wap.wmlc"));
        mappings.add(createMimeMapping("wmls", "text/vnd.wap.wmlscript"));
        mappings.add(createMimeMapping("wmlscriptc", "application/vnd.wap.wmlscriptc"));
        mappings.add(createMimeMapping("wmv", "video/x-ms-wmv"));
        mappings.add(createMimeMapping("wrl", "x-world/x-vrml"));
        mappings.add(createMimeMapping("wsdl", "text/xml"));
        mappings.add(createMimeMapping("xsd", "text/xml"));
        mappings.add(createMimeMapping("Z", "application/x-compress"));
        mappings.add(createMimeMapping("z", "application/x-compress"));
        mappings.add(createMimeMapping("zip", "application/zip"));
        mappings.add(createMimeMapping("xls", "application/vnd.ms-excel"));
        mappings.add(createMimeMapping("doc", "application/vnd.ms-word"));
        mappings.add(createMimeMapping("ppt", "application/vnd.ms-powerpoint"));
    }

}
