/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.security.common.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jboss.logging.Logger;

/**
 * This servlet prints value of an request/session attribute. It supports also nested values for {@link Map} and {@link List}
 * instances - names in such case are delimited by {@value #DELIMITER}.
 * <p>
 * The servlet supports 2 parameters:
 * <ul>
 * <li>{@value #PARAM_ATTR_NAME} - name of attribute to retrieve. Default value is {@value #DEFAULT_PROPERTY_NAME}</li>
 * <li>{@value #PARAM_SCOPE_NAME} - scope from which the value should be retrieved. Supported vaules are "session" and
 * "request". Default is {@value #DEFAULT_SCOPE_NAME}</li>
 * </ul>
 * <p>
 * For instance the Picketlink's handler SAML2AttributeHandler stores a map with attributes received in SAML assertion to an
 * {@link HttpSession} as attribute named "SESSION_ATTRIBUTE_MAP". Each entry in this map is represented by a list of values. So
 * if you want to read the first value of SAML assertion attribute named "cn" from the session, use the parameter
 * "attrib=SESSION_ATTRIBUTE_MAP/cn/0"
 *
 * @author Josef Cacek
 */
@WebServlet(PrintAttributeServlet.SERVLET_PATH)
public class PrintAttributeServlet extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(PrintAttributeServlet.class);

    private static final long serialVersionUID = 1L;

    public static final String SERVLET_PATH = "/PrintAttrServlet";
    public static final String PARAM_ATTR_NAME = "attr";
    public static final String PARAM_SCOPE_NAME = "scope";
    public static final String DELIMITER = "/";
    public static final String DEFAULT_PROPERTY_NAME = "SESSION_ATTRIBUTE_MAP" + DELIMITER + "cn" + DELIMITER + "0";

    public static final String SCOPE_REQUEST = "request";
    public static final String SCOPE_SESSION = "session";
    public static final String DEFAULT_SCOPE_NAME = SCOPE_SESSION;

    @SuppressWarnings("rawtypes")
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        String attrName = req.getParameter(PARAM_ATTR_NAME);
        if (attrName == null || attrName.length() == 0) {
            attrName = DEFAULT_PROPERTY_NAME;
        }
        StringTokenizer st = new StringTokenizer(attrName, DELIMITER);
        String actualName = st.nextToken();
        Object actualValue = null;
        if (SCOPE_REQUEST.equalsIgnoreCase(req.getParameter(PARAM_SCOPE_NAME))) {
            actualValue = req.getAttribute(actualName);
        } else {
            final HttpSession session = req.getSession(false);
            if (session != null)
                actualValue = session.getAttribute(actualName);
        }

        while (st.hasMoreTokens() && (actualValue instanceof Map || actualValue instanceof List)) {
            actualName = st.nextToken();
            if (actualValue instanceof List) {
                try {
                    actualValue = ((List) actualValue).get(Integer.parseInt(actualName));
                } catch (Exception e) {
                    LOGGER.warn("Unable to retrieve value from list", e);
                    actualValue = null;
                }
            } else {
                actualValue = ((Map) actualValue).get(actualName);
            }
        }

        final PrintWriter writer = resp.getWriter();
        writer.write(String.valueOf(actualValue));
        writer.close();
    }
}
