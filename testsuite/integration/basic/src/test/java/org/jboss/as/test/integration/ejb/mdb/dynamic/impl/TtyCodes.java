/*
 * Copyright 2012 David Blevins
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.jboss.as.test.integration.ejb.mdb.dynamic.impl;

/**
 * @version $Revision$ $Date$
 */
public interface TtyCodes {

    public static final char ESC = (char) 27;

    public static final String TTY_Reset = ESC + "[0m";

    public static final String TTY_Bright = ESC + "[1m";

    public static final String TTY_Dim = ESC + "[2m";

    public static final String TTY_Underscore = ESC + "[4m";

    public static final String TTY_Blink = ESC + "[5m";

    public static final String TTY_Reverse = ESC + "[7m";

    public static final String TTY_Hidden = ESC + "[8m";

    /* Foreground Colors */

    public static final String TTY_FG_Black = ESC + "[30m";

    public static final String TTY_FG_Red = ESC + "[31m";

    public static final String TTY_FG_Green = ESC + "[32m";

    public static final String TTY_FG_Yellow = ESC + "[33m";

    public static final String TTY_FG_Blue = ESC + "[34m";

    public static final String TTY_FG_Magenta = ESC + "[35m";

    public static final String TTY_FG_Cyan = ESC + "[36m";

    public static final String TTY_FG_White = ESC + "[37m";

    /* Background Colors */

    public static final String TTY_BG_Black = ESC + "[40m";

    public static final String TTY_BG_Red = ESC + "[41m";

    public static final String TTY_BG_Green = ESC + "[42m";

    public static final String TTY_BG_Yellow = ESC + "[43m";

    public static final String TTY_BG_Blue = ESC + "[44m";

    public static final String TTY_BG_Magenta = ESC + "[45m";

    public static final String TTY_BG_Cyan = ESC + "[46m";

    public static final String TTY_BG_White = ESC + "[47m";

}
