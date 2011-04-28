#ifndef I18N_PHONENUMBERS_CONFIG_H_
# define I18N_PHONENUMBERS_CONFIG_H_

# ifdef __x86_64__

// Needed by atomicops
#   define ARCH_CPU_64_BITS

# endif

#ifdef __APPLE__

# ifdef __GNUC__
#  ifndef COMPILER_GCC
#     define COMPILER_GCC
#  endif
# endif

# if defined(__i386__) || defined(__x86_64__)
#   ifndef ARCH_CPU_X86_FAMILY
#     define ARCH_CPU_X86_FAMILY
#   endif
# endif

# ifdef __arm__
#   ifndef ARCH_CPU_ARM_FAMILY
#     define ARCH_CPU_ARM_FAMILY
#   endif
# endif

# ifndef OS_MACOSX
#   define OS_MACOSX
# endif

# ifndef OS_POSIX
#   define OS_POSIX
# endif

# ifndef USE_TR1_UNORDERED_MAP
#   define USE_TR1_UNORDERED_MAP
# endif

#endif

#endif
