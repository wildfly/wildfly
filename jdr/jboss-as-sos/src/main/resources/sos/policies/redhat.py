## Implement policies required for the sos system support tool

## Copyright (C) Steve Conklin <sconklin@redhat.com>

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

# This enables the use of with syntax in python 2.5 (e.g. jython)
from __future__ import with_statement

import os
import sys
from tempfile import gettempdir
import random
import re
import platform
import time
from collections import deque

from sos import _sos as _
from sos.plugins import RedHatPlugin, IndependentPlugin
from sos.policies import Policy, PackageManager
from sos.utilities import shell_out

sys.path.insert(0, "/usr/share/rhn/")
try:
    from up2date_client import up2dateAuth
    from up2date_client import config
    from rhn import rpclib
except:
    # might fail if non-RHEL
    pass


class RHELPackageManager(PackageManager):

    def _get_rpm_list(self):
        pkg_list = shell_out(["rpm",
            "-qa",
            "--queryformat",
            "%{NAME}|%{VERSION}\\n"]).splitlines()
        self._rpms = {}
        for pkg in pkg_list:
            name, version = pkg.split("|")
            self._rpms[name] = {
                    'name': name,
                    'version': version
                    }

    def allPkgsByName(self, name):
        return fnmatch.filter(self.allPkgs().keys(), name)

    def allPkgsByNameRegex(self, regex_name, flags=None):
        reg = re.compile(regex_name, flags)
        return [pkg for pkg in self.allPkgs().keys() if reg.match(pkg)]

    def pkgByName(self, name):
        try:
            self.AllPkgsByName(name)[-1]
        except Exception:
            return None

    def allPkgs(self):
        if not self._rpms:
            self._rpms = self._get_rpm_list()
        return self._rpms

    def pkgNVRA(self, pkg):
        fields = pkg.split("-")
        version, release, arch = fields[-3:]
        name = "-".join(fields[:-3])
        return (name, version, release, arch)


class RHELPolicy(Policy):

    def __init__(self):
        super(RHELPolicy, self).__init__()
        self.reportName = ""
        self.ticketNumber = ""
        self.package_manager = RHELPackageManager()

    def validatePlugin(self, plugin_class):
        "Checks that the plugin will execute given the environment"
        return issubclass(plugin_class, RedHatPlugin) or issubclass(plugin_class, IndependentPlugin)

    @classmethod
    def check(self):
        "This method checks to see if we are running on RHEL. It returns True or False."
        return os.path.isfile('/etc/redhat-release') or os.path.isfile('/etc/fedora-release')

    def preferedArchive(self):
        from sos.utilities import TarFileArchive
        return TarFileArchive

    def getPreferredHashAlgorithm(self):
        checksum = "md5"
        try:
            fp = open("/proc/sys/crypto/fips_enabled", "r")
        except:
            return checksum

        fips_enabled = fp.read()
        if fips_enabled.find("1") >= 0:
            checksum = "sha256"
        fp.close()
        return checksum

    def pkgByName(self, name):
        return self.package_manager.pkgByName(name)

    def runlevelByService(self, name):
        from subprocess import Popen, PIPE
        ret = []
        p = Popen("LC_ALL=C /sbin/chkconfig --list %s" % name,
                  shell=True,
                  stdout=PIPE,
                  stderr=PIPE,
                  bufsize=-1)
        out, err = p.communicate()
        if err:
            return ret
        for tabs in out.split()[1:]:
            try:
                (runlevel, onoff) = tabs.split(":", 1)
            except:
                pass
            else:
                if onoff == "on":
                    ret.append(int(runlevel))
        return ret

    def runlevelDefault(self):
        try:
            with open("/etc/inittab") as fp:
                pattern = r"id:(\d{1}):initdefault:"
                text = fp.read()
                return int(re.findall(pattern, text)[0])
        except:
            return 3

    def kernelVersion(self):
        return self.release

    def hostName(self):
        return self.hostname

    def rhelVersion(self):
        try:
            pkg = self.pkgByName("redhat-release") or \
            self.allPkgsByNameRegex("redhat-release-.*")[-1]
            pkgname = pkg["version"]
            if pkgname[0] == "4":
                return 4
            elif pkgname in [ "5Server", "5Client" ]:
                return 5
            elif pkgname[0] == "6":
                return 6
        except:
            pass
        return False

    def rhnUsername(self):
        try:
            cfg = config.initUp2dateConfig()

            return rpclib.xmlrpclib.loads(up2dateAuth.getSystemId())[0][0]['username']
        except:
            # ignore any exception and return an empty username
            return ""

    def isKernelSMP(self):
        return self.smp

    def getArch(self):
        return self.machine

    def preWork(self):
        # this method will be called before the gathering begins

        localname = self.rhnUsername()
        if len(localname) == 0: localname = self.hostName()

        if not self.commons['cmdlineopts'].batch and not self.commons['cmdlineopts'].silent:
            try:
                self.reportName = raw_input(_("Please enter your first initial and last name [%s]: ") % localname)
                self.reportName = re.sub(r"[^a-zA-Z.0-9]", "", self.reportName)

                self.ticketNumber = raw_input(_("Please enter the case number that you are generating this report for: "))
                self.ticketNumber = re.sub(r"[^0-9]", "", self.ticketNumber)
                self._print()
            except:
                self._print()
                sys.exit(0)

        if len(self.reportName) == 0:
            self.reportName = localname

        if self.commons['cmdlineopts'].customerName:
            self.reportName = self.commons['cmdlineopts'].customerName
            self.reportName = re.sub(r"[^a-zA-Z.0-9]", "", self.reportName)

        if self.commons['cmdlineopts'].ticketNumber:
            self.ticketNumber = self.commons['cmdlineopts'].ticketNumber
            self.ticketNumber = re.sub(r"[^0-9]", "", self.ticketNumber)

        return

    def packageResults(self, archive_filename):
        self._print(_("Creating compressed archive..."))

    def get_msg(self):
        msg_dict = {"distro": "Red Hat Enterprise Linux"}
        if os.path.isfile('/etc/fedora-release'):
           msg_dict['distro'] = 'Fedora'
        return self.msg % msg_dict

# vim: ts=4 sw=4 et
