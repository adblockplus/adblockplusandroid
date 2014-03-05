LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libadblockplus
LOCAL_SRC_FILES := ./libadblockplus-binaries/android_arm/libadblockplus.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := v8-base
LOCAL_SRC_FILES := ./libadblockplus-binaries/android_arm/libv8_base.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := v8-snapshot
LOCAL_SRC_FILES := ./libadblockplus-binaries/android_arm/libv8_snapshot.a

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE := abpEngine
LOCAL_SRC_FILES := AbpEngine.cpp Utils.cpp AndroidLogSystem.cpp AndroidWebRequest.cpp

LOCAL_CPP_FEATURES := rtti exceptions
LOCAL_CFLAGS += -std=gnu++0x

LOCAL_C_INCLUDES := jni/libadblockplus-binaries/include/
LOCAL_STATIC_LIBRARIES := libadblockplus v8-base v8-snapshot
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
