#include <v8.h>
#include <jni.h>

extern jobject wrap(JNIEnv *pEnv, v8::Handle<v8::Value> value);
extern v8::Handle<v8::Value> wrap(JNIEnv *pEnv, jobject value);
