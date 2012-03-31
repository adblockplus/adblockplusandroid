#include <v8.h>
#include <jni.h>

extern JNIEnv* jniEnv;
extern jobject jniCallback;

extern v8::Handle<v8::Value> loadImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> printImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> setStatusImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> showToastImpl(const v8::Arguments& args);

extern v8::Handle<v8::Value> fileExistsImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileLastModifiedImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileRemoveImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileRenameImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileReadImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileWriteImpl(const v8::Arguments& args);

extern v8::Handle<v8::Value> httpSendImpl(const v8::Arguments& args);

extern v8::Handle<v8::Value> setTimeoutImpl(const v8::Arguments& args);
extern bool RunNextCallback(v8::Handle<v8::Context> context);
extern void ClearQueue();
