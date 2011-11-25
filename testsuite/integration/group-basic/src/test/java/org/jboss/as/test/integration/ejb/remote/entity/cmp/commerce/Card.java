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
package org.jboss.as.test.integration.ejb.remote.entity.cmp.commerce;


public class Card implements java.io.Serializable {
    public static final int VISA = 0;
    public static final int AMERICAN_EXPRESS = 1;
    public static final int MASTER_CARD = 2;
    public static final int DISCOVER = 3;

    private int type;
    private FormalName cardHolder;
    private String cardNumber;
    private int billingZip;

    public Card() {
    }

    public FormalName getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(FormalName name) {
        this.cardHolder = name;
    }

    public int getBillingZip() {
        return billingZip;
    }

    public void setBillingZip(int zip) {
        this.billingZip = zip;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String num) {
        this.cardNumber = num;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        if (type != VISA &&
                type != AMERICAN_EXPRESS &&
                type != MASTER_CARD &&
                type != DISCOVER) {
            throw new IllegalArgumentException("Unknown card type: " + type);
        }
        this.type = type;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Card) {
            Card c = (Card) obj;
            return
                    equal(c.cardNumber, cardNumber) &&
                            equal(c.cardHolder, cardHolder) &&
                            c.type == type &&
                            c.billingZip == billingZip;
        }
        return false;
    }

    private boolean equal(Object a, Object b) {
        return (a == null && b == null) || (a != null && a.equals(b));
    }

    public String toString() {
        return cardNumber;
    }
}
