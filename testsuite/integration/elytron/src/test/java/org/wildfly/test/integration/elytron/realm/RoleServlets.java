/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.elytron.realm;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.wildfly.test.integration.elytron.realm.AggregateRealmWithTransformerTestCase.ROLE_1;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmWithTransformerTestCase.ROLE_2;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmWithTransformerTestCase.ROLE_3;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmWithTransformerTestCase.ROLE_4;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmWithTransformerTestCase.ROLE_5;
import static org.wildfly.test.integration.elytron.realm.AggregateRealmWithTransformerTestCase.ROLE_6;

/**
 * These servlets allows access to different roles.
 * These servlets are used in AggregateRealmWithTransformerTestCase.
 */
public class RoleServlets {
    @WebServlet(urlPatterns = { Role1Servlet.SERVLET_PATH })
    @ServletSecurity(@HttpConstraint(rolesAllowed = { ROLE_1 }))
    public static class Role1Servlet extends GeneralRoleServlet {

        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        /** The default servlet path (used in {@link WebServlet} annotation). */
        public static final String SERVLET_PATH = "/" + ROLE_1;
    }

    @WebServlet(urlPatterns = { Role2Servlet.SERVLET_PATH })
    @ServletSecurity(@HttpConstraint(rolesAllowed = { ROLE_2 }))
    public static class Role2Servlet extends GeneralRoleServlet {

        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        /** The default servlet path (used in {@link WebServlet} annotation). */
        public static final String SERVLET_PATH = "/" + ROLE_2;
    }

    @WebServlet(urlPatterns = { Role3Servlet.SERVLET_PATH })
    @ServletSecurity(@HttpConstraint(rolesAllowed = { ROLE_3 }))
    public static class Role3Servlet extends GeneralRoleServlet {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        /** The default servlet path (used in {@link WebServlet} annotation). */
        public static final String SERVLET_PATH = "/" + ROLE_3;
    }

    @WebServlet(urlPatterns = { Role4Servlet.SERVLET_PATH })
    @ServletSecurity(@HttpConstraint(rolesAllowed = { ROLE_4 }))
    public static class Role4Servlet extends GeneralRoleServlet {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        /** The default servlet path (used in {@link WebServlet} annotation). */
        public static final String SERVLET_PATH = "/" + ROLE_4;
    }

    @WebServlet(urlPatterns = { Role5Servlet.SERVLET_PATH })
    @ServletSecurity(@HttpConstraint(rolesAllowed = { ROLE_5 }))
    public static class Role5Servlet extends GeneralRoleServlet {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        /** The default servlet path (used in {@link WebServlet} annotation). */
        public static final String SERVLET_PATH = "/" + ROLE_5;
    }

    @WebServlet(urlPatterns = { Role6Servlet.SERVLET_PATH })
    @ServletSecurity(@HttpConstraint(rolesAllowed = { ROLE_6 }))
    public static class Role6Servlet extends GeneralRoleServlet {
        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        /** The default servlet path (used in {@link WebServlet} annotation). */
        public static final String SERVLET_PATH = "/" + ROLE_6;
    }

    public abstract static class GeneralRoleServlet extends HttpServlet {
        /** Writes plain-text ok response. */
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/plain");
            final PrintWriter writer = resp.getWriter();
            writer.write("ok");
            writer.close();
        }
    }
}
