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
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author baranowb
 *
 */
public class WithProperties implements WithPropertiesMBean {

    // those will have default values on get op, so test must set it to non def.
    private boolean _boolean;
    private byte _byte;
    private char _char;
    private double _double;
    private float _float;
    private int _integer;
    private long _long;
    private short _short;

    private AtomicBoolean atomicBoolean;
    private AtomicInteger atomicInteger;
    private AtomicLong atomicLong;
    private BigDecimal bigDecimal;

    private BigInteger bigInteger;
    private boolean[] booleanArray;
    private byte[] byteArray;
    private char[] characterArray;
    private Class[] classArray;
    private Class clazz;
    private Document document;
    private Element element;
    private File file;
    private float[] floatArray;

    private InetAddress inetAddress;
    private InetAddress[] inetAddressArray;

    private int[] integerArray;
    private Locale locale;
    private long[] longArray;
    private Boolean objectBoolean;
    private Byte objectByte;
    private Character objectChar;
    private Double objectDouble;
    private Float objectFloat;
    private Integer objectInteger;
    private Long objectLong;
    private Short objectShort;
    private Properties properties;
    private short[] shortArray;
    private String[] stringArray;
    private URI uri;
    private URL url;

    public WithProperties() {

    }

    @Override
    public AtomicBoolean getAtomicBoolean() {
        return atomicBoolean;
    }

    @Override
    public AtomicInteger getAtomicInteger() {
        return atomicInteger;
    }

    @Override
    public AtomicLong getAtomicLong() {
        return atomicLong;
    }

    @Override
    public BigDecimal getBigDecimal() {
        return this.bigDecimal;
    }

    @Override
    public BigInteger getBigInteger() {
        return bigInteger;
    }

    @Override
    public boolean getBoolean() {
        return this._boolean;
    }

    @Override
    public boolean[] getBooleanArray() {
        return booleanArray;
    }

    @Override
    public byte getByte() {
        return _byte;
    }

    @Override
    public byte[] getByteArray() {
        return this.byteArray;
    }

    @Override
    public char getChar() {
        return _char;
    }

    @Override
    public char[] getCharacterArray() {
        return this.characterArray;
    }

    @Override
    public Class[] getClassArray() {
        return this.classArray;
    }

    @Override
    public Class getClazz() {
        return this.clazz;
    }

    @Override
    public Document getDocument() {
        return this.document;
    }

    @Override
    public double getDouble() {
        return _double;
    }

    @Override
    public Element getElement() {
        return this.element;
    }

    @Override
    public File getFile() {
        return this.file;
    }

    @Override
    public float getFloat() {
        return _float;
    }

    @Override
    public float[] getFloatArray() {
        return this.floatArray;
    }

    @Override
    public InetAddress getInetAddress() {
        return this.inetAddress;
    }

    @Override
    public InetAddress[] getInetAddressArray() {
        return this.inetAddressArray;
    }

    @Override
    public int getInteger() {
        return _integer;
    }

    @Override
    public int[] getIntegerArray() {
        return this.integerArray;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public long getLong() {
        return _long;
    }

    @Override
    public long[] getLongArray() {
        return this.longArray;
    }

    @Override
    public Boolean getObjectBoolean() {
        return objectBoolean;
    }

    @Override
    public Byte getObjectByte() {
        return objectByte;
    }

    @Override
    public Character getObjectChar() {
        return objectChar;
    }

    @Override
    public Double getObjectDouble() {
        return objectDouble;
    }

    @Override
    public Float getObjectFloat() {
        return objectFloat;
    }

    @Override
    public Integer getObjectInteger() {
        return objectInteger;
    }

    @Override
    public Long getObjectLong() {
        return objectLong;
    }

    @Override
    public Short getObjectShort() {
        return objectShort;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public short getShort() {
        return _short;
    }

    @Override
    public short[] getShortArray() {
        return this.shortArray;
    }

    @Override
    public String[] getStringArray() {
        return this.stringArray;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public URL getURL() {
        return this.url;
    }

    @Override
    public void setAtomicBoolean(AtomicBoolean atomicBoolean) {
        this.atomicBoolean = atomicBoolean;
    }

    @Override
    public void setAtomicInteger(AtomicInteger atomicInteger) {
        this.atomicInteger = atomicInteger;
    }

    @Override
    public void setAtomicLong(AtomicLong atomicLong) {
        this.atomicLong = atomicLong;
    }

    @Override
    public void setBigDecimal(BigDecimal bd) {
        this.bigDecimal = bd;
    }

    @Override
    public void setBigInteger(BigInteger bigInteger) {
        this.bigInteger = bigInteger;
    }

    @Override
    public void setBoolean(boolean b) {
        this._boolean = b;

    }

    @Override
    public void setBooleanArray(boolean[] b) {
        this.booleanArray = b;

    }

    @Override
    public void setByte(byte _byte) {
        this._byte = _byte;
    }

    @Override
    public void setByteArray(byte[] b) {
        this.byteArray = b;
    }

    @Override
    public void setChar(char _char) {
        this._char = _char;
    }

    @Override
    public void setCharacterArray(char[] b) {
        this.characterArray = b;
    }

    @Override
    public void setClassArray(Class[] b) {
        this.classArray = b;
    }

    @Override
    public void setClazz(Class b) {
        this.clazz = b;
    }

    @Override
    public void setDocument(Document b) {
        this.document = b;
    }

    @Override
    public void setDouble(double _double) {
        this._double = _double;
    }

    @Override
    public void setElement(Element b) {
        this.element = b;
    }

    @Override
    public void setFile(File b) {
        this.file = b;

    }

    @Override
    public void setFloat(float _float) {
        this._float = _float;
    }

    @Override
    public void setFloatArray(float[] b) {
        this.floatArray = b;

    }

    @Override
    public void setInetAddress(InetAddress b) {
        this.inetAddress =b;

    }

    @Override
    public void setInetAddressArray(InetAddress[] b) {
        this.inetAddressArray = b;
    }

    @Override
    public void setInteger(int _integer) {
        this._integer = _integer;
    }

    @Override
    public void setIntegerArray(int[] b) {
        this.integerArray = b;
    }

    @Override
    public void setLocale(Locale b) {
        this.locale =b;
    }

    @Override
    public void setLong(long _long) {
        this._long = _long;
    }

    @Override
    public void setLongArray(long[] b) {
        this.longArray = b;
    }

    @Override
    public void setObjectBoolean(Boolean objectBoolean) {
        this.objectBoolean = objectBoolean;
    }

    @Override
    public void setObjectByte(Byte objectByte) {
        this.objectByte = objectByte;
    }

    @Override
    public void setObjectChar(Character objectChar) {
        this.objectChar = objectChar;
    }

    @Override
    public void setObjectDouble(Double objectDouble) {
        this.objectDouble = objectDouble;
    }

    @Override
    public void setObjectFloat(Float objectFloat) {
        this.objectFloat = objectFloat;
    }

    @Override
    public void setObjectInteger(Integer objectInteger) {
        this.objectInteger = objectInteger;
    }

    @Override
    public void setObjectLong(Long objectLong) {
        this.objectLong = objectLong;
    }

    @Override
    public void setObjectShort(Short objectShort) {
        this.objectShort = objectShort;
    }

    @Override
    public void setProperties(Properties b) {
        this.properties = b;
    }

    @Override
    public void setShort(short _short) {
        this._short = _short;
    }

    @Override
    public void setShortArray(short[] b) {
        this.shortArray = b;

    }

    @Override
    public void setStringArray(String[] b) {
        this.stringArray = b;

    }

    @Override
    public void setURI(URI b) {
        this.uri = b;
    }

    @Override
    public void setURL(URL b) {
        this.url = b;

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

}
