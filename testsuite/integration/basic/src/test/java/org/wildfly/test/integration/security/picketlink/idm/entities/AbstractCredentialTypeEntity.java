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

import org.picketlink.idm.jpa.annotations.CredentialClass;
import org.picketlink.idm.jpa.annotations.EffectiveDate;
import org.picketlink.idm.jpa.annotations.ExpiryDate;
import org.picketlink.idm.jpa.annotations.OwnerReference;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

/**
 * <p> Base mapping for credentials. </p>
 *
 * @author pedroigor
 */
@MappedSuperclass
public abstract class AbstractCredentialTypeEntity implements Serializable {

    private static final long serialVersionUID = -8032908635337756851L;

    @Id
    @GeneratedValue
    private Long id;

    @OwnerReference
    @ManyToOne
    private AttributedTypeEntity owner;

    @CredentialClass
    private String typeName;

    @EffectiveDate
    private Date effectiveDate;

    @ExpiryDate
    private Date expiryDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttributedTypeEntity getOwner() {
        return owner;
    }

    public void setOwner(AttributedTypeEntity owner) {
        this.owner = owner;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
}
