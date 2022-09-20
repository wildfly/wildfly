/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.weld.resource;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Named;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
@Named("envEntry")
@ApplicationScoped
public class EnvEntryInjectionBean {

    @Resource(name = "unmappedEnvEntry")
    private Integer unmappedEnvEntry = 1;

    @Resource(name = "mappedEnvEntry")
    private Integer mappedEnvEntry = 0;

    @Resource()
    private Integer unnamedMappedEnvEntry = 0;

    private String unannotatedString = "notInjected";
    private boolean unannotatedBoolean = false;
    private char unannotatedChar = 'a';
    private byte unannotatedByte = 0;
    private short unannotatedShort = 0;
    private int unannotatedInt = 0;
    private long unannotatedLong = 0L;
    private float unannotatedFloat = 0;
    private double unannotatedDouble = 0;

    private String unannotatedStringProperty = "notInjected";
    private boolean unannotatedBooleanProperty = false;
    private int unannotatedIntProperty = 0;
    private int unannotatedIntLookup = 0;

    @SuppressWarnings("unused")
    public void setUnannotatedStringProperty(String unannotatedStringProperty) {
        this.unannotatedStringProperty = unannotatedStringProperty;
    }

    @SuppressWarnings("unused")
    public void setUnannotatedBooleanProperty(boolean unannotatedBooleanProperty) {
        this.unannotatedBooleanProperty = unannotatedBooleanProperty;
    }

    @SuppressWarnings("unused")
    public void setUnannotatedIntProperty(int unannotatedIntProperty) {
        this.unannotatedIntProperty = unannotatedIntProperty;
    }

    // Force deployment-time instantiation
    @SuppressWarnings("unused")
    public void xxx(@Observes @Initialized(ApplicationScoped.class) Object context) {
        SimpleEnvEntryResourceInjectionTestCase.setBean(this);
    }

    public Integer getUnmappedEnvEntry() {
        return unmappedEnvEntry;
    }

    public Integer getMappedEnvEntry() {
        return mappedEnvEntry;
    }

    public Integer getUnnamedMappedEnvEntry() {
        return unnamedMappedEnvEntry;
    }

    public String getUnannotatedString() {
        return unannotatedString;
    }

    public boolean isUnannotatedBoolean() {
        return unannotatedBoolean;
    }

    public char getUnannotatedChar() {
        return unannotatedChar;
    }

    public byte getUnannotatedByte() {
        return unannotatedByte;
    }

    public short getUnannotatedShort() {
        return unannotatedShort;
    }

    public int getUnannotatedInt() {
        return unannotatedInt;
    }

    public long getUnannotatedLong() {
        return unannotatedLong;
    }

    public float getUnannotatedFloat() {
        return unannotatedFloat;
    }

    public double getUnannotatedDouble() {
        return unannotatedDouble;
    }

    public String getUnannotatedStringProperty() {
        return unannotatedStringProperty;
    }

    public boolean isUnannotatedBooleanProperty() {
        return unannotatedBooleanProperty;
    }

    public int getUnannotatedIntProperty() {
        return unannotatedIntProperty;
    }

    public int getUnannotatedIntLookup() {
        return unannotatedIntLookup;
    }
}
