#ifndef DEBUG
#define DEBUG 0
#endif

#ifdef DEBUG

#include <android/log.h>

#define D_INFO  ANDROID_LOG_INFO
#define D_WARN  ANDROID_LOG_WARN
#define D_ERROR ANDROID_LOG_ERROR
#define D(lvl, msg, args...) if(lvl) { __android_log_print(lvl, "JS", msg, ## args ); __android_log_print(lvl, "JS", "\n"); }

#else

#define D(lvl, msg, args...)

#endif
