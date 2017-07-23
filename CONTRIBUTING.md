# Contributing to libphonenumber

Thanks for contributing to libphonenumber!

Please read the following before filing an issue. **Note that we are _not_ accepting pull requests for metadata updates at this time.**

## Checklist before filing an issue

Please check the following:
* Has the issue already been discussed in the [open issues](https://github.com/googlei18n/libphonenumber/issues) or [discussion group](https://groups.google.com/group/libphonenumber-discuss)? If so, you may want to comment on an existing discussion.
* Is the issue reproducible using the [demo](http://libphonenumber.appspot.com/)? If not:
    + Your issue may be resolved by upgrading to the latest version of the library.
    + If you are using a [port](https://github.com/googlei18n/libphonenumber#known-ports), issues should be reported against that project.
    + If you are using the library through a front-end or wrapper, issues should be reported against that project.


## Filing a metadata issue

Please copy this template into your report and answer the questions to the best of your ability.
* Country/region affected (e.g., "US"):
* Example number(s) affected ("+1 555 555-1234"):
* The phone number range(s) to which the issue applies ("+1 555 555-XXXX"):
* The type of the number(s) ("fixed-line", "mobile", "short code", etc.):
* The cost, if applicable ("toll-free", "premium rate", "shared cost"):
* Supporting evidence (for example, national numbering plan, announcement from mobile carrier, news article): [see below](#provide-supporting-evidence)


### Provide supporting evidence

Ideally, change requests for ranges should be accompanied by authoritative evidence such as official government or public carrier documents.

If the evidence is publicly available online, please provide the link.

If the evidence is not publicly available online, make sure that you have the rights to share this with us, and confirm this by reading and signing the appropriate Contributor License Agreement (CLA):
* If you are contributing as an individual, sign the [individual CLA](https://cla.developers.google.com/about/google-individual?csw=1).
* If you are contributing as part of a corporation, sign the [corporate CLA](https://developers.google.com/open-source/cla/corporate?csw=1).

**By signing the CLA, you confirm that you have the rights to share the information with us to make it available under the library's [open source license](https://github.com/googlei18n/libphonenumber/blob/master/LICENSE).**

Once you have signed the CLA, you can request to be whitelisted to send emails to the [libphonenumber-supporting-docs](mailto:libphonenumber-supporting-docs@googlegroups.com) mailbox. Put the GitHub issue number in the subject line and attach the supporting documents. Only the core libphonenumber team has access to view emails sent to this mailbox.

## Filing a code issue

When filing a code issue, include the specifics of your operating system and provide as much information as possible that helps us reproduce the problem.

Please be advised that metadata updates are prioritized over code changes, except for bug fixes. In addition, we work with a limited number of build systems and may not be able to support every setup.
