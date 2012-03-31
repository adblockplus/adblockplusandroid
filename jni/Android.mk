LOCAL_PATH := $(call my-dir)

V8_HOME			:= /home/andrey/Workspace/v8

include $(CLEAR_VARS)

LOCAL_MODULE		:= v8-base
LOCAL_SRC_FILES		:= ./lib/libv8_base.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    	:= v8-snapshot
LOCAL_SRC_FILES		:= ./lib/libv8_nosnapshot.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    	:= v8-preparser
LOCAL_SRC_FILES		:= ./lib/libpreparser_lib.a

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
	
LOCAL_C_INCLUDES 		:= $(V8_HOME)/include
LOCAL_STATIC_LIBRARIES	:= v8-base v8-snapshot
LOCAL_LDLIBS 			:= -llog

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

# This is the target being built.
LOCAL_MODULE:= exec

# All of the source files that we will compile.
LOCAL_SRC_FILES:= \
  termExec.cpp

LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)

