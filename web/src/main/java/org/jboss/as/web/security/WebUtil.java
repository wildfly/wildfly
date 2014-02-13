/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.web.security;

import java.security.PrivilegedAction;
import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

/**
 * Provides utility static methods for the web security integration
 *
 * @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class WebUtil {

    /**
     * System Property setting to configure the web audit
     * <blockquote>
     * off = turn it off <br>
     * headers = audit the headers<br>
     * cookies = audit the cookies<br>
     * parameters = audit the parameters<br>
     * attributes = audit the attributes<br>
     * headers,cookies,parameters = audit the headers,cookie and parameters<br>
     * headers,cookies = audit the headers and cookies and so on<br>
     * </blockquote>
     * <p>
     * Note: If this flag is not set in the system property, then we get no audit data for the web request
     *
     * */
    public static final String WEB_AUDIT_FLAG = "org.jboss.security.web.audit";
    public static final String WEB_AUDIT_FLAG_DEFAULT = "headers,parameters";

    private static final String auditFlag = getSystemPropertySafe(WEB_AUDIT_FLAG, WEB_AUDIT_FLAG_DEFAULT);

    /**
     * System Property to configure mask of cookies, headers, parameters and attributes.
     * Comma separated list of names to be omitted from display in audit log/debug messages.
     */
    public static final String WEB_AUDIT_MASK = "org.jboss.security.web.audit.mask";
    public static final String WEB_AUDIT_MASK_DEFAULT = "j_password,authorization";

    private static final String auditMask = getSystemPropertySafe(WEB_AUDIT_MASK, WEB_AUDIT_MASK_DEFAULT);

    /**
     * Obtain audit/debug information from the servlet request object
     *
     * @param httpRequest to be formatted
     * @return formatted {@link String} of audit logging info {@see WEB_AUDIT_FLAG}
     */
    public static String deriveUsefulInfo(HttpServletRequest httpRequest) {

        String[] mask = auditMask.split(",");

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(httpRequest.getContextPath());
        // Append cookies
        if (auditFlag.contains("cookies")) {
            sb.append(":cookies=");
            int i = 0;
            sb.append("[");
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for(Cookie cookie: cookies) {
                    if (! contains(cookie.getName(), mask)) {
                        if (i++ > 0)
                            sb.append(",");
                        sb.append(cookie.toString());
                    }
                }
            }
            sb.append("]");
        }
        // Append Header information
        if (auditFlag.contains("headers")) {
            sb.append(":headers=");
            int i = 0;
            sb.append("[");
            Enumeration<String> en = httpRequest.getHeaderNames();
            if (en != null) {
                while(en.hasMoreElements()) {
                    String headerName = en.nextElement();
                    // Ensure HTTP Basic Password is not logged by adding "authorization" to WEB_AUDIT_MASK via configuring System Property org.jboss.security.web.audit.mask
                    if (!contains(headerName, mask)) {
                        if (i++ > 0)
                            sb.append(",");
                        sb.append(headerName).append("=").append(httpRequest.getHeader(headerName));
                    }
                }
            }
            sb.append("]");
        }
        // Append Request parameter information
        if (auditFlag.contains("parameters")) {
            sb.append(":parameters=");
            int i = 0;
            sb.append("[");
            Enumeration<String> enparam = httpRequest.getParameterNames();
            if (enparam != null) {
                while(enparam.hasMoreElements()) {
                    String paramName = enparam.nextElement();
                    // check if paramName is not banned by mask
                    if (!contains(paramName, mask)) {
                        if (i++ > 0)
                            sb.append(",");
                        sb.append(paramName).append("=");
                        String[] paramValues = httpRequest.getParameterValues(paramName);
                        int j = 0;
                        for (String v: paramValues) {
                            if (j++ > 0)
                                sb.append("::");
                            sb.append(v);
                        }
                    }
                }
            }
            sb.append("]");
        }
        // Append Request attribute information
        if (auditFlag.contains("attributes")) {
            sb.append(":attributes=");
            int i = 0;
            sb.append("[");
            Enumeration<String> enu = httpRequest.getAttributeNames();
            if (enu != null) {
                while(enu.hasMoreElements()) {
                    String attrName = enu.nextElement();
                    // check if attrName is not banned by mask
                    if (!contains(attrName, mask)) {
                        if (i++ > 0)
                            sb.append(",");
                        sb.append(attrName).append("=").append(httpRequest.getAttribute(attrName));
                    }
                }
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String getSystemPropertySafe(final String key, final String def) {
        return getSecurityManager() == null ? getProperty(key, def)
                : doPrivileged(new PrivilegedAction<String>() {
                    @Override
                    public String run() {
                        return getProperty(key, def);
                    }
                });
    }

    /**
     * Check whether name contains any element of mask
     *
     * @param name non-null {@link String} to check
     * @param mask array of String to check name against
     * @return <code>true</code> if name contains any {@link String} in mask, otherwise <code>false</code>
     */
    private static boolean contains(String name, String[] mask) {
        for (String m: mask) {
            if (name.contains(m)) {
                return true;
            }
        }
        return false;
    }

}
