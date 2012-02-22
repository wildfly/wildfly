## This exports methods available for use by plugins for sos

## Copyright (C) 2006 Steve Conklin <sconklin@redhat.com>

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

from sos.utilities import sosGetCommandOutput, import_module
from sos import _sos as _
import inspect
import os
import sys
import string
import glob
import re
import traceback
import shutil
from stat import *
from time import time
from itertools import *
from collections import deque
import logging
import urllib2

try:
    import json
except ImportError:
    import simplejson as json

def commonPrefix(l1, l2, common = None):
    """
    Returns a tuple like the following:
        ([common, elements, from l1, and l2], [[tails, from, l1], [tails, from, l2]])

    >>> commonPrefix(['usr','share','foo'], ['usr','share','bar'])
    (['usr','share'], [['foo'], ['bar']])
    """
    if common is None:
        common = []
    if len(l1) < 1 or len(l2) < 1 or  l1[0] != l2[0]:
        return (common, [l1, l2])
    return commonPrefix(l1[1:], l2[1:], common+[l1[0]])

def sosRelPath(path1, path2, sep=os.path.sep, pardir=os.path.pardir):
    ''' return a relative path from path1 equivalent to path path2.
        In particular: the empty string, if path1 == path2;
                       path2, if path1 and path2 have no common prefix.
    '''
    try:
        common, (u1, u2) = commonPrefix(path1.split(sep), path2.split(sep))
    except AttributeError:
        return path2

    if not common:
        return path2      # leave path absolute if nothing at all in common
    return sep.join( [pardir]*len(u1) + u2 )


class PluginException(Exception):
    pass


class Plugin(object):
    """
    This is the base class for sosreport plugins. This class should
    be subclassed by platform specific superclasses. Actual plugins
    should not subclass this class directly.
    """

    requires_root = True
    version = 'unversioned'

    def __init__(self, commons):
        if not getattr(self, "optionList", False):
            self.optionList = deque()

        self.copiedFiles = deque()
        self.executedCommands = deque()
        self.diagnose_msgs = deque()
        self.alerts = deque()
        self.customText = ""
        self.optNames = deque()
        self.optParms = deque()
        self.cInfo = commons
        self.forbiddenPaths = deque()
        self.copyPaths = deque()
        self.copyStrings = deque()
        self.collectProgs = deque()

        self.packages = deque()
        self.files = deque()

        self.must_exit = False

        self.soslog = logging.getLogger('sos')
        self.proflog = logging.getLogger('sosprofile')

        # get the option list into a dictionary
        for opt in self.optionList:
            self.optNames.append(opt[0])
            self.optParms.append({'desc':opt[1], 'speed':opt[2], 'enabled':opt[3]})

    @classmethod
    def name(class_):
        "Returns the plugin's name as a string"
        return class_.__name__.lower()

    def setArchive(self, archive):
        self.archive = archive

    def policy(self):
        return self.cInfo["policy"]

    def isInstalled(self, package_name):
        '''Is the package $package_name installed?
        '''
        return (self.policy().pkgByName(package_name) is not None)

    def doRegexSub(self, srcpath, regexp, subst):
        '''Apply a regexp substitution to a file archived by sosreport.
        srcpath is the path in the archive where the file can be found.
        regexp can be a regexp string or a compiled re object.
        subst is a string to replace each occurance of regexp in the content
        of srcpath.

        This function returns the number of replacements made.
        '''
        try:
            path = self._get_dest_for_srcpath(srcpath)
            if not path:
                return 0
            readable = self.archive.open_file(path)
            result, replacements = re.subn(regexp, subst, readable.read())
            if replacements:
                self.archive.add_string(result, srcpath)
                return replacements
            else:
                return 0
        except Exception:
            return 0

    def doRegexFindAll(self, regex, fname):
        ''' Return a list of all non overlapping matches in the string(s)
        '''
        try:
            return re.findall(regex, open(fname, 'r').read(), re.MULTILINE)
        except:  # IOError, AttributeError, etc.
            return []

    def _path_in_path_list(self, path, path_list):
        for p in path_list:
            if p in path:
                return True
        return False

    def copy_symlink(self, srcpath, sub=None):
        link = os.readlink(srcpath)
        if not os.path.isabs(link):
            link = os.path.normpath(
                    os.path.join(
                        os.path.dirname(srcpath),
                        link)
                    )

        if os.path.isdir(link):
            self.soslog.debug("link %s is a directory, skipping..." % link)
            return

        dest = link

        if sub:
            old, new = sub
            dest = srcpath.replace(old, new)

        self.archive.add_file(link, dest=dest)

        self.copiedFiles.append({
            'srcpath':srcpath,
            'dstpath':dest,
            'symlink':"yes",
            'pointsto':link})

    def copy_dir(self, srcpath, sub=None):
        for afile in os.listdir(srcpath):
            self.doCopyFileOrDir(os.path.join(srcpath, afile), dest=None, sub=sub)

    def _get_dest_for_srcpath(self, srcpath):
        for copied in self.copiedFiles:
            if srcpath == copied["srcpath"]:
                return copied["dstpath"]
        return None

    # Methods for copying files and shelling out
    def doCopyFileOrDir(self, srcpath, dest=None, sub=None):
        # pylint: disable-msg = R0912
        # pylint: disable-msg = R0915
        '''
        Copy file or directory to the destination tree. If a directory,
        then everything below it is recursively copied. A list of copied files
        are saved for use later in preparing a report.  sub can be used to
        rename the destination of the file, sub should be a two-tuple of
        (old,new). For example if you passed in ("etc","configurations") for
        use against /etc/my_file.conf the file would end up at
        /configurations/my_file.conf.
        '''

        if self.cInfo['cmdlineopts'].profiler:
            start_time = time()

        if self._path_in_path_list(srcpath, self.forbiddenPaths):
            self.soslog.debug("%s is in the forbidden path list" % srcpath)
            return ''

        if not os.path.exists(srcpath):
            self.soslog.debug("file or directory %s does not exist" % srcpath)
            return

        if not dest:
            dest = srcpath

        if sub:
            old, new = sub
            dest = srcpath.replace(old, new)

        if os.path.islink(srcpath):
            self.copy_symlink(srcpath, sub=sub)
            return
        else:
            if os.path.isdir(srcpath):
                self.copy_dir(srcpath, sub=sub)
                return

        # if we get here, it's definitely a regular file (not a symlink or dir)
        self.soslog.debug("copying file %s to %s" % (srcpath,dest))

        try:
            self.archive.add_file(srcpath, dest)

            self.copiedFiles.append({
                'srcpath':srcpath,
                'dstpath':dest,
                'symlink':"no"})

            if self.cInfo['cmdlineopts'].profiler:
                time_passed = time() - start_time
                self.proflog.debug("copied: %-75s time: %f" % (srcpath, time_passed))
        except Exception, e:
            self.soslog.debug(traceback.format_exc())


    def addForbiddenPath(self, forbiddenPath):
        """Specify a path to not copy, even if it's part of a copyPaths[] entry.
        """
        # Glob case handling is such that a valid non-glob is a reduced glob
        for filespec in glob.glob(forbiddenPath):
            self.forbiddenPaths.append(filespec)

    def getAllOptions(self):
        """
        return a list of all options selected
        """
        return (self.optNames, self.optParms)

    def setOption(self, optionname, value):
        ''' set the named option to value.
        '''
        for name, parms in izip(self.optNames, self.optParms):
            if name == optionname:
                parms['enabled'] = value
                return True
        else:
            return False

    def isOptionEnabled(self, optionname):
        ''' Deprecated, use getOption() instead
        '''
        return self.getOption(optionname)

    def getOption(self, optionname, default=0):
        """Returns the first value that matches 'optionname' in parameters
        passed in via the command line or set via set_option or via the
        global_plugin_options dictionary, in that order.

        optionaname may be iterable, in which case the first option that matches
        any of the option names is returned."""

        def _check(key):
            if hasattr(optionname, "__iter__"):
                return key in optionname
            else:
                return key == optionname

        for name, parms in izip(self.optNames, self.optParms):
            if _check(name):
                val = parms['enabled']
                if val != None:
                    return val

        for key, value in self.cInfo.get('global_plugin_options', {}).iteritems():
            if _check(key):
                return value

        return default

    def getOptionAsList(self, optionname, delimiter=",", default=None):
        '''Will try to return the option as a list separated by the delimiter'''
        option = self.getOption(optionname)
        try:
            opt_list = [opt.strip() for opt in option.split(delimiter)]
            return filter(None, opt_list)
        except Exception:
            return default

    def addCopySpecLimit(self, fname, sizelimit=None, sub=None):
        """Add a file specification (with limits)
        """
        if not ( fname and len(fname) ):
            # self.soslog.warning("invalid file path")
            return False
        files = glob.glob(fname)
        files.sort()
        cursize = 0
        limit_reached = False
        sizelimit *= 1024 * 1024 # in MB
        for flog in files:
            cursize += os.stat(flog)[ST_SIZE]
            if sizelimit and cursize > sizelimit:
                limit_reached = True
                break
            self.addCopySpec(flog, sub)
        # Truncate the first file (others would likely be compressed),
        # ensuring we get at least some logs
        # FIXME: figure this out for jython
        if flog == files[0] and limit_reached:
            self.collectExtOutput("tail -c%d %s" % (sizelimit, flog),
                "tail_" + os.path.basename(flog), flog[1:] + ".tailed")

    def addCopySpecs(self, copyspecs, sub=None):
        for copyspec in copyspecs:
            self.addCopySpec(copyspec, sub)

    def addCopySpec(self, copyspec, sub=None):
        """ Add a file specification (can be file, dir,or shell glob) to be
        copied into the sosreport by this module
        """
        if not (copyspec and len(copyspec)):
            # self.soslog.warning("invalid file path")
            return False
        # Glob case handling is such that a valid non-glob is a reduced glob
        for filespec in glob.glob(copyspec):
            if filespec not in self.copyPaths:
                self.copyPaths.append((filespec, sub))

    def callExtProg(self, prog):
        """ Execute a command independantly of the output gathering part of
        sosreport
        """
        # pylint: disable-msg = W0612
        status, shout, runtime = sosGetCommandOutput(prog)
        return (status, shout, runtime)

    def checkExtprog(self, prog):
        """ Execute a command independently of the output gathering part of
        sosreport and check the return code. Return True for a return code of 0
        and False otherwise."""
        (status, output, runtime) = self.callExtProg(prog)
        return (status == 0)


    def collectExtOutput(self, exe, suggest_filename = None, root_symlink = None, timeout = 300):
        """
        Run a program and collect the output
        """
        self.collectProgs.append( (exe, suggest_filename, root_symlink, timeout) )

    def fileGrep(self, regexp, fname):
        try:
            return [l for l in open(fname).readlines() if re.match(regexp, l)]
        except:  # IOError, AttributeError, etc.
            return []

    def mangleCommand(self, exe):
        # FIXME: this can be improved
        mangledname = re.sub(r"^/(usr/|)(bin|sbin)/", "", exe)
        mangledname = re.sub(r"[^\w\-\.\/]+", "_", mangledname)
        mangledname = re.sub(r"/", ".", mangledname).strip(" ._-")[0:64]
        return mangledname

    def makeCommandFilename(self, exe):
        """ The internal function to build up a filename based on a command """

        outfn = os.path.join(self.cInfo['cmddir'], self.name(), self.mangleCommand(exe))

        # check for collisions
        if os.path.exists(outfn):
            inc = 2
            while True:
               newfn = "%s_%d" % (outfn, inc)
               if not os.path.exists(newfn):
                  outfn = newfn
                  break
               inc +=1

        return outfn

    def addStringAsFile(self, content, filename):
        """Add a string to the archive as a file named `filename`"""
        self.copyStrings.append((content, filename))

    def collectOutputNow(self, exe, suggest_filename=None, root_symlink=False, timeout=300):
        """ Execute a command and save the output to a file for inclusion in
        the report
        """
        if self.cInfo['cmdlineopts'].profiler:
            start_time = time()

        # pylint: disable-msg = W0612
        status, shout, runtime = sosGetCommandOutput(exe, timeout=timeout)

        if suggest_filename:
            outfn = self.makeCommandFilename(suggest_filename)
        else:
            outfn = self.makeCommandFilename(exe)

        if not (status == 127 or status == 32512): # if not command_not_found
            outfn_strip = outfn[len(self.cInfo['cmddir'])+1:]
            self.archive.add_string(shout, outfn)
            if root_symlink:
                self.archive.add_link(outfn, root_symlink)
        else:
            self.soslog.debug("could not run command: %s" % exe)
            outfn = None
            outfn_strip = None

        # save info for later
        self.executedCommands.append({'exe': exe, 'file':outfn_strip}) # save in our list
        self.cInfo['xmlreport'].add_command(cmdline=exe,exitcode=status,f_stdout=outfn_strip,runtime=runtime)

        if self.cInfo['cmdlineopts'].profiler:
            time_passed = time() - start_time
            self.proflog.debug("output: %-75s time: %f" % (exe, time_passed))

        return outfn

    # For adding warning messages regarding configuration sanity
    def addDiagnose(self, alertstring):
        """ Add a configuration sanity warning for this plugin. These
        will be displayed on-screen before collection and in the report as well.
        """
        self.diagnose_msgs.append(alertstring)

    # For adding output
    def addAlert(self, alertstring):
        """ Add an alert to the collection of alerts for this plugin. These
        will be displayed in the report
        """
        self.alerts.append(alertstring)

    def addCustomText(self, text):
        """ Append text to the custom text that is included in the report. This
        is freeform and can include html.
        """
        self.customText += text

    def copyStuff(self):
        """
        Collect the data for a plugin
        """
        for path, sub in self.copyPaths:
            self.doCopyFileOrDir(path, sub=sub)

        for string, file_name in self.copyStrings:
            try:
                self.archive.add_string(string,
                        os.path.join('sos_strings', self.name(), file_name))
            except Exception, e:
                self.soslog.debug("could not create %s, traceback follows: %s" % (file_name, e))

        for progs in izip(self.collectProgs):
            prog, suggest_filename, root_symlink, timeout = progs[0]
            # self.soslog.debug("collecting output of '%s'" % prog)
            try:
                self.collectOutputNow(prog, suggest_filename, root_symlink, timeout)
            except Exception, e:
                self.soslog.debug("error collection output of '%s', traceback follows: %s" % (prog, e))

    def exit_please(self):
        """ This function tells the plugin that it should exit ASAP"""
        self.must_exit = True

    def get_description(self):
        """ This function will return the description for the plugin"""
        try:
            return self.__doc__.strip()
        except:
            return "<no description available>"

    def checkenabled(self):
        """ This function can be overidden to let the plugin decide whether
        it should run or not.
        """
        # some files or packages have been specified for this package
        if len(self.files) or len(self.packages):
            for fname in self.files:
                if os.path.exists(fname):
                    return True
            for pkgname in self.packages:
                if self.isInstalled(pkgname):
                    return True
            return False

        return True

    def defaultenabled(self):
        """This devices whether a plugin should be automatically loaded or
        only if manually specified in the command line."""
        return True

    def diagnose(self):
        """This method must be overridden to check the sanity of the system's
        configuration before the collection begins.
        """
        pass

    def setup(self):
        """This method must be overridden to add the copyPaths, forbiddenPaths,
        and external programs to be collected at a minimum.
        """
        pass

    def analyze(self):
        """
        perform any analysis. To be replaced by a plugin if desired
        """
        pass

    def postproc(self):
        """
        perform any postprocessing. To be replaced by a plugin if desired
        """
        pass

    def report(self):
        """ Present all information that was gathered in an html file that allows browsing
        the results.
        """
        # make this prettier
        html = '<hr/><a name="%s"></a>\n' % self.name()

        # Intro
        html = html + "<h2> Plugin <em>" + self.name() + "</em></h2>\n"

        # Files
        if len(self.copiedFiles):
            html = html + "<p>Files copied:<br><ul>\n"
            for afile in self.copiedFiles:
                html = html + '<li><a href="%s">%s</a>' % (afile['dstpath'], afile['srcpath'])
                if (afile['symlink'] == "yes"):
                    html = html + " (symlink to %s)" % afile['pointsto']
                html = html + '</li>\n'
            html = html + "</ul></p>\n"

        # Command Output
        if len(self.executedCommands):
            html = html + "<p>Commands Executed:<br><ul>\n"
            # convert file name to relative path from our root
            for cmd in self.executedCommands:
                if cmd["file"] and len(cmd["file"]):
                    cmdOutRelPath = sosRelPath(self.cInfo['rptdir'], self.cInfo['cmddir'] + "/" + cmd['file'])
                    html = html + '<li><a href="%s">%s</a></li>\n' % (cmdOutRelPath, cmd['exe'])
                else:
                    html = html + '<li>%s</li>\n' % (cmd['exe'])
            html = html + "</ul></p>\n"

        # Alerts
        if len(self.alerts):
            html = html + "<p>Alerts:<br><ul>\n"
            for alert in self.alerts:
                html = html + '<li>%s</li>\n' % alert
            html = html + "</ul></p>\n"

        # Custom Text
        if (self.customText != ""):
            html = html + "<p>Additional Information:<br>\n"
            html = html + self.customText + "</p>\n"

        return html


class RedHatPlugin(object):
    """Tagging class to indicate that this plugin works with Red Hat Linux"""
    pass

class UbuntuPlugin(object):
    """Tagging class to indicate that this plugin works with Ubuntu Linux"""
    pass

class DebianPlugin(object):
    """Tagging class to indicate that this plugin works with Debian Linux"""
    pass

class IndependentPlugin(object):
    """Tagging class that indicates this plugin can run on any platform"""
    pass

class AS7Mixin(object):
    """A mixin class that adds some helpful methods for AS7 related plugins"""

    class Request(object):

        def __init__(self, resource, operation="read-resource", parameters=None):
            self.resource = resource
            self.operation = operation
            if parameters:
                self.parameters = parameters
            else:
                self.parameters = {}

        def url_parts(self):
            """Generator function to split a url into (key, value) tuples. The url
            should contain an even number of pairs.  In the case of / the generator
            will immediately stop iteration."""
            parts = self.resource.strip("/").split("/")

            if parts == ['']:
                raise StopIteration

            while parts:
                yield (parts.pop(0), parts.pop(0))

    def get_jboss_home(self):
        return self.getOption(('home', 'as7_home')) or os.getenv("JBOSS_HOME", None)

    def query(self, request_obj):
        try:
            return self.query_java(request_obj)
        except Exception, e:
            self.addAlert("JBOSS API call failed, falling back to HTTP: %s" % e)
            return self.query_http(request_obj)

    def _get_opt(self, first, second, default=None):
        val = self.getOption(first)
        if val:
            return val
        val = self.getOption(second)
        if val:
            return val
        return default

    def query_java(self, request_obj):
        from org.jboss.dmr import ModelNode
        controller_client = self.getOption('controller_client_proxy')
        if not controller_client:
            raise AttributeError("Controller Client is not available")

        request = ModelNode()
        request.get("operation").set(request_obj.operation)

        for key, val in request_obj.url_parts():
            request.get('address').add(key,val)

        if request_obj.parameters:
            for key, value in request_obj.parameters.iteritems():
                request.get(key).set(value)

        return controller_client.execute(request).toJSONString(True)

    def query_http(self, request_obj, postdata=None):
        host = self._get_opt('host', 'as7_host')
        port = self._get_opt('port', 'as7_port')

        username = self._get_opt('user', 'as7_user')
        password = self._get_opt('pass', 'as7_pass')

        uri = "http://%s:%s/management" % (host,port)

        json_data = {'operation': request_obj.operation,
                     'address': []}

        for key, val in request_obj.url_parts():
            json_data['address'].append({key:val})

        for key, val in request_obj.parameters.iteritems():
            json_data[key] = val

        postdata = json.dumps(json_data)
        headers = {'Content-Type': 'application/json',
                   'Accept': 'application/json'}

        opener = urllib2.build_opener()

        if username and password:
            passwd_manager = urllib2.HTTPPasswordMgrWithDefaultRealm()
            passwd_manager.add_password(realm="ManagementRealm",
                                        uri=uri,
                                        user=username,
                                        passwd=password)
            digest_auth_handler = urllib2.HTTPDigestAuthHandler(passwd_manager)
            basic_auth_handler = urllib2.HTTPBasicAuthHandler(passwd_manager)

            opener.add_handler(digest_auth_handler)
            opener.add_handler(basic_auth_handler)

        req = urllib2.Request(uri, data=postdata, headers=headers)

        try:
            resp = opener.open(req)
            return resp.read()
        except Exception, e:
            err_msg = "Could not query url: %s; error: %s" % (uri, e)
            self.addAlert(err_msg)
            return err_msg

    def set_domain_info(self, parameters=None):
        """This function will add host controller and server instance
        name data if it is present to the desired resource. This is to support
        domain-mode operation in AS7"""
        host_controller_name = self.getOption("as7_host_controller_name")
        server_name = self.getOption("as7_server_name")

        if host_controller_name and server_name:
            if not parameters:
                parameters = {}

            parameters['host'] = host_controller_name
            parameters['server'] = server_name

        return parameters


    def resource_to_file(self, resource=None, parameters=None, operation='read-resource', outfile=None):
        parameters = self.set_domain_info(parameters)

        r = self.Request(resource=resource,
                    parameters=parameters,
                    operation=operation)
        self.addStringAsFile(self.query(r), filename=outfile)


def import_plugin(name):
    """Import name as a module and return a list of all classes defined in that
    module"""
    try:
        plugin_fqname = "sos.plugins.%s" % name
        return import_module(plugin_fqname, superclass=Plugin)
    except ImportError, e:
        return None
