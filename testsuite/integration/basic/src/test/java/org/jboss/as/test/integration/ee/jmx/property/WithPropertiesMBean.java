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
public interface WithPropertiesMBean {

    public AtomicBoolean getAtomicBoolean();

    public AtomicInteger getAtomicInteger();

    public AtomicLong getAtomicLong();

    public BigDecimal getBigDecimal();

    public BigInteger getBigInteger();

    public boolean getBoolean();

    public boolean[] getBooleanArray();

    public byte getByte();

    public byte[] getByteArray();

    public char getChar();

    public char[] getCharacterArray();

    public Class[] getClassArray();

    public Class getClazz();

    public Document getDocument();

    public double getDouble();

    public Element getElement();

    public File getFile();

    public float getFloat();

    public float[] getFloatArray();

    public InetAddress getInetAddress();

    public InetAddress[] getInetAddressArray();

    public int getInteger();

    public int[] getIntegerArray();

    public Locale getLocale();

    public long getLong();

    public long[] getLongArray();

    public Boolean getObjectBoolean();

    public Byte getObjectByte();

    public Character getObjectChar();

    public Double getObjectDouble();

    public Float getObjectFloat();

    public Integer getObjectInteger();

    public Long getObjectLong();

    public Short getObjectShort();

    public Properties getProperties();

    public short getShort();

    public short[] getShortArray();

    public String[] getStringArray();

    public URI getURI();

    public URL getURL();

    public void setAtomicBoolean(AtomicBoolean b);

    public void setAtomicInteger(AtomicInteger b);

    public void setAtomicLong(AtomicLong b);

    public void setBigDecimal(BigDecimal bd);

    public void setBigInteger(BigInteger bigInteger);

    public void setBoolean(boolean b);

    public void setBooleanArray(boolean[] b);

    public void setByte(byte _byte);

    public void setByteArray(byte[] b);

    public void setChar(char _char);

    public void setCharacterArray(char[] b);

    public void setClassArray(Class[] b);

    // cant override final method from Object :)
    public void setClazz(Class b);

    public void setDocument(Document b);

    public void setDouble(double _double);

    public void setElement(Element b);

    public void setFile(File b);

    public void setFloat(float _float);

    public void setFloatArray(float[] b);

    public void setInetAddress(InetAddress b);

    public void setInetAddressArray(InetAddress[] b);

    public void setInteger(int _integer);

    public void setIntegerArray(int[] b);

    public void setLocale(Locale b);

    public void setLong(long _long);

    public void setLongArray(long[] b);

    public void setObjectBoolean(Boolean objectBoolean);

    public void setObjectByte(Byte objectByte);

    public void setObjectChar(Character objectChar);

    public void setObjectDouble(Double objectDouble);

    public void setObjectFloat(Float objectFloat);

    public void setObjectInteger(Integer objectInteger);

    public void setObjectLong(Long objectLong);

    public void setObjectShort(Short objectShort);

    public void setProperties(Properties b);

    public void setShort(short _short);

    public void setShortArray(short[] b);

    public void setStringArray(String[] b);

    public void setURI(URI b);

    public void setURL(URL b);

    public void start() throws Exception;

    public void stop() throws Exception;

}
