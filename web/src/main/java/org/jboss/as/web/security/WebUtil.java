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

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

/**
 * Provides utility static methods for the web security integration
 *
 * @author <a href="mailto:Anil.Saldhana@jboss.org">Anil Saldhana</a>
 */
public class WebUtil {
    /**
     * Obtain debug information from the servlet request object
     *
     * @param httpRequest
     * @return
     */
    public static String deriveUsefulInfo(HttpServletRequest httpRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(httpRequest.getContextPath());
        sb.append(":cookies=").append(httpRequest.getCookies()).append(":headers=");
        // Append Header information
        Enumeration<?> en = httpRequest.getHeaderNames();
        while (en.hasMoreElements()) {
            String headerName = (String) en.nextElement();
            sb.append(headerName).append("=");
            // Ensure HTTP Basic Password is not logged
            if (headerName.contains("authorization") == false)
                sb.append(httpRequest.getHeader(headerName)).append(",");
        }
        sb.append("]");
        // Append Request parameter information
        sb.append("[parameters=");
        Enumeration<?> enparam = httpRequest.getParameterNames();
        while (enparam.hasMoreElements()) {
            String paramName = (String) enparam.nextElement();
            String[] paramValues = httpRequest.getParameterValues(paramName);
            int len = paramValues != null ? paramValues.length : 0;
            for (int i = 0; i < len; i++)
                sb.append(paramValues[i]).append("::");
            sb.append(",");
        }
        sb.append("][attributes=");
        // Append Request attribute information
        Enumeration<?> enu = httpRequest.getAttributeNames();
        while (enu.hasMoreElements()) {
            String attrName = (String) enu.nextElement();
            sb.append(attrName).append("=");
            sb.append(httpRequest.getAttribute(attrName)).append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
