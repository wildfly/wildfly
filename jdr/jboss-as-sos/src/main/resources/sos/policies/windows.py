### This program is free software; you can redistribute it and/or modify
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
import os
import time
import platform

from sos.policies import PackageManager, Policy
from sos.utilities import shell_out

class WindowsPolicy(Policy):

    distro = "Microsoft Windows"

    @classmethod
    def check(class_):
        is_windows = False
        try:
            from java.lang import System
            is_windows = "win" in System.getProperty('os.name').lower()
        except:
            is_windows = "win" in platform.system().lower()
        return is_windows

    def is_root(self):
        if "S-1-16-12288" in shell_out("whoami /groups"):
            return True
        else:
            admins = shell_out("net localgroup administrators")
            username = shell_out("echo %USERNAME%")
            return username.strip() in admins

    def preferedArchive(self):
        from sos.utilities import ZipFileArchive
        return ZipFileArchive
