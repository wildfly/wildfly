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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author baranowb
 * 
 */
public interface WithPropertiesMBean {

    public void setBoolean(Boolean b);

    public Boolean getBoolean();

    public Byte getByte();

    public void setByte(Byte _byte);

    public Character getChar();

    public void setChar(Character _char);

    public Double getDouble();

    public void setDouble(Double _double);

    public Float getFloat();

    public void setFloat(Float _float);

    public Long getLong();

    public void setLong(Long _long);

    public Integer getInteger();

    public void setInteger(Integer _integer);

    public Short getShort();

    public void setShort(Short _short);

    public BigInteger getBigInteger();

    public void setBigInteger(BigInteger bigInteger);

    public void setBigDecimal(BigDecimal bd);

    public BigDecimal getBigDecimal();

    public AtomicBoolean getAtomicBoolean();

    public void setAtomicBoolean(AtomicBoolean b);

    public void setAtomicInteger(AtomicInteger b);

    public AtomicInteger getAtomicInteger();

    public void setAtomicLong(AtomicLong b);

    public AtomicLong getAtomicLong();

    public void start() throws Exception;

    public void stop() throws Exception;

}
