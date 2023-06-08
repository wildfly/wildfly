# Copyright 2023 Red Hat, Inc.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import re
import os
import shutil
import sys
from argparse import ArgumentParser, Namespace
from datetime import datetime, timezone
from typing import Tuple
from pathlib import Path, PurePath
from xml.etree import ElementTree
from xml.etree.ElementTree import SubElement

from bs4 import BeautifulSoup, Tag

REDIRECT_TEMPLATE: Tuple[str, str, str, str, str] = (
    """<!DOCTYPE html>
<html lang=\"en\">
  <head>
    <meta charset=\"UTF-8\">
    <meta http-equiv=\"refresh\" content=\"0; url=""",
    """\">
    <link rel=\"canonical\" href=\"""",
    """\" />
  </head>
  <body>
    <p>Redirecting to the latest version. If the page doesn't open, click the following link: <a href=\"""",
    """\">""",
    """</a></p>
  </body>
</html>""",
)

REMOVED_REDIRECT_TEMPLATE: Tuple[str, str, str, str, str, str] = (
    """<!DOCTYPE html>
<html lang=\"en\">
  <head>
    <meta charset=\"UTF-8\">
    <meta http-equiv=\"refresh\" content=\"3; url=""",
    """\">
    <link rel=\"canonical\" href=\"""",
    """\" />
  </head>
  <body>
    <p>This page does not exist in the latest version (""",
    """). You will be redirected to the last available version in 3 seconds.</p>
    <p>If the page doesn't open, click the following link: <a href=\"""",
    """\">""",
    """</a></p>
  </body>
</html>""",
)

REL_PATH_PATTERN = re.compile(r"(/)?([^/]+)")

SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9"


def rename_directories(
    base_docs_path: Path,
    latest_docs_path: PurePath,
    args: Namespace,
) -> None:
    """Adjust names of the latest directories.

    :param base_docs_path: base directory for documentation
    :param latest_docs_path: directory for latest docs. If the directory already exists, it is renamed to the previous
        version (if provided as an argument) or deleted
    :param args: paths and versions for current and previous documentation
    """
    if Path(latest_docs_path).exists():
        # Rename to previous directory version if provided
        if args.previous_version:
            prev_relative_path = base_docs_path.joinpath(args.previous_version)

            shutil.rmtree(prev_relative_path) if prev_relative_path.exists() else None
            os.rename(latest_docs_path, prev_relative_path)
        else:
            shutil.rmtree(latest_docs_path)

    os.rename(args.source, latest_docs_path)
    if (versioned_path := base_docs_path.joinpath(args.source_version)).exists():
        shutil.rmtree(versioned_path)


def rel_path_replace(matched: re.Match) -> str:
    """Replaces path elements with ".." for relative URLs

    :param matched: the match element
    :return: a replacement for ascending levels in a relative path
    """
    replacement: str = ""
    for m in matched.groups():
        if m is not None:
            replacement += ".." if os.sep not in m else os.sep

    return replacement


def generate_redirect_url(
    base_docs_path: Path,
    canon_file: PurePath,
    canon_dir_path: PurePath,
    redirect_dirname: str,
) -> Tuple[PurePath, str]:
    """Returns relative URLs for two files with identical paths, except for one level

    :param base_docs_path: base directory for documentation. Both files have an identical path to this point
    :param canon_file: full path of the canonical file
    :param canon_dir_path: full path of the directory immediately below :py:data:`base_docs_path`
        containing :py:data:`canon_file`
    :param redirect_dirname: name of the directory immediately below :py:data:`base_docs_path`
        containing the redirect file
    """
    redirect_file = base_docs_path.joinpath(redirect_dirname).joinpath(
        canon_file.relative_to(canon_dir_path)
    )

    # Generated a relative path via the base directory (ex. "../../..")
    redirect_url = PurePath(
        re.sub(
            REL_PATH_PATTERN,
            rel_path_replace,
            str(redirect_file.relative_to(base_docs_path)),
        )
    )
    redirect_url = redirect_url.joinpath(canon_file.relative_to(base_docs_path))

    return redirect_file, str(redirect_url).replace(os.sep, "/")


def create_redirect(
    base_docs_path: Path,
    latest_docs_path: PurePath,
    latest_file: PurePath,
    version: str,
    use_abs_path: bool,
) -> None:
    """Create a versioned redirect for a new documentation page.

    :param base_docs_path: base directory for documentation
    :param latest_docs_path: directory for latest docs
    :param latest_file: the file a redirect is being created for
    :param version: name of the latest version
    :param use_abs_path: sets redirects to use relative or absolute paths
    """
    versioned_file, relative_redirect = generate_redirect_url(
        base_docs_path, latest_file, latest_docs_path, version
    )
    if use_abs_path:
        relative_redirect = "/" + str(latest_file.relative_to(base_docs_path)).replace(
            os.sep, "/"
        )

    os.makedirs(versioned_file.parent, exist_ok=True)
    with open(versioned_file, "w") as redirect_file:
        redirect_file.write(relative_redirect.join(REDIRECT_TEMPLATE))


def redirect_removed_file(
    base_docs_path: Path,
    previous_docs_file: PurePath,
    current_version: str,
    previous_version: str,
    use_abs_path: bool,
    product_name: str | None,
) -> None:
    """Create a redirect for a page found in the previous version, but not the current.

    This method also propagates static redirects found in the previous version.

    :param base_docs_path: base directory for documentation
    :param previous_docs_file: path of a file present in the previous version
    :param current_version: name of the latest version
    :param previous_version: name of the previous version
    :param use_abs_path: sets redirects to use relative or absolute paths
    :param product_name: name of the product, if provided
    """
    redirect_file, redirect_url = generate_redirect_url(
        base_docs_path,
        previous_docs_file,
        base_docs_path.joinpath(previous_version),
        "latest",
    )
    if use_abs_path:
        redirect_url = "/" + str(
            previous_docs_file.relative_to(base_docs_path)
        ).replace(os.sep, "/")

    # For versioned page of current docs
    versioned_redirect_file = generate_redirect_url(
        base_docs_path,
        previous_docs_file,
        base_docs_path.joinpath(previous_version),
        current_version,
    )[0]

    if not Path(redirect_file).exists():
        redirect_file_contents: str

        # Extract existing static redirect in the "canonical" page, and use as redirect instead
        with open(previous_docs_file, "r") as prev_file_io:
            prev_file = BeautifulSoup(prev_file_io.read(), "html.parser")
            if static_redirect := prev_file.find(
                "meta", attrs={"http-equiv": "refresh"}
            ):
                canon_link = prev_file.find("link", rel="canonical")

                if canon_link and canon_link["href"] in static_redirect["content"]:
                    redirect_url = canon_link["href"]

        if product_name:
            redirect_file_contents = (
                redirect_url.join(REMOVED_REDIRECT_TEMPLATE[0:3])
                + f"{product_name} {current_version}"
                + redirect_url.join(REMOVED_REDIRECT_TEMPLATE[3:6])
            )
        else:
            redirect_file_contents = (
                redirect_url.join(REMOVED_REDIRECT_TEMPLATE[0:3])
                + str(current_version)
                + redirect_url.join(REMOVED_REDIRECT_TEMPLATE[3:6])
            )

        os.makedirs(redirect_file.parent, exist_ok=True)
        with open(redirect_file, "w") as red_file_io:
            red_file_io.write(redirect_file_contents)

        os.makedirs(versioned_redirect_file.parent, exist_ok=True)
        with open(versioned_redirect_file, "w") as ver_red_file_io:
            ver_red_file_io.write(redirect_file_contents)


def redirect_walker(
    base_docs_path: Path, latest_docs_path: PurePath, version: str, use_abs_path: bool
) -> None:
    """Walk the files contained within the "latest/" directory and create redirects for the versioned path.

    Redirects are only created for HTML pages, since they do not consistently work with images, stylesheets, etc.

    :param base_docs_path: base directory for documentation
    :param latest_docs_path: directory with new documentation
    :param version: name of the latest version
    :param use_abs_path: sets redirects to use relative or absolute paths
    """

    for parent, dirs, files in os.walk(latest_docs_path):
        for f in files:
            if f.endswith(".html"):
                create_redirect(
                    base_docs_path,
                    latest_docs_path,
                    PurePath(parent, f),
                    version,
                    use_abs_path,
                )


def redirect_previous_versions(
    base_docs_path: Path,
    previous_docs_path: PurePath,
    current_version: str,
    previous_version: str,
    use_abs_path: bool,
    product_name: str | None,
) -> None:
    """Create redirects from the previous version of the documentation, if pages have been removed.

    :param base_docs_path: base directory for documentation
    :param previous_docs_path: directory with documentation for previous version
    :param current_version: name of the current version
    :param previous_version: name of the previous version
    :param use_abs_path: sets redirects to use relative or absolute paths
    :param product_name: name of the product, if provided
    """
    for parent, dirs, files in os.walk(previous_docs_path):
        for f in files:
            if f.endswith("html"):
                redirect_removed_file(
                    base_docs_path,
                    PurePath(parent, f),
                    current_version,
                    previous_version,
                    use_abs_path,
                    product_name,
                )


def update_sitemap(base_docs_path: Path) -> None:
    """Update a file sitemap.xml, located in the same folder as :py:data:`base_docs_path`.

    Sets `lastmod` on all URLs to today's date.

    :param base_docs_path: base directory, containing sitemap.xml
    """
    ElementTree.register_namespace("", SITEMAP_NS)
    revdate = str(datetime.now(tz=timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"))

    sitemap_path = base_docs_path.joinpath("sitemap.xml")
    tree = ElementTree.parse(sitemap_path)

    for link in tree.getroot().findall(f"{{{SITEMAP_NS}}}url"):
        for lastmod in link.findall(f"{{{SITEMAP_NS}}}lastmod"):
            link.remove(lastmod)

        lastmod = SubElement(link, f"{{{SITEMAP_NS}}}lastmod")
        lastmod.text = revdate

    ElementTree.indent(tree)
    tree.write(
        sitemap_path,
        xml_declaration=True,
        encoding="utf-8",
    )

    with open(sitemap_path, "r") as sitemap:
        decl = sitemap.readline()
        remainder = sitemap.readlines()

    decl = decl.replace("'", '"')
    decl_end = decl.rfind("?")
    decl = decl[:decl_end] + ' standalone="yes" ' + decl[decl_end:]
    remainder.insert(0, decl)

    with open(sitemap_path, "w") as sitemap:
        sitemap.writelines(remainder)


def setup_argparse() -> ArgumentParser:
    """Loads args into the argument parser.

    :return: The argument parser
    """
    parser = ArgumentParser(
        prog="update-latest-and-redirects",
        description="Use `latest` as a canonical path for current documentation. (ex. wildfly.org/latest/)."
        + " All paths are relative to the parent directory of -s, --source.",
    )
    parser.add_argument(
        "-s",
        "--source",
        metavar="directory",
        help="Required. Directory containing the latest documentation. This directory will be renamed to `latest`. "
        + "Any existing directory named `latest` will be removed, unless renamed (see -p, --previous).",
        type=lambda p: Path(p).resolve(),
        required=True,
    )
    parser.add_argument(
        "-sv",
        "--source-version",
        metavar="version",
        help="Required. Version of the latest documentation (ex. 28). To be used for versioned redirects. "
        + " Any existing directory with this name will be deleted.",
        required=True,
    )
    parser.add_argument(
        "-p",
        "--previous",
        metavar="directory",
        help="Folder with canonical links to the previous version of the documentation. If previously configured, "
        "this argument should be `latest`. If used, the argument -pv, --previous-version is also required.",
    )
    parser.add_argument(
        "-pv",
        "--previous-version",
        metavar="version",
        help="Version of the previous documentation (ex. 27). To be used for redirecting to pages not present in "
        + "the latest version. Any existing directory with this name will be deleted. If used, the argument "
        + "-p, --previous is also required.",
    )
    parser.add_argument(
        "--no-absolute-path",
        help="Do not treat the parent directory of -s, --source as a top level path element when generating redirects. "
        + "Ex. a source directory at latest-new/ will create a redirect of the form `../latest/index.html` "
        + "instead of `/latest/index.html`. This may break some redirects.",
        dest="absolute_path",
        action="store_false",
    )
    parser.add_argument(
        "--product",
        metavar="name",
        help="Name of the product (ex. WildFly), used on redirect pages.",
    )
    parser.add_argument(
        "--update-sitemap",
        help="Update a file sitemap.xml, located in the same folder as -s, --source.  Sets `lastmod` on all URLs "
        + "to today's date.",
        action="store_true",
    )

    return parser


def main():
    args = setup_argparse().parse_args()

    if (args.previous and not args.previous_version) or (
        args.previous_version and not args.previous
    ):
        print(
            "Both -p, --previous and -pv, --previous-version are required if either argument is provided",
            file=sys.stderr,
        )
        sys.exit(1)
    if not args.source.exists():
        print(f"{args.source} is not a valid directory", file=sys.stderr)
        sys.exit(1)

    base_docs_path = Path(args.source.parent)
    latest_docs_path = base_docs_path.joinpath("latest")

    rename_directories(base_docs_path, latest_docs_path, args)
    redirect_walker(
        base_docs_path, latest_docs_path, args.source_version, args.absolute_path
    )

    if args.previous:
        previous_docs_path = base_docs_path.joinpath(args.previous_version)
        redirect_previous_versions(
            base_docs_path,
            previous_docs_path,
            args.source_version,
            args.previous_version,
            args.absolute_path,
            args.product,
        )

    if args.update_sitemap:
        update_sitemap(base_docs_path)


main()
