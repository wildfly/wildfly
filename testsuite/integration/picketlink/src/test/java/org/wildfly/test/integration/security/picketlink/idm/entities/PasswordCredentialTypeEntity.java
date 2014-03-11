/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.security.picketlink.idm.entities;

import org.picketlink.idm.credential.storage.EncodedPasswordStorage;
import org.picketlink.idm.jpa.annotations.CredentialProperty;
import org.picketlink.idm.jpa.annotations.entity.ManagedCredential;

import javax.persistence.Entity;

/**
 * <p> This entity is mapped to support Password credential types using a {@link org.picketlink.idm.credential.storage.EncodedPasswordStorage}.
 * </p>
 *
 * @author pedroigor
 */
@ManagedCredential(EncodedPasswordStorage.class)
@Entity
public class PasswordCredentialTypeEntity extends AbstractCredentialTypeEntity {

    private static final long serialVersionUID = -1073369096518010570L;

    @CredentialProperty(name = "encodedHash")
    private String passwordEncodedHash;

    @CredentialProperty(name = "salt")
    private String passwordSalt;

    public String getPasswordEncodedHash() {
        return passwordEncodedHash;
    }

    public void setPasswordEncodedHash(String passwordEncodedHash) {
        this.passwordEncodedHash = passwordEncodedHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }
}
