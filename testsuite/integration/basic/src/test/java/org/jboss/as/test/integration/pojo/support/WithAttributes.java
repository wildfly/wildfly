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

package org.jboss.as.test.integration.pojo.support;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author baranowb
 * 
 */
public class WithAttributes {
    private Boolean _boolean;
    private Byte _byte;
    private Character _char;
    private Double _double;
    private Float _float;
    private Long _long;
    private Integer _integer;
    private Short _short;

    private BigDecimal bigDecimal;
    private BigInteger bigInteger;

    private AtomicBoolean atomicBoolean;
    private AtomicInteger atomicInteger;
    private AtomicLong atomicLong;

    private Date date;
    private File file;
    private InetAddress inetAddress;
    private Locale locale;
    private Properties properties;
    private URI uri;
    private URL url;

    /**
     * 
     */
    public WithAttributes() {
        // TODO Auto-generated constructor stub
    }

    public void setBoolean(Boolean b) {
        this._boolean = b;

    }

    public Boolean getBoolean() {
        return this._boolean;
    }

    public Byte getByte() {
        return _byte;
    }

    public void setByte(Byte _byte) {
        this._byte = _byte;
    }

    public Character getChar() {
        return _char;
    }

    public void setChar(Character _char) {
        this._char = _char;
    }

    public Double getDouble() {
        return _double;
    }

    public void setDouble(Double _double) {
        this._double = _double;
    }

    public Float getFloat() {
        return _float;
    }

    public void setFloat(Float _float) {
        this._float = _float;
    }

    public Long getLong() {
        return _long;
    }

    public void setLong(Long _long) {
        this._long = _long;
    }

    public Integer getInteger() {
        return _integer;
    }

    public void setInteger(Integer _integer) {
        this._integer = _integer;
    }

    public Short getShort() {
        return _short;
    }

    public void setShort(Short _short) {
        this._short = _short;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    public BigDecimal getBigDecimal() {
        return this.bigDecimal;
    }

    public void setBigDecimal(BigDecimal bd) {
        this.bigDecimal = bd;
    }

    public AtomicBoolean getAtomicBoolean() {
        return atomicBoolean;
    }

    public void setAtomicBoolean(AtomicBoolean atomicBoolean) {
        this.atomicBoolean = atomicBoolean;
    }

    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }

    public void setAtomicInteger(AtomicInteger atomicInteger) {
        this.atomicInteger = atomicInteger;
    }

    public AtomicLong getAtomicLong() {
        return atomicLong;
    }

    public void setAtomicLong(AtomicLong atomicLong) {
        this.atomicLong = atomicLong;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public InetAddress getInetAddress() {
        return inetAddress;
    }

    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public URI getURI() {
        return uri;
    }

    public void setURI(URI uri) {
        this.uri = uri;
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }
}