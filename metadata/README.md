## Libphonenumber Metadata Tools

This directory contains auxiliary libraries to support reading and manipulating
the CSV packaged metadata for libphonenumber client library. The initial release
of these libraries is purely concerned with processing the CSV files, and does
not yet contain the classes needed to convert the CSV data in the XML and other
text files used by libphonenumber.

Eventually it is expected that all the tooling for manipulating CSV metadata and
generating the XML files will be released here, at which time the CSV files will
become the source of truth for libphonenumber and the XML-based metadata and
other mapping files will be automatically derived from it.

## About metadata.zip

It contains canonical metadata for the libphonenumber project, intended for use
by libphonenumber tools. CSV schemas are not promised to be stable.

### Support and Api Stability

**These libraries are not currently supported and do not provide a stable API;
use at your own risk.**

There are no guarantees of stability for the APIs in this library, and while
they are not expected to change dramatically, API tweaks and bug fixes are
inevitable.

### Issues and Contributions

Patches and pull requests cannot be accepted directly on this codebase, so if
you find an issue with these libraries, please open a new issue for it. However
we do not accept feature requests, or provide answers or technical support for
anything in this directory at this time.
