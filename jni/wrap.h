#include <v8.h>
#include <jni.h>

extern jobject NewBoolean(JNIEnv *pEnv, jboolean value);

extern jobject wrapJSObject(JNIEnv *pEnv, v8::Handle<v8::Value> value);
extern v8::Handle<v8::Value> wrapJavaObject(JNIEnv *pEnv, jobject value);
