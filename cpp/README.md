
# libphonenumber C++ Library

A C++ port of the original Java [libphonenumber](https://github.com/google/libphonenumber) library.

This project includes some third-party code, such as sources from [lib9 UTF-8 package](https://github.com/golang/go/tree/master/src/internal/utf8) used in `src/phonenumbers/utf/`.

---

## Table of Contents

- [Installation on GNU/Linux](#installation-on-gnulinux)  
  - [Using Packages (Debian-based)](#using-packages-debian-based)  
  - [Manual Build](#manual-build)  
- [Installation on macOS](#installation-on-macos)  
- [Building and Testing](#building-and-testing)  
- [Troubleshooting](#troubleshooting)  
- [Building on Windows](#building-on-windows)  
- [Supported Build Options](#supported-build-options)

---

## Installation on GNU/Linux

### Using Packages (Debian-based)

If you want to simply use or link the library:

```bash
sudo apt-get install libphonenumber8 libphonenumber-dev
```

For development or debugging:

```bash
sudo apt-get source libphonenumber
```

Latest packages can be found at:  
[https://packages.debian.org/search?searchon=names&keywords=libphonenumber](https://packages.debian.org/search?searchon=names&keywords=libphonenumber)

---

### Manual Build

Use this if packages are unavailable or you want to build from source.

#### Prerequisites

- **CMake**

```bash
sudo apt-get install cmake cmake-curses-gui
```

- **Protocol Buffers (≥ 3.6.1)**

```bash
sudo apt-get install libprotobuf-dev protobuf-compiler
```

If unavailable, see: https://github.com/protocolbuffers/protobuf

- **Google Test**

```bash
sudo apt-get install libgtest-dev
```

- **RE2**

```bash
sudo apt-get install libre2-dev
```

- **ICU**

```bash
sudo apt-get install libicu-dev
```

- **Thread Synchronization (optional, for thread safety)**

Supported options:  
- Boost (≥ 1.40)  
- POSIX Threads (Linux/macOS)  
- C++11 `std::mutex` (`-DUSE_STDMUTEX=ON`)  
- Windows Win32 API

---

### On Debian, install Boost libraries:

```bash
sudo apt-get install libboost-dev libboost-thread-dev libboost-system-dev
```

Abseil-cpp is downloaded and built automatically but can also be installed manually:  
https://abseil.io/docs/cpp/tools/cmake-installs

---

## Installation on macOS

Install [Homebrew](https://brew.sh) if you don't have it:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

### Install dependencies:

```bash
brew install boost cmake icu4c pkg-config protobuf wget
```

### Clone GoogleTest (not available via Homebrew):

```bash
mkdir ~/googletest_clone
cd ~/googletest_clone
git clone https://github.com/google/googletest.git
```

### Clone libphonenumber:

```bash
mkdir ~/libphonenumber_clone
cd ~/libphonenumber_clone
git clone https://github.com/google/libphonenumber.git
```

---

## Building and Testing

```bash
cd libphonenumber/cpp
mkdir build && cd build
```

### Replace `XXX` with your ICU version number (e.g., `77`)

To find your ICU version number, run:

```bash
ls /usr/local/Cellar/icu4c/
```

If no versions are found, install ICU with:

```bash
brew install icu4c
```

Then replace `XXX` with the installed version number.

### Configure and build:

```bash
cmake   -DGTEST_SOURCE_DIR=~/googletest_clone/googletest/googletest/   -DGTEST_INCLUDE_DIR=~/googletest_clone/googletest/googletest/include/   -DICU_UC_INCLUDE_DIR=/usr/local/Cellar/icu4c/XXX/include/   -DICU_UC_LIB=/usr/local/Cellar/icu4c/XXX/lib/libicuuc.dylib   -DICU_I18N_INCLUDE_DIR=/usr/local/Cellar/icu4c/XXX/include/   -DICU_I18N_LIB=/usr/local/Cellar/icu4c/XXX/lib/libicui18n.dylib   -DUSE_STD_MAP=ON   ..

make
./libphonenumber_test
```

---

## Troubleshooting

### Protobuf Issues

If you encounter issues with Protocol Buffers, use `ccmake` or another CMake GUI to set:

```bash
PROTOBUF_INCLUDE_DIR  /usr/local/include
PROTOBUF_LIB          /usr/local/lib/libprotobuf.dylib
PROTOC_BIN            /usr/local/bin/protoc
```

### ICU Issues

Set ICU paths appropriately:

```bash
ICU_I18N_INCLUDE_DIR  /usr/local/include
ICU_I18N_LIB          /usr/local/lib/libicui18n.so
ICU_UC_INCLUDE_DIR    /usr/local/include
ICU_UC_LIB            /usr/local/lib/libicuuc.so
```

---

## Building on Windows

1. Use Visual Studio 2015 Update 2 or later.  
2. Manually install dependencies: CMake, Boost, GTest, ICU, Protobuf.  
3. Use `cmake-gui` to configure and generate project files.  
4. Build the `INSTALL` target to install the library.

---

## Supported Build Options

| Option              | Default | Description                                   |
|---------------------|---------|-----------------------------------------------|
| USE_ALTERNATE_FORMATS| ON      | Use alternate phone number formats             |
| USE_BOOST           | ON      | Use Boost libraries for threading               |
| USE_ICU_REGEXP      | ON      | Use ICU regular expressions                      |
| USE_LITE_METADATA   | OFF     | Generate smaller metadata without example numbers|
| USE_POSIX_THREAD    | OFF     | Use POSIX threads                                |
| USE_RE2             | OFF     | Use RE2 regex engine                             |
| USE_STD_MAP         | OFF     | Force use of std::map                            |
| USE_STDMUTEX        | OFF     | Use C++11 std::mutex                             |
| REGENERATE_METADATA | ON      | Regenerate metadata during build                 |
