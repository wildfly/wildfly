/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General License for more details.
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
public interface WithPropertiesMBean {

    AtomicBoolean getAtomicBoolean();

    AtomicInteger getAtomicInteger();

    AtomicLong getAtomicLong();

    BigDecimal getBigDecimal();

    BigInteger getBigInteger();

    boolean getBoolean();

    boolean[] getBooleanArray();

    byte getByte();

    byte[] getByteArray();

    char getChar();

    char[] getCharacterArray();

    Class[] getClassArray();

    Class getClazz();

    Document getDocument();

    double getDouble();

    Element getElement();

    File getFile();

    float getFloat();

    float[] getFloatArray();

    InetAddress getInetAddress();

    InetAddress[] getInetAddressArray();

    int getInteger();

    int[] getIntegerArray();

    Locale getLocale();

    long getLong();

    long[] getLongArray();

    Boolean getObjectBoolean();

    Byte getObjectByte();

    Character getObjectChar();

    Double getObjectDouble();

    Float getObjectFloat();

    Integer getObjectInteger();

    Long getObjectLong();

    Short getObjectShort();

    Properties getProperties();

    short getShort();

    short[] getShortArray();

    String[] getStringArray();

    URI getURI();

    URL getURL();

    void setAtomicBoolean(AtomicBoolean b);

    void setAtomicInteger(AtomicInteger b);

    void setAtomicLong(AtomicLong b);

    void setBigDecimal(BigDecimal bd);

    void setBigInteger(BigInteger bigInteger);

    void setBoolean(boolean b);

    void setBooleanArray(boolean[] b);

    void setByte(byte _byte);

    void setByteArray(byte[] b);

    void setChar(char _char);

    void setCharacterArray(char[] b);

    void setClassArray(Class[] b);

    // cant override final method from Object :)
    void setClazz(Class b);

    void setDocument(Document b);

    void setDouble(double _double);

    void setElement(Element b);

    void setFile(File b);

    void setFloat(float _float);

    void setFloatArray(float[] b);

    void setInetAddress(InetAddress b);

    void setInetAddressArray(InetAddress[] b);

    void setInteger(int _integer);

    void setIntegerArray(int[] b);

    void setLocale(Locale b);

    void setLong(long _long);

    void setLongArray(long[] b);

    void setObjectBoolean(Boolean objectBoolean);

    void setObjectByte(Byte objectByte);

    void setObjectChar(Character objectChar);

    void setObjectDouble(Double objectDouble);

    void setObjectFloat(Float objectFloat);

    void setObjectInteger(Integer objectInteger);

    void setObjectLong(Long objectLong);

    void setObjectShort(Short objectShort);

    void setProperties(Properties b);

    void setShort(short _short);

    void setShortArray(short[] b);

    void setStringArray(String[] b);

    void setURI(URI b);

    void setURL(URL b);

    void start() throws Exception;

    void stop() throws Exception;

}
