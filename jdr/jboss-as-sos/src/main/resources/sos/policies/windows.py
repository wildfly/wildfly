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

from sos.policies import PackageManager, Policy
from sos.plugins import IndependentPlugin
import subprocess

try:
    from hashlib import md5
except ImportError:
    from md5 import md5

class WindowsPolicy(Policy):

    def __init__(self):
        self._parse_uname()
        self.ticketNumber = None
        self.reportName = self.hostname
        self.package_manager = PackageManager()

    def setCommons(self, commons):
        self.commons = commons

    def validatePlugin(self, plugin_class):
        return issubclass(plugin_class, IndependentPlugin)

    @classmethod
    def check(class_):
        try:
            p = subprocess.Popen("ver", shell=True, stdout=subprocess.PIPE)
            ver_string = p.communicate()[0]
            return "Windows" in ver_string
        except Exception, e:
            return False

    def is_root(self):
        p = subprocess.Popen("whoami /groups",
                shell=True, stdout=subprocess.PIPE)
        stdout = p.communicate()[0]
        if "S-1-16-12288" in stdout:
            return True
        else:
            cmd = 'net localgroup administrators | find "%USERNAME"'
            print cmd
            return subprocess.call(cmd, shell=True) == 0

    def preferedArchive(self):
        from sos.utilities import ZipFileArchive
        return ZipFileArchive

    def pkgByName(self, name):
        return None

    def preWork(self):
        pass

    def packageResults(self, archive_filename):
        self.report_file = archive_filename

    def getArchiveName(self):
        if self.ticketNumber:
            self.reportName += "." + self.ticketNumber
        return "sosreport-%s-%s" % (self.reportName, time.strftime("%Y%m%d%H%M%S"))

    def displayResults(self, final_filename=None):

        if not final_filename:
            return False

        fp = open(final_filename, "r")
        md5sum = md5(fp.read()).hexdigest()
        fp.close()

        fp = open(final_filename + ".md5", "w")
        fp.write(md5sum + "\n")
        fp.close()

    def uploadResults(self, final_filename=None):
        pass
