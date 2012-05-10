/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_TABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SIMPLE_SELECT_USERNAME_FIELD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SQL_SELECT_ROLES;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.IOException;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.domain.management.connections.ConnectionManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Abstract resource definition for database authorization and authentication resource.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseSubjectSupplemental implements Service<SubjectSupplementalService>, SubjectSupplementalService,
        SubjectSupplemental {

    private final InjectedValue<ConnectionManager> connectionManager = new InjectedValue<ConnectionManager>();

    public static final String SERVICE_SUFFIX = "database_authorization";
    private static final String COMMA = ",";

    private String table;
    private String sqlStatement;

    public DatabaseSubjectSupplemental(String realmName, ModelNode database) {
        if (database.hasDefined(SIMPLE_SELECT_ROLES)) {
            table = database.require(SIMPLE_SELECT_TABLE).asString() ;
            String userNameField = database.require(SIMPLE_SELECT_USERNAME_FIELD).asString() ;
            String userRolesField = database.require(ROLES_FIELD).asString();
            sqlStatement = String.format("select %s from %s where %s=?",userRolesField,table,userNameField);
        } else if (database.hasDefined(SQL_SELECT_ROLES)) {
            sqlStatement = database.require(SQL_SELECT_ROLES).asString();
        }
    }

    /*
     * Service Methods
     */

    public SubjectSupplementalService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    /*
     * SubjectSupplementalService Method
     */

    public SubjectSupplemental getSubjectSupplemental(Map<String, Object> sharedState) {
        return this;
    }

    /*
     *  Access to Injectors
     */

    public InjectedValue<ConnectionManager> getConnectionManagerInjector() {
        return connectionManager;
    }


    /*
     * SubjectSupplementalMethods
     */

    /**
     * @see org.jboss.as.domain.management.security.SubjectSupplemental#supplementSubject(javax.security.auth.Subject)
     */
    public void supplementSubject(Subject subject) throws IOException {
        Set<RealmUser> users = subject.getPrincipals(RealmUser.class);
        Set<Principal> principals = subject.getPrincipals();
        for (RealmUser current : users) {
            principals.addAll(getRoles(this.getConnectionManagerInjector().getValue(), current.getName()));
        }
    }

    private Set<RealmRole> getRoles(ConnectionManager connectionManager, String userName) throws IOException {
        Set<RealmRole> response = Collections.emptySet();
        Connection dbc = null;
        ResultSet rs = null;
        try {
            dbc = (Connection) connectionManager.getConnection();

            PreparedStatement preparedStatement = dbc.prepareStatement(sqlStatement, ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
            preparedStatement.setString(1,userName);
            rs = preparedStatement.executeQuery();
            response = new HashSet<RealmRole>();
            while (rs.next()) {
                String rolesString = rs.getString(1);
                String[] roles = rolesString.split(COMMA);
                for (String current : roles) {
                    String cleaned = current.trim();
                    if (cleaned.length() > 0) {
                        response.add(new RealmRole(cleaned));
                    }
                }
            }
        } catch (Exception e) {
            throw MESSAGES.cannotPerformVerification(e);
        } finally {
            try {
                closeSafely(rs, dbc);
            } catch (SQLException e) {
                throw MESSAGES.closeSafelyException(e);
            }
        }
        return response;
    }

    private void closeSafely(ResultSet rs, Connection dbc) throws SQLException {
        if (rs != null) {
            rs.close();
        }

        if (dbc != null) {
            dbc.close();
        }
    }

}
