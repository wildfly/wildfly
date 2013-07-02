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

package org.jboss.as.domain.management.security.adduser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.domain.management.security.adduser.AddUser.FileMode;
import org.jboss.as.domain.management.security.adduser.AddUser.Interactiveness;
import org.jboss.as.domain.management.security.adduser.AddUser.RealmMode;

/**
* Place holder object to pass between the state
*
* @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
*/
public class StateValues {
    private final RuntimeOptions options;
    private AddUser.Interactiveness howInteractive = Interactiveness.INTERACTIVE;
    private AddUser.RealmMode realmMode = RealmMode.DEFAULT;
    private String realm;
    private String userName;
    private char[] password;
    private AddUser.FileMode fileMode = FileMode.UNDEFINED;
    private String roles;
    private boolean existingUser = false;
    private List<File> userFiles;
    private List<File> roleFiles;
    private Set<String> knownUsers;
    private Map<String,String> knownRoles;

    public StateValues() {
        options = new RuntimeOptions();
    }

    public StateValues(final RuntimeOptions options) {
        this.options = options;
    }

    public boolean isSilentOrNonInteractive() {
        return (howInteractive == AddUser.Interactiveness.NON_INTERACTIVE) || isSilent();
    }

    public void setHowInteractive(AddUser.Interactiveness howInteractive) {
        this.howInteractive = howInteractive;
    }


    public boolean isSilent() {
        return (howInteractive == AddUser.Interactiveness.SILENT);
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

    public RealmMode getRealmMode() {
        return realmMode;
    }

    public void setRealmMode(final RealmMode realmMode) {
        this.realmMode = realmMode;
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

    public AddUser.FileMode getFileMode() {
        return fileMode;
    }

    public void setFileMode(AddUser.FileMode fileMode) {
        this.fileMode = fileMode;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public List<File> getUserFiles() {
        return userFiles;
    }

    public void setUserFiles(List<File> userFiles) {
        this.userFiles = userFiles;
    }

    public List<File> getRoleFiles() {
        return roleFiles;
    }

    public void setRoleFiles(List<File> roleFiles) {
        this.roleFiles = roleFiles;
    }

    public boolean rolePropertiesFound() {
        return roleFiles != null && roleFiles.size() > 0;
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

    public RuntimeOptions getOptions() {
        return options;
    }

}
