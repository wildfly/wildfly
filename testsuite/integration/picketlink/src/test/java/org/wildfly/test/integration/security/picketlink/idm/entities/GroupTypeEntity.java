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

import org.picketlink.idm.jpa.annotations.AttributeValue;
import org.picketlink.idm.jpa.annotations.entity.IdentityManaged;
import org.picketlink.idm.model.basic.Group;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * @author pedroigor
 */
@IdentityManaged(Group.class)
@Entity
public class GroupTypeEntity extends IdentityTypeEntity {

    private static final long serialVersionUID = 158403858486164771L;

    @AttributeValue
    private String name;

    @AttributeValue
    private String path;

    @ManyToOne
    @AttributeValue(name = "parentGroup")
    private GroupTypeEntity parent;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public GroupTypeEntity getParent() {
        return parent;
    }

    public void setParent(GroupTypeEntity parent) {
        this.parent = parent;
    }
}
