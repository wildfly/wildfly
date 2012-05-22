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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jboss.as.domain.management.security.password.PasswordCheckResult.Result;
import org.jboss.as.domain.management.security.password.simple.SimplePasswordStrengthChecker;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
/**
 * Simple util which narrows down password checks so there is no hassle in performing those checks in CLI.
 * @author baranowb
 *
 */
public class PasswordCheckUtil {

    public static final String _PROPERTY_CHECKER = "checker";
    public static final String _PROPERTY_STRENGTH = "strength";
    public static final String _PROPERTY_FORBIDDEN = "forbidden";

    public static final PasswordCheckUtil INSTANCE = new PasswordCheckUtil();

    private PasswordStrengthChecker passwordStrengthChecker;
    private PasswordStrength acceptable = PasswordStrength.MODERATE;
    private List<PasswordRestriction> passwordValuesRestrictions = new ArrayList<PasswordRestriction>();

    private PasswordCheckUtil() {
        InputStream is = Keyboard.class.getResourceAsStream("utility.properties");
        if (is != null) {
            this.init(is);
        } else {
            this.simple();
        }
    }

    private void simple() {
        // revert to simple
        this.passwordStrengthChecker = new SimplePasswordStrengthChecker();
    }

    private void init(InputStream is) {
        try {
            Properties props = new Properties();
            props.load(is);
            // strength
            this.initDefaultStrength(props);
            // checker
            this.initStrengthChecker(props);
            // name restrictions
            this.initPasswordRestrictions(props);
        } catch (Exception e) {
            // print?
            this.simple();
        }
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
            for (String v : values) {
                if (v != null && v.length() > 0) {
                    this.passwordValuesRestrictions.add(new ValueRestriction(v));
                }
            }
        } catch (Exception e) {
            // log?
        }
    }

    /**
     * @param props
     */
    private void initStrengthChecker(Properties props) {
        try {
            String stringClassName = (String) props.get(_PROPERTY_CHECKER);
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

    private void initDefaultStrength(Properties props) {
        try {
            this.acceptable = PasswordStrength.valueOf(props.getProperty(_PROPERTY_STRENGTH).toUpperCase());
        } catch (Exception e) {
            // log
        }
    }

    private boolean assertStrength(PasswordStrength result) {
        return result.getStrength() >= this.acceptable.getStrength();
    }

    /**
     * Method which performs strength checks on password. It returns outcome which can be used by CLI.
     * @param isAdminitrative - administrative checks are less restrictive. This means that weak password or one which violates restrictions is not indicated as failure.
     * Administrative checks are usually performed by admin changing/setting default password for user.
     * @param userName - the name of user for which password is set.
     * @param password - password.
     * @return
     */
    public PasswordCheckResult check(boolean isAdminitrative, String userName, String password) {
        // TODO: allow custom restrictions?

        List<PasswordRestriction> passwordValuesRestrictions = new ArrayList<PasswordRestriction>(this.passwordValuesRestrictions);
        passwordValuesRestrictions.add(new ValueRestriction(userName));
        final PasswordStrengthCheckResult strengthResult = this.passwordStrengthChecker.check(password, passwordValuesRestrictions);

        final int failedRestrictions = strengthResult.getFailedRestrictions().size();
        final PasswordStrength strength = strengthResult.getStrength();
        final boolean strongEnough = assertStrength(strength);

        PasswordCheckResult.Result resultAction = null;
        String resultMessage = null;
        if (isAdminitrative) {
            if (strongEnough) {
                if (failedRestrictions > 0) {
                    resultAction = Result.WARN;
                    resultMessage = strengthResult.getFailedRestrictions().get(0).getMessage();
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
                    resultMessage = strengthResult.getFailedRestrictions().get(0).getMessage();
                } else {
                    resultAction = Result.ACCEPT;
                }
            } else {
                if (failedRestrictions > 0) {
                    resultAction = Result.REJECT;
                    resultMessage = strengthResult.getFailedRestrictions().get(0).getMessage();
                } else {
                    resultAction = Result.REJECT;
                    resultMessage = MESSAGES.passwordNotStrongEnough(strength.toString(), this.acceptable.toString());
                }
            }
        }

        return new PasswordCheckResult(resultAction, resultMessage);

    }
}
