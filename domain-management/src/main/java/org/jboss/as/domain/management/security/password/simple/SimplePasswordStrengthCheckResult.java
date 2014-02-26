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
import org.jboss.as.domain.management.security.password.PasswordValidationException;

/**
 *
 * @author baranowb
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SimplePasswordStrengthCheckResult implements PasswordStrengthCheckResult {

    private PasswordStrength strength;
    private List<PasswordValidationException> restrictionFailures = new ArrayList<PasswordValidationException>();
    private List<PasswordRestriction> passedRestrictions = new ArrayList<PasswordRestriction>();
    private int positive = 0;
    private int negative = 0;

    /**
     * @return the strength
     */
    public PasswordStrength getStrength() {
        return strength;
    }

    public List<PasswordValidationException> getRestrictionFailures() {
        return restrictionFailures;
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
    void addRestrictionFailure(PasswordValidationException pve) {
        restrictionFailures.add(pve);
    }

    private static final float BOUNDARY_EXCEPTIONAL = 0.03f;
    private static final float BOUNDARY_VERY_STRONG = 0.1f;
    private static final float BOUNDARY_STRONG = 0.15f;
    private static final float BOUNDARY_MEDIUM = 0.5f;
    private static final float BOUNDARY_MODERATE = 0.7f;
    private static final float BOUNDARY_WEAK = 0.9f;

    void calculateStrength() {
        float f = (float) negative / positive;

        if (f <= BOUNDARY_EXCEPTIONAL) {
            this.strength = PasswordStrength.EXCEPTIONAL;
        } else if (f <= BOUNDARY_VERY_STRONG) {
            this.strength = PasswordStrength.VERY_STRONG;
        } else if (f <= BOUNDARY_STRONG) {
            this.strength = PasswordStrength.STRONG;
        } else if (f <= BOUNDARY_MEDIUM) {
            this.strength = PasswordStrength.MEDIUM;
        } else if (f <= BOUNDARY_MODERATE) {
            this.strength = PasswordStrength.MODERATE;
        } else if (f <= BOUNDARY_WEAK) {
            this.strength = PasswordStrength.WEAK;
        } else {
            this.strength = PasswordStrength.VERY_WEAK;
        }
    }

}
