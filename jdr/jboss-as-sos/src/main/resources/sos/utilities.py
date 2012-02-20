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

# pylint: disable-msg = R0902
# pylint: disable-msg = R0904
# pylint: disable-msg = W0702
# pylint: disable-msg = W0703
# pylint: disable-msg = R0201
# pylint: disable-msg = W0611
# pylint: disable-msg = W0613

import os
import re
import sys
import string
import fnmatch
import inspect
from stat import *
from itertools import *
from subprocess import Popen, PIPE
import shlex
import logging
import zipfile
import tarfile
import hashlib
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO
import time

def checksum(filename, chunk_size=128):
    """Returns the checksum of the supplied filename. The file is read in
    chunk_size blocks"""
    name = get_hash_name()
    digest = hashlib.new(name)
    fd = open(filename, 'rb')
    data = fd.read(chunk_size)
    while data:
        digest.update(data)
        data = fd.read(chunk_size)
    return digest.hexdigest()

def get_hash_name():
    """Returns the algorithm used when computing a hash"""
    import sos.policies
    policy = sos.policies.load()
    try:
        name = policy.getPreferredHashAlgorithm()
        hashlib.new(name)
        return name
    except:
        return 'sha256'

class DirTree(object):
    """Builds an ascii representation of a directory structure"""

    def __init__(self, top_directory):
        self.directory_count = 0
        self.file_count = 0
        self.buffer = []
        self.top_directory = top_directory
        self._build_tree()

    def buf(self, s):
        self.buffer.append(s)

    def printtree(self):
        print self.as_string()

    def as_string(self):
        return "\n".join(self.buffer)

    def _build_tree(self):
        self.buf(os.path.abspath(self.top_directory))
        self.tree_i(self.top_directory, first=True)

    def _convert_bytes(self, n):
        K, M, G, T = 1 << 10, 1 << 20, 1 << 30, 1 << 40
        if n >= T:
            return '%.1fT' % (float(n) / T)
        elif n >= G:
            return '%.1fG' % (float(n) / G)
        elif n >= M:
            return '%.1fM' % (float(n) / M)
        elif n >= K:
            return '%.1fK' % (float(n) / K)
        else:
            return '%d' % n

    def _get_user(self, stats):
        try:
            import pwd
            return pwd.getpwuid(stats.st_uid)[0]
        except ImportError:
            return str(stats.st_uid)

    def _get_group(self, stats):
        try:
            import grp
            return grp.getgrgid(stats.st_gid)[0]
        except ImportError:
            return str(stats.st_uid)

    def _format(self, path):
        """Conditionally adds detail to paths"""
        stats = os.stat(path)
        details = {
                "filename": os.path.basename(path),
                "user": self._get_user(stats),
                "group": self._get_group(stats),
                "filesize": self._convert_bytes(stats.st_size),
                }
        return ("[%(user)s %(group)s %(filesize)s] " % details, "%(filename)s" % details)

    def tree_i(self, dir_, padding='', first=False, fmt="%-30s %s%s%s"):
        if not first:
            details, filename = self._format(os.path.abspath(dir_))
            line = fmt % (details, padding[:-1], "+-- ", filename)
            self.buf(line)
            padding += '   '

        count = 0
        files = os.listdir(dir_)
        files.sort(key=string.lower)
        for f in files:
            count += 1
            path = os.path.join(dir_, f)

            if f.startswith("."):
                pass
            elif os.path.isfile(path):
                self.file_count += 1
                details, filename = self._format(path)
                line = fmt % (details, padding, "+-- ", filename)
                self.buf(line)
            elif os.path.islink(path):
                self.buf(padding +
                         '+-- ' +
                         f +
                         ' -> ' + os.path.basename(os.path.realpath(path)))
                if os.path.isdir(path):
                    self.directory_count += 1
                else:
                    self.file_count += 1
            elif os.path.isdir(path):
                self.directory_count += 1
                if count == len(files):
                    self.tree_i(path, padding + ' ')
                else:
                    self.tree_i(path, padding + '|')


class ImporterHelper(object):
    """Provides a list of modules that can be imported in a package.
    Importable modules are located along the module __path__ list and modules
    are files that end in .py. This class will read from PKZip archives as well
    for listing out jar and egg contents."""

    def __init__(self, package):
        """package is a package module
        import my.package.module
        helper = ImporterHelper(my.package.module)"""
        self.package = package

    def _plugin_name(self, path):
        "Returns the plugin module name given the path"
        base = os.path.basename(path)
        name, ext = os.path.splitext(base)
        return name

    def _get_plugins_from_list(self, list_):
        plugins = [self._plugin_name(plugin)
                for plugin in list_
                if "__init__" not in plugin
                and plugin.endswith(".py")]
        plugins.sort()
        return plugins

    def _find_plugins_in_dir(self, path):
        if os.path.exists(path):
            py_files = list(find("*.py", path))
            pnames = self._get_plugins_from_list(py_files)
            if pnames:
                return pnames
            else:
                return []

    def _get_path_to_zip(self, path, tail_list=None):
        if not tail_list:
            tail_list = ['']

        if path.endswith(('.jar', '.zip', '.egg')):
            return path, os.path.join(*tail_list)

        head, tail = os.path.split(path)
        tail_list.insert(0, tail)

        if head == path:
            raise Exception("not a zip file")
        else:
            return self._get_path_to_zip(head, tail_list)


    def _find_plugins_in_zipfile(self, path):
        try:
            path_to_zip, tail = self._get_path_to_zip(path)
            zf = zipfile.ZipFile(path_to_zip)
            root_names = [name for name in zf.namelist() if tail in name]
            candidates = self._get_plugins_from_list(root_names)
            zf.close()
            if candidates:
                return candidates
            else:
                return []
        except (IOError, Exception):
            return []

    def get_modules(self):
        "Returns the list of importable modules in the configured python package."
        plugins = []
        for path in self.package.__path__:
            if os.path.isdir(path) or path == '':
                plugins.extend(self._find_plugins_in_dir(path))
            else:
                plugins.extend(self._find_plugins_in_zipfile(path))

        return plugins


def find(file_pattern, top_dir, max_depth=None, path_pattern=None):
    """generator function to find files recursively. Usage:

    for filename in find("*.properties", "/var/log/foobar"):
        print filename
    """
    if max_depth:
        base_depth = os.path.dirname(top_dir).count(os.path.sep)
        max_depth += base_depth

    for path, dirlist, filelist in os.walk(top_dir):
        if max_depth and path.count(os.path.sep) >= max_depth:
            del dirlist[:]

        if path_pattern and not fnmatch.fnmatch(path, path_pattern):
            continue

        for name in fnmatch.filter(filelist, file_pattern):
            yield os.path.join(path, name)


def sosGetCommandOutput(command, timeout=300):
    """Execute a command through the system shell. First checks to see if the
    requested command is executable. Returns (returncode, stdout, 0)"""
    # XXX: what is this doing this for?
    cmdfile = command.strip("(").split()[0]

    possibles = [cmdfile] + [os.path.join(path, cmdfile) for path in os.environ.get("PATH", "").split(":")]

    if any(os.access(path, os.X_OK) for path in possibles):
        p = Popen(command, shell=True, stdout=PIPE, stderr=PIPE, bufsize=-1)
        stdout, stderr = p.communicate()
        return (p.returncode, stdout.strip(), 0)
    else:
        return (127, "", 0)


def import_module(module_fqname, superclass=None):
    module_name = module_fqname.rpartition(".")[-1]
    module = __import__(module_fqname, globals(), locals(), [module_name])
    modules = [class_ for cname, class_ in
               inspect.getmembers(module, inspect.isclass)
               if class_.__module__ == module_fqname]
    if superclass:
        modules = [m for m in modules if issubclass(m, superclass)]

    return modules

class Archive(object):

    _name = "unset"

    def prepend(self, src):
        if src:
            name = os.path.split(self._name)[-1]
            return os.path.join(name, src.lstrip(os.sep))

    def add_link(self, dest, link_name):
        pass


class TarFileArchive(Archive):

    def __init__(self, name):
        self._name = name
        self.tarfile = tarfile.open(self.name(), mode="w")

    def name(self):
        return "%s.tar" % self._name

    def add_file(self, src, dest=None):
        if dest:
            dest = self.prepend(dest)
        else:
            dest = self.prepend(src)

        fp = open(src, 'rb')
        content = fp.read()
        fp.close()

        tar_info = tarfile.TarInfo(name=dest)
        tar_info.size = len(content)
        tar_info.mtime = os.stat(src).st_mtime

        self.tarfile.addfile(tar_info, StringIO(content))

    def add_string(self, content, dest):
        dest = self.prepend(dest)
        tar_info = tarfile.TarInfo(name=dest)
        tar_info.size = len(content)
        tar_info.mtime = time.time()
        self.tarfile.addfile(tar_info, StringIO(content))

    def add_link(self, dest, link_name):
        tar_info = tarfile.TarInfo(name=self.prepend(link_name))
        tar_info.type = tarfile.SYMTYPE
        tar_info.linkname = dest
        tar_info.mtime = time.time()
        self.tarfile.addfile(tar_info, None)

    def open_file(self, name):
        try:
            self.tarfile.close()
            self.tarfile = tarfile.open(self.name(), mode="r")
            name = self.prepend(name)
            file_obj = self.tarfile.extractfile(name)
            file_obj = StringIO(file_obj.read())
            return file_obj
        finally:
            self.tarfile.close()
            self.tarfile = tarfile.open(self.name(), mode="a")

    def close(self):
        self.tarfile.close()


class ZipFileArchive(Archive):

    def __init__(self, name):
        self._name = name
        try:
            import zlib
            self.compression = zipfile.ZIP_DEFLATED
        except:
            self.compression = zipfile.ZIP_STORED

        self.zipfile = zipfile.ZipFile(self.name(), mode="w", compression=self.compression)

    def name(self):
        return "%s.zip" % self._name

    def add_file(self, src, dest=None):
        if os.path.isdir(src):
            # We may not need, this, but if we do I only want to do it
            # one time
            regex = re.compile(r"^" + src)
            for path, dirnames, filenames in os.walk(src):
                for filename in filenames:
                    filename = path + filename
                    if dest:
                        self.zipfile.write(filename,
                                self.prepend(re.sub(regex, dest, filename)))
                    else:
                        self.zipfile.write(filename, self.prepend(filename))
        else:
            if dest:
                self.zipfile.write(src, self.prepend(dest))
            else:
                self.zipfile.write(src, self.prepend(src))

    def add_string(self, content, dest):
        info = zipfile.ZipInfo(self.prepend(dest),
                date_time=time.localtime(time.time()))
        info.compress_type = self.compression
        info.external_attr = 0400 << 16L
        self.zipfile.writestr(info, content)

    def open_file(self, name):
        try:
            self.zipfile.close()
            self.zipfile = zipfile.ZipFile(self.name(), mode="r")
            name = self.prepend(name)
            file_obj = self.zipfile.open(name)
            return file_obj
        finally:
            self.zipfile.close()
            self.zipfile = zipfile.ZipFile(self.name(), mode="a")

    def close(self):
        self.zipfile.close()


def compress(archive, method):
    """Compress an archive object via method. ZIP archives are ignored. If
    method is automatic then the following technologies are tried in order: xz,
    bz2 and gzip"""

    if method == "zip":
        return archive.name()

    methods = ['xz', 'bzip2', 'gzip']

    if method in ('xz', 'bzip2', 'gzip'):
        methods = [method]

    compressed = False
    last_error = None
    for cmd in ('xz', 'bzip2', 'gzip'):
        if compressed:
            break
        try:
            command = shlex.split("%s %s" % (cmd,archive.name()))
            p = Popen(command, stdout=PIPE, stderr=PIPE, bufsize=-1)
            stdout, stderr = p.communicate()
            log = logging.getLogger('sos')
            if stdout:
                log.info(stdout)
            if stderr:
                log.error(stderr)
            compressed = True
            return archive.name() + "." + cmd.replace('ip','')
        except Exception, e:
            last_error = e

    if not compressed:
        raise last_error


def shell_out(cmd):
    """Uses subprocess.Popen to make a system call and returns stdout.
    Does not handle exceptions."""
    p = Popen(cmd, shell=True, stdout=PIPE, stderr=PIPE)
    return p.communicate()[0]
# vim:ts=4 sw=4 et
