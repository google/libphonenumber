prefix=@CMAKE_INSTALL_PREFIX@
exec_prefix=@CMAKE_INSTALL_PREFIX@
libdir=@CMAKE_INSTALL_FULL_LIBDIR@
includedir=@CMAKE_INSTALL_FULL_INCLUDEDIR@

Name: Libphonenumber
Description: Google's Phonenumber library
Version: @CMAKE_PROJECT_VERSION@
Requires: @PROTOBUF_LIB@
Libs: -L${libdir} -lphonenumber
Cflags: -I${includedir}
