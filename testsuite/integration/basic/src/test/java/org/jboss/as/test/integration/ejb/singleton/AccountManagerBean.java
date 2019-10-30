/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.singleton;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Singleton;

/**
 * AccountManagerBean
 *
 * @author Jaikiran Pai
 */
@Singleton
@LocalBean
@Remote(AccountManager.class)
public class AccountManagerBean implements AccountManager {
    /**
     * Inject the no-interface view of the Calculator
     */
    @EJB
    private Calculator simpleCalculator;

    private int balance;

    /**
     * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#credit(int)
     */
    public void credit(int amount) {
        this.balance = this.simpleCalculator.add(this.balance, amount);

    }

    /**
     * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#debit(int)
     */
    public void debit(int amount) {
        this.balance = this.simpleCalculator.subtract(this.balance, amount);
    }

    public int balance() {
        return this.balance;
    }

    public void throwException() {
        throw new IllegalArgumentException();
    }

}
