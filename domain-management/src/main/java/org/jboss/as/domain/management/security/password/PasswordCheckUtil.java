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

package org.jboss.as.domain.management.security.password;

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.jboss.as.domain.management.security.password.PasswordCheckResult.Result;
import org.jboss.as.domain.management.security.password.simple.SimplePasswordStrengthChecker;

/**
 * Simple util which narrows down password checks so there is no hassle in performing those checks in CLI.
 *
 * @author baranowb
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PasswordCheckUtil {

    public static final String _PROPERTY_CHECKER = "password.restriction.checker";
    public static final String _PROPERTY_STRENGTH = "password.restriction.strength";
    public static final String _PROPERTY_FORBIDDEN = "password.restriction.forbiddenValue";

    public static final String _PROPERTY_RESTRICTION = "password.restriction";
    public static final String _PROPERTY_MIN_LENGTH = "password.restriction.minLength";
    public static final String _PROPERTY_MIN_ALPHA = "password.restriction.minAlpha";
    public static final String _PROPERTY_MIN_DIGIT = "password.restriction.minDigit";
    public static final String _PROPERTY_MIN_SYMBOL = "password.restriction.minSymbol";
    public static final String _PROPERTY_MATCH_USERNAME = "password.restriction.mustNotMatchUsername";

    private PasswordStrengthChecker passwordStrengthChecker;
    private PasswordStrength acceptable = PasswordStrength.MODERATE;
    private RestrictionLevel level = RestrictionLevel.WARN;
    // Something ordered is good so the ordering of messages and validation is consistent across invocations.
    public List<PasswordRestriction> passwordValuesRestrictions = new ArrayList<PasswordRestriction>();
    private CompoundRestriction compountRestriction = null;

    private PasswordCheckUtil(final File configFile) {
        if (configFile != null && configFile.exists()) {
            try {
                Properties configProperties = new Properties();
                configProperties.load(new FileInputStream(configFile));
                // strength
                initDefaultStrength(configProperties);
                // checker
                initStrengthChecker(configProperties);
                // name restrictions
                initPasswordRestrictions(configProperties);
                // length
                initMinLength(configProperties);
                // alpha
                initMinAlpha(configProperties);
                // digit
                initMinDigit(configProperties);
                // symbol
                initMinSymbol(configProperties);
                // match username
                initMustNotMatchUsername(configProperties);
                // level
                initRestrictionLevel(configProperties);
            } catch (IOException e) {
                simple();
            }
        } else {
            simple();
        }
    }

    private void simple() {
        // revert to simple
        this.passwordStrengthChecker = new SimplePasswordStrengthChecker();
    }

    public static PasswordCheckUtil create(final File configFile) {
        return new PasswordCheckUtil(configFile);
    }

    /**
     * @param props
     */
    private void initPasswordRestrictions(Properties props) {
        try {
            String forbiddens = props.getProperty(_PROPERTY_FORBIDDEN);
            if (forbiddens == null) {
                return;
            }

            String[] values = forbiddens.split(",");
            this.passwordValuesRestrictions.add(new ValueRestriction(values));
        } catch (Exception e) {
            // log?
        }
    }

    /**
     * @param props
     */
    private void initStrengthChecker(Properties props) {
        try {
            String stringClassName = props.getProperty(_PROPERTY_CHECKER);
            if (stringClassName == null) {
                this.simple();
                return;
            }

            Class<PasswordStrengthChecker> clazz = (Class<PasswordStrengthChecker>) PasswordCheckUtil.class
                    .forName(stringClassName);
            this.passwordStrengthChecker = clazz.newInstance();
        } catch (Exception e) {
            this.simple();
        }
    }

    /**
     * @param props
     */
    private void initDefaultStrength(Properties props) {
        try {
            this.acceptable = PasswordStrength.valueOf(props.getProperty(_PROPERTY_STRENGTH).toUpperCase());
        } catch (Exception e) {
            // log
        }
    }

    /**
     * @param props
     */
    private void initMinAlpha(Properties props) {
        try {
            int minAlpha = Integer.parseInt(props.getProperty(_PROPERTY_MIN_ALPHA));
            createAlphaRestriction(minAlpha);
        } catch (Exception e) {
            // log
        }
    }

    /**
     * @param props
     */
    private void initMinSymbol(Properties props) {
        try {
            int minAlpha = Integer.parseInt(props.getProperty(_PROPERTY_MIN_SYMBOL));
            createSymbolRestriction(minAlpha);
        } catch (Exception e) {
            // log
        }
    }

    /**
     * @param props
     */
    private void initMinDigit(Properties props) {
        try {
            int minDigit = Integer.parseInt(props.getProperty(_PROPERTY_MIN_DIGIT));
            createDigitRestriction(minDigit);
        } catch (Exception e) {
            // log
        }
    }

    /**
     * @param props
     */
    private void initMinLength(Properties props) {
        try {
            int minLength = Integer.parseInt(props.getProperty(_PROPERTY_MIN_LENGTH));
            createLengthRestriction(minLength);
        } catch (Exception e) {
            // log
        }
    }

    /**
     * @param props
     */
    private void initMustNotMatchUsername(Properties props) {
        try {
            if (Boolean.parseBoolean(props.getProperty(_PROPERTY_MATCH_USERNAME))) {
                passwordValuesRestrictions.add(new UsernamePasswordMatch());
            }
        } catch (Exception e) {
            // log
        }
    }

    /**
     * @param props
     */
    private void initRestrictionLevel(Properties props) {
        try {
            level = RestrictionLevel.valueOf(props.getProperty(_PROPERTY_RESTRICTION));
        } catch (Exception e) {
            // log
        }
    }

    private boolean assertStrength(PasswordStrength result) {
        return result.getStrength() >= this.acceptable.getStrength();
    }

    /**
     * Method which performs strength checks on password. It returns outcome which can be used by CLI.
     *
     * @param isAdminitrative - administrative checks are less restrictive. This means that weak password or one which violates restrictions is not indicated as failure.
     * Administrative checks are usually performed by admin changing/setting default password for user.
     * @param userName - the name of user for which password is set.
     * @param password - password.
     * @return
     */
    public PasswordCheckResult check(boolean isAdminitrative, String userName, String password) {
        // TODO: allow custom restrictions?
        List<PasswordRestriction> passwordValuesRestrictions = getPasswordRestrictions();
        final PasswordStrengthCheckResult strengthResult = this.passwordStrengthChecker.check(userName, password, passwordValuesRestrictions);

        final int failedRestrictions = strengthResult.getRestrictionFailures().size();
        final PasswordStrength strength = strengthResult.getStrength();
        final boolean strongEnough = assertStrength(strength);

        PasswordCheckResult.Result resultAction;
        String resultMessage = null;
        if (isAdminitrative) {
            if (strongEnough) {
                if (failedRestrictions > 0) {
                    resultAction = Result.WARN;
                    resultMessage = strengthResult.getRestrictionFailures().get(0).getMessage();
                } else {
                    resultAction = Result.ACCEPT;
                }
            } else {
                resultAction = Result.WARN;
                resultMessage = MESSAGES.passwordNotStrongEnough(strength.toString(), this.acceptable.toString());
            }
        } else {
            if (strongEnough) {
                if (failedRestrictions > 0) {
                    resultAction = Result.REJECT;
                    resultMessage = strengthResult.getRestrictionFailures().get(0).getMessage();
                } else {
                    resultAction = Result.ACCEPT;
                }
            } else {
                if (failedRestrictions > 0) {
                    resultAction = Result.REJECT;
                    resultMessage = strengthResult.getRestrictionFailures().get(0).getMessage();
                } else {
                    resultAction = Result.REJECT;
                    resultMessage = MESSAGES.passwordNotStrongEnough(strength.toString(), this.acceptable.toString());
                }
            }
        }

        return new PasswordCheckResult(resultAction, resultMessage);

    }

    public RestrictionLevel getRestrictionLevel() {
        return level;
    }

    public List<PasswordRestriction> getPasswordRestrictions() {
        return Collections.unmodifiableList(passwordValuesRestrictions);
    }

    private void addToCompointRestriction(final PasswordRestriction toWrap) {
        if (compountRestriction == null) {
            compountRestriction = new CompoundRestriction();
            passwordValuesRestrictions.add(compountRestriction);
        }
        compountRestriction.add(toWrap);
    }

    public void createLengthRestriction(int minLength) {
        if (minLength > 0) {
            addToCompointRestriction(new LengthRestriction(minLength));
        }
    }

    public PasswordRestriction createAlphaRestriction(int minAlpha) {
        return createRegExRestriction(minAlpha, SimplePasswordStrengthChecker.REGEX_ALPHA,
                MESSAGES.passwordMustHaveAlphaInfo(minAlpha), MESSAGES.passwordMustHaveAlpha(minAlpha));
    }

    public PasswordRestriction createDigitRestriction(int minDigit) {
        return createRegExRestriction(minDigit, SimplePasswordStrengthChecker.REGEX_DIGITS,
                MESSAGES.passwordMustHaveDigitInfo(minDigit), MESSAGES.passwordMustHaveDigit(minDigit));
    }

    public PasswordRestriction createSymbolRestriction(int minSymbol) {
        return createRegExRestriction(minSymbol, SimplePasswordStrengthChecker.REGEX_SYMBOLS,
                MESSAGES.passwordMustHaveSymbolInfo(minSymbol), MESSAGES.passwordMustHaveSymbol(minSymbol));
    }

    private PasswordRestriction createRegExRestriction(int minChar, String regex, String requirementsMessage,
            String failureMessage) {
        if (minChar > 0) {
            PasswordRestriction pr = new RegexRestriction(String.format("(.*%s.*){%d}", regex, minChar), requirementsMessage,
                    failureMessage);
            addToCompointRestriction(pr);
            return pr;
        }
        return null;
    }
}
