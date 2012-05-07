/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security.state;

import org.jboss.as.domain.management.security.AddPropertiesUser;
import org.jboss.as.domain.management.security.AddPropertiesUser.Interactiveness;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
* Place holder object to pass between the state
*
* @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
*/
public class StateValues {
    private AddPropertiesUser.Interactiveness howInteractive = AddPropertiesUser.Interactiveness.INTERACTIVE;
    private String realm;
    private String userName;
    private char[] password;
    private boolean management;
    private String roles;
    private boolean existingUser = false;
    private List<File> propertiesFiles;
    private List<File> roleFiles;
    private Set<String> knownUsers;
    private Map<String,String> knownRoles;
    private String jbossHome;

    public boolean isSilentOrNonInteractive() {
        return (howInteractive == AddPropertiesUser.Interactiveness.NON_INTERACTIVE) || isSilent();
    }

    public void setHowInteractive(AddPropertiesUser.Interactiveness howInteractive) {
        this.howInteractive = howInteractive;
    }


    public boolean isSilent() {
        return (howInteractive == AddPropertiesUser.Interactiveness.SILENT);
    }

    public boolean isInteractive() {
        return howInteractive == Interactiveness.INTERACTIVE;
    }

    public boolean isExistingUser() {
        return existingUser;
    }

    public void setExistingUser(boolean existingUser) {
        this.existingUser = existingUser;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public boolean isManagement() {
        return management;
    }

    public void setManagement(boolean management) {
        this.management = management;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public List<File> getPropertiesFiles() {
        return propertiesFiles;
    }

    public void setPropertiesFiles(List<File> propertiesFiles) {
        this.propertiesFiles = propertiesFiles;
    }

    public List<File> getRoleFiles() {
        return roleFiles;
    }

    public void setRoleFiles(List<File> roleFiles) {
        this.roleFiles = roleFiles;
    }

    public Set<String> getKnownUsers() {
        return knownUsers;
    }

    public void setKnownUsers(Set<String> knownUsers) {
        this.knownUsers = knownUsers;
    }

    public Map<String, String> getKnownRoles() {
        return knownRoles;
    }

    public void setKnownRoles(Map<String, String> knownRoles) {
        this.knownRoles = knownRoles;
    }

    public String getJBossHome() {
        return this.jbossHome;
    }

    public void setJbossHome(String path) {
        this.jbossHome = path;
    }
}
