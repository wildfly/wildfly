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

package org.jboss.as.domain.management.security.password.simple;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.security.password.PasswordStrengthCheckResult;
import org.jboss.as.domain.management.security.password.PasswordRestriction;
import org.jboss.as.domain.management.security.password.PasswordStrength;

/**
 * @author baranowb
 *
 */
public class SimplePasswordStrengthCheckResult implements PasswordStrengthCheckResult {

    private PasswordStrength strength;
    private List<PasswordRestriction> failedRestrictions = new ArrayList<PasswordRestriction>();
    private List<PasswordRestriction> passedRestrictions = new ArrayList<PasswordRestriction>();
    private int positive = 0;
    private int negative = 0;

    /**
     * @return the strength
     */
    public PasswordStrength getStrength() {
        return strength;
    }

    /**
     * @return the failedRestrictions
     */
    public List<PasswordRestriction> getFailedRestrictions() {
        return failedRestrictions;
    }

    /**
     * @return the passedRestrictions
     */
    public List<PasswordRestriction> getPassedRestrictions() {
        return passedRestrictions;
    }

    /**
     * @param pr
     */
    void addPassedRestriction(PasswordRestriction pr) {
        this.passedRestrictions.add(pr);
    }

    /**
     * @param i
     */
    public void negative(int i) {
        this.negative += i;
    }

    /**
     * @param i
     */
    void positive(int i) {
        this.positive += i;
    }

    /**
     * @param pr
     */
    void addFailedRestriction(PasswordRestriction pr) {
        this.failedRestrictions.add(pr);
    }

    private static final float BOUNDRY_EXCEPTIONAL = 0.03f;
    private static final float BOUNDRY_VERY_STRONG = 0.1f;
    private static final float BOUNDRY_STRONG = 0.15f;
    private static final float BOUNDRY_MEDIUM = 0.5f;
    private static final float BOUNDRY_MODERATE = 0.7f;
    private static final float BOUNDRY_WEAK = 0.9f;

    void calculateStrength() {
        float f = (float) negative / positive;

        if (f <= BOUNDRY_EXCEPTIONAL) {
            this.strength = PasswordStrength.EXCEPTIONAL;
        } else if (f <= BOUNDRY_VERY_STRONG) {
            this.strength = PasswordStrength.VERY_STRONG;
        } else if (f <= BOUNDRY_STRONG) {
            this.strength = PasswordStrength.STRONG;
        } else if (f <= BOUNDRY_MEDIUM) {
            this.strength = PasswordStrength.MEDIUM;
        } else if (f <= BOUNDRY_MODERATE) {
            this.strength = PasswordStrength.MODERATE;
        } else if (f <= BOUNDRY_WEAK) {
            this.strength = PasswordStrength.WEAK;
        } else {
            this.strength = PasswordStrength.VERY_WEAK;
        }
    }

}
