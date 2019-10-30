/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.web.security;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.security.SecurityConstants;
import org.jboss.security.identity.Role;
import org.jboss.security.identity.RoleGroup;
import org.jboss.security.identity.plugins.SimpleRoleGroup;
import org.jboss.security.mapping.MappingContext;
import org.jboss.security.mapping.MappingManager;
import org.jboss.security.mapping.MappingResult;
import org.jboss.security.mapping.MappingType;
import org.picketbox.factories.SecurityFactory;

/**
 * <code>RoleMappingSecuredServlet</code> prints out the mapping roles of current principal.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@WebServlet(name = "RoleMappingSecuredServlet", urlPatterns = { "/rolemapping-secured/" }, loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed = { "gooduser" }))
public class RoleMappingSecuredServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Writer writer = resp.getWriter();
        RoleGroup mappedObject = new SimpleRoleGroup("Roles");
        MappingManager mm = SecurityFactory.getMappingManager(WebSimpleRoleMappingSecurityDomainSetup.WEB_SECURITY_DOMAIN);
        MappingContext<Object> mc = mm.getMappingContext(MappingType.ROLE.name());
        Map<String, Object> contextMap = new HashMap<String, Object>();
        contextMap.put(SecurityConstants.PRINCIPAL_IDENTIFIER, req.getUserPrincipal());
        mc.performMapping(contextMap, mappedObject);
        MappingResult<Object> mappingResult = mc.getMappingResult();
        mappedObject = (RoleGroup) mappingResult.getMappedObject();
        if (mappedObject != null) {
            List<Role> roles = new ArrayList<>(mappedObject.getRoles());
            Collections.sort(roles, new Comparator<Role>() {
                @Override
                public int compare(Role o1, Role o2) {
                    return o1.getRoleName().compareTo(o2.getRoleName());
                }
            });
            String output = String.join(":", roles.stream().map(r -> r.getRoleName()).collect(Collectors.toList()));
            writer.write(output);
        }
    }
}
