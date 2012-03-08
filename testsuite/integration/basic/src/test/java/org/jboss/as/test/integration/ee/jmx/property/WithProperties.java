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

package org.jboss.as.test.integration.ee.jmx.property;

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
public class WithProperties implements WithPropertiesMBean {

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
    public WithProperties() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public void setBoolean(Boolean b) {
        this._boolean = b;

    }

    @Override
    public Boolean getBoolean() {
        return this._boolean;
    }

    @Override
    public Byte getByte() {
        return _byte;
    }

    @Override
    public void setByte(Byte _byte) {
        this._byte = _byte;
    }

    @Override
    public Character getChar() {
        return _char;
    }

    @Override
    public void setChar(Character _char) {
        this._char = _char;
    }

    @Override
    public Double getDouble() {
        return _double;
    }

    @Override
    public void setDouble(Double _double) {
        this._double = _double;
    }

    @Override
    public Float getFloat() {
        return _float;
    }

    @Override
    public void setFloat(Float _float) {
        this._float = _float;
    }

    @Override
    public Long getLong() {
        return _long;
    }

    @Override
    public void setLong(Long _long) {
        this._long = _long;
    }

    @Override
    public Integer getInteger() {
        return _integer;
    }

    @Override
    public void setInteger(Integer _integer) {
        this._integer = _integer;
    }

    @Override
    public Short getShort() {
        return _short;
    }

    @Override
    public void setShort(Short _short) {
        this._short = _short;
    }

    @Override
    public BigInteger getBigInteger() {
        return bigInteger;
    }

    @Override
    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    @Override
    public BigDecimal getBigDecimal() {
        return this.bigDecimal;
    }

    @Override
    public void setBigDecimal(BigDecimal bd) {
        this.bigDecimal = bd;
    }

    @Override
    public AtomicBoolean getAtomicBoolean() {
        return atomicBoolean;
    }

    @Override
    public void setAtomicBoolean(AtomicBoolean atomicBoolean) {
        this.atomicBoolean = atomicBoolean;
    }

    @Override
    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }

    @Override
    public void setAtomicInteger(AtomicInteger atomicInteger) {
        this.atomicInteger = atomicInteger;
    }

    @Override
    public AtomicLong getAtomicLong() {
        return atomicLong;
    }

    @Override
    public void setAtomicLong(AtomicLong atomicLong) {
        this.atomicLong = atomicLong;
    }

    @Override
    public Date getDate() {
        return date;
    }

    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    @Override
    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public void setURI(URI uri) {
        this.uri = uri;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public void setURL(URL url) {
        this.url = url;
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

}