LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE		:= v8-base
LOCAL_SRC_FILES		:= ./v8/libv8_base.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    	:= v8-snapshot
LOCAL_SRC_FILES		:= ./v8/libv8_nosnapshot.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    	:= v8-preparser
LOCAL_SRC_FILES		:= ./v8/libpreparser_lib.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    	:= jsEngine
LOCAL_SRC_FILES 	:= jsEngine.cpp wrap.cpp fileOps.cpp timerOps.cpp utilityOps.cpp xhrOps.cpp

LOCAL_CPP_FEATURES 	:= #rtti #exceptions
LOCAL_CFLAGS += \
	-Wno-endif-labels \
	-Wno-import \
	-Wno-format \
	-fno-exceptions \
	-DENABLE_DEBUGGER_SUPPORT \
	-DV8_NATIVE_REGEXP \
	-fvisibility=hidden \
	-DARM \
	-DV8_TARGET_ARCH_ARM \
	-DENABLE_LOGGING_AND_PROFILING
	
LOCAL_C_INCLUDES 		:= jni/v8/
LOCAL_STATIC_LIBRARIES	:= v8-base v8-snapshot
LOCAL_LDLIBS 			:= -llog

include $(BUILD_SHARED_LIBRARY)


