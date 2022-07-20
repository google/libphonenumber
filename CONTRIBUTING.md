# Contributing to libphonenumber

Thanks for contributing to libphonenumber!

Please read the following before filing an issue or sending a pull request.

We hope these guidelines will enhance your experience as a contributor to our
library and know that we appreciate the time you put into making it better.

## Checklist before filing an issue

Please check the following:
*   Is the issue reproducible using the
    [demo](http://libphonenumber.appspot.com/)? If not:
    *   Your issue may be resolved by upgrading to the latest version of the
        library. (Reach out to the product/app team if you're not the developer!)
    *   If you are using a
        [port](http://github.com/google/libphonenumber#third-party-ports),
        issues should be reported against that project.
    *   If you are using the library through a front-end or wrapper, issues
        should be reported against that project.
*   Has the issue already been discussed in the
    [issues](http://issuetracker.google.com/issues/new?component=192347) or [discussion
    group](https://groups.google.com/group/libphonenumber-discuss)? If so, you
    may want to comment on an existing discussion.

## Filing a metadata issue

Please copy this template into your report and answer the questions to the best of your ability.
For acceptable evidence, [see below](#provide-supporting-evidence).

``` nomarkdown
*   Country:
*   Example number(s) and/or range(s):
*   Number type ("fixed-line", "mobile", "short code", etc.):
*   For short codes, cost and dialing restrictions:
*   Where or whom did you get the number(s) from:
*   Authoritative evidence (e.g. national numbering plan, operator announcement):
*   Link from demo (http://libphonenumber.appspot.com) showing error:
```

As of March 2018, we kindly request that you use Google's [Issue Tracker](http://issuetracker.google.com/issues/new?component=192347)
to report issues with the library. Using Issue Tracker will enable to us to
get back to you more efficiently as we align GitHub user issues with ones
reported at Google. Note that we transitioned active GitHub issues to Issue Tracker,
but anything inactive for over a year must be filed again. To check if your issue
was transitioned, see this [list of issues](https://issuetracker.google.com/hotlists/886952).

Alternatively, if you have a particular issue with a Google product handling phone numbers (e.g. signup),
you may also use the [Google Product Forums](https://productforums.google.com/forum).

### Provide authoritative evidence

Ideally, change requests for ranges should be accompanied by authoritative
evidence such as official government or public carrier documents.

If the evidence is publicly available online, please provide the link.

If the evidence is not publicly available online, make sure that you have the
rights to share this with us, and confirm this by reading and signing the
appropriate Contributor License Agreement (CLA). Then we can accept this
on GitHub, or we can invite you to a private Google Group archive if you
prefer not to share on GitHub.

See https://cla.developers.google.com/about to determine whether you need to
sign a Corporate or Individual CLA.

**By signing the CLA, you confirm that you have the rights to share the
information with us, and that we may use, modify, reproduce, publicly display,
and distribute the information in accordance with the library's [open source
license](http://github.com/google/libphonenumber/blob/master/LICENSE).**

*   [Corporate CLA](http://developers.google.com/open-source/cla/corporate?csw=1):
    *   See https://cla.developers.google.com/about and follow the instructions.
    *   Once you are all set up, either create a validation issue for your
        request, or go to the relevant open issue.
    *   Comment on the issue, using the email that's already set up for
        your Corporate CLA, with "I signed the CLA."
    *   If you're set up correctly, someone from the team will update you on the
        issue, with instructions for sending private documentation to our closed
        mailing list. Only the core libphonenumber team has access to view
        emails sent to this address.

*   [Individual CLA](http://cla.developers.google.com/about/google-individual?csw=1):
    *   See https://cla.developers.google.com/about to ensure that an Individual
        CLA is what you need to sign, and for help with troubleshooting.
    *   When signing the Individual CLA, use the same email you used
        (or will use) to file the corresponding issue, so that we may check
        and you don't have to post your correct email in public.
    *   Once you are all set up, either create a validation issue for your
        request, or go to the relevant open issue.
    *   If you're set up correctly, someone from the team will update you on the
        issue, with instructions for sending private documentation to our closed
        mailing list. Only the core libphonenumber team has access to view
        emails sent to this address.

## Pull requests

Since all metadata (XML and mapping files) is generated automatically from other
sources, we cannot accept pull requests to edit these files directly. This includes
validation, formatting, geocoding, operator or timezone metadata. However this does
not mean you cannot raise issues for this data.

For changes specific to windows builds, see [Filing a code
issue](#filing-a-code-issue) and make sure you have found a reviewer and tester
before sending the pull request. If the issue wasn’t reproducible and seems to be
specific to your build/setup, please prefer [creating a code bug](#filing-a-code-issue) or email the
[discussion group](https://groups.google.com/group/libphonenumber-discuss) than creating a pull request.

We are happy to review and accept pull requests for the following:

*   Bug fixes
*   Documentation and code hygiene cleanups

This list is not exhaustive. To clarify whether we'd accept a pull request, and
especially before spending significant time on one such as for a bug fix, we
strongly encourage that you bring up the question on an issue.

To get your pull request merged, we need the following:

*   A CLA signature. See the previous section.
*   If the email address you use in the Individual CLA is not already
    associated with that GitHub account, [add it](https://help.github.com/articles/adding-an-email-address-to-your-github-account/).
*   If you need to modify an Individual CLA signature, such as to change the
        GitHub username, go to https://cla.developers.google.com/clas and click
        on "Edit Contact Information".
*   Responsiveness to back and forth review comments, because we will do a code
    review.
*   A link to a clear description of what the pull request intends to solve in [Issue
    Tracker](http://issuetracker.google.com/issues/new?component=192347).
*   Tests that illustrate the issue, if applicable.
*   Willingness and ability to implement the changes in C++, Java, and JS if
    applicable. For example, changes to `PhoneNumberUtil.java` must be ported to
    `phonenumberutil.cc` and `phonenumberutil.js`.
    *   The initial pull request may contain the implementation in only one
        language to get approval on the approach.

If this is not possible, please file an issue instead.

## Filing a code issue

When filing a code issue, include the specifics of your operating system and
provide as much information as possible that helps us reproduce the problem.

Please be advised that metadata updates are prioritized over code changes,
except for bug fixes. In addition, we work with a limited number of build
systems and may not be able to support every setup.

In particular, we don't actively maintain windows builds but would be able to
accept a PR provided someone else in the open-source community could test it out
and would be able to help with the review. One way to look for such
collaborators would be to email the [discussion
group](https://groups.google.com/group/libphonenumber-discuss). Also see the
[known Windows issues](FAQ.md#what-about-windows).
