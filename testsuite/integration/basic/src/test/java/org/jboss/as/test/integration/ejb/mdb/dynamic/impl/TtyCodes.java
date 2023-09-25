/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.impl;

/**
 * @version $Revision$ $Date$
 */
public interface TtyCodes {

    char ESC = (char) 27;

    String TTY_Reset = ESC + "[0m";

    String TTY_Bright = ESC + "[1m";

    String TTY_Dim = ESC + "[2m";

    String TTY_Underscore = ESC + "[4m";

    String TTY_Blink = ESC + "[5m";

    String TTY_Reverse = ESC + "[7m";

    String TTY_Hidden = ESC + "[8m";

    /* Foreground Colors */

    String TTY_FG_Black = ESC + "[30m";

    String TTY_FG_Red = ESC + "[31m";

    String TTY_FG_Green = ESC + "[32m";

    String TTY_FG_Yellow = ESC + "[33m";

    String TTY_FG_Blue = ESC + "[34m";

    String TTY_FG_Magenta = ESC + "[35m";

    String TTY_FG_Cyan = ESC + "[36m";

    String TTY_FG_White = ESC + "[37m";

    /* Background Colors */

    String TTY_BG_Black = ESC + "[40m";

    String TTY_BG_Red = ESC + "[41m";

    String TTY_BG_Green = ESC + "[42m";

    String TTY_BG_Yellow = ESC + "[43m";

    String TTY_BG_Blue = ESC + "[44m";

    String TTY_BG_Magenta = ESC + "[45m";

    String TTY_BG_Cyan = ESC + "[46m";

    String TTY_BG_White = ESC + "[47m";

}
