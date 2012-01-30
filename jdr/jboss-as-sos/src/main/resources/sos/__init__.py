## Copyright 2010 Red Hat, Inc.
## Author: Adam Stokes <astokes@fedoraproject.org>

## This program is free software; you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation; either version 2 of the License, or
## (at your option) any later version.

## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.

## You should have received a copy of the GNU General Public License
## along with this program; if not, write to the Free Software
## Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

__version__ = "@SOSVERSION@"

import gettext
gettext_dir = "/usr/share/locale"
gettext_app = "sos"

gettext.bindtextdomain(gettext_app, gettext_dir)

def _default(msg):
    return gettext.dgettext(gettext_app, msg)

_sos = _default

def _get_classloader(jarfile):
    """Makes a new classloader loaded with the jarfile"""
    from java.net import URLClassLoader, URL
    from java.io import File
    import jarray

    file_ = File(jarfile)
    ary = jarray.array([file_.toURL()], URL)
    classloader = URLClassLoader.newInstance(ary)
    return classloader

def set_i18n(path=None):
    try:
        from java.util import ResourceBundle, Locale

        rb = ResourceBundle.getBundle("sos.po.sos",
                Locale.getDefault(), _get_classloader(path))

        def _java(msg):
            try:
                return rb.getString(msg).encode('utf-8')
            except:
                return msg
        _sos = _java
    except:
        pass
