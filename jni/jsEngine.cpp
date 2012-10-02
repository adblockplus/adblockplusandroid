#include <string>
#include <android/log.h>
#include <jni.h>
#include "wrap.h"
#include "ops.h"

const char* scriptDir;
const char* dataDir;
JNIEnv* jniEnv;
jobject jniCallback;

extern "C"
{
  JNIEXPORT jlong JNICALL Java_org_adblockplus_android_JSEngine_nativeInitialize(JNIEnv *pEnv, jobject, jobject pCallback);
  JNIEXPORT void JNICALL Java_org_adblockplus_android_JSEngine_nativeRelease(JNIEnv *pEnv, jobject pObj, jlong pContext);
  JNIEXPORT jobject JNICALL Java_org_adblockplus_android_JSEngine_nativeExecute(JNIEnv *pEnv, jobject pObj, jstring pScript, jlong pContext);
  JNIEXPORT jobject JNICALL Java_org_adblockplus_android_JSEngine_nativeGet(JNIEnv *pEnv, jobject pObj, jstring pKey, jlong pContext);
  JNIEXPORT jobject JNICALL Java_org_adblockplus_android_JSEngine_nativePut(JNIEnv *pEnv, jobject pObj, jstring pKey, jobject pValue, jlong pContext);
  JNIEXPORT void JNICALL Java_org_adblockplus_android_JSEngine_nativeCallback(JNIEnv *pEnv, jobject pObj, jlong pCallback, jobjectArray pParams, jlong pContext);
  JNIEXPORT jlong JNICALL Java_org_adblockplus_android_JSEngine_nativeRunCallbacks(JNIEnv *pEnv, jobject pObj, jlong pContext);
};

typedef struct __ObjMethod
{
  const char* name;
  v8::InvocationCallback callback;
} ObjMethod;

static ObjMethod methods[] =
{
{ "load", loadImpl },
{ "print", printImpl },
{ "showToast", showToastImpl },
{ "canAutoupdate", canAutoupdateImpl },
{ "setStatus", setStatusImpl },
{ "fileExists", fileExistsImpl },
{ "fileLastModified", fileLastModifiedImpl },
{ "fileRemove", fileRemoveImpl },
{ "fileRename", fileRenameImpl },
{ "fileRead", fileReadImpl },
{ "fileWrite", fileWriteImpl },
{ "setTimeout", setTimeoutImpl },
{ "httpSend", httpSendImpl },
{ NULL, NULL }, };

void reportException(v8::TryCatch* try_catch)
{
  v8::HandleScope handle_scope;
  v8::String::Utf8Value exception(try_catch->Exception());
  v8::Handle < v8::Message > message = try_catch->Message();
  if (message.IsEmpty())
  {
    // Exception context is unknown
    __android_log_print(ANDROID_LOG_ERROR, "JS", "Uncaught exception: %s", *exception);
  }
  else
  {
    v8::String::Utf8Value filename(message->GetScriptResourceName());
    int linenum = message->GetLineNumber();
    __android_log_print(ANDROID_LOG_ERROR, "JS", "Uncaught exception in %s:%i: %s\n", *filename, linenum, *exception);

    v8::String::Utf8Value sourceLine(message->GetSourceLine());
    if (*sourceLine)
    {
      fprintf(stderr, "\n%s\n", *sourceLine);

      int startcol = message->GetStartColumn();
      int endcol = message->GetEndColumn();
      for (int i = 0; i < startcol; i++)
        fprintf(stderr, " ");
      for (int i = startcol; i < endcol; i++)
        fprintf(stderr, "^");
      fprintf(stderr, "\n");
    }
  }
}

const std::string getString(JNIEnv *pEnv, jstring str)
{
  jboolean iscopy;

  const char *s = pEnv->GetStringUTFChars(str, &iscopy);
  jsize len = pEnv->GetStringUTFLength(str);

  const std::string value(s, len);

  pEnv->ReleaseStringUTFChars(str, s);

  return value;
}

jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
  return JNI_VERSION_1_2;
}

void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
}

JNIEXPORT jlong JNICALL Java_org_adblockplus_android_JSEngine_nativeInitialize
  (JNIEnv *pEnv, jobject, jobject pCallback)
{
  jniEnv = pEnv;
  jniCallback = pEnv->NewGlobalRef(pCallback);

  v8::HandleScope handle_scope;

  v8::Handle < v8::ObjectTemplate > system = v8::ObjectTemplate::New();
  for (int i = 0; methods[i].name; i++)
    system->Set(v8::String::New(methods[i].name), v8::FunctionTemplate::New(methods[i].callback));

  v8::Handle < v8::ObjectTemplate > global = v8::ObjectTemplate::New();
  global->Set(v8::String::New("Android"), system);

  return (jlong) *v8::Persistent<v8::Context>::New(v8::Context::New(NULL, global));
}

JNIEXPORT void JNICALL Java_org_adblockplus_android_JSEngine_nativeRelease
  (JNIEnv *pEnv, jobject, jlong pContext)
{
  ClearQueue();
  if (pContext)
  {
    v8::Persistent<v8::Context> context((v8::Context *) pContext);
    context.Dispose();
  }
  jniCallback = NULL;
  jniEnv = NULL;
}

JNIEXPORT jobject JNICALL Java_org_adblockplus_android_JSEngine_nativeExecute
  (JNIEnv *pEnv, jobject pObj, jstring pScript, jlong pContext)
{
  v8::HandleScope handle_scope;

  v8::Persistent<v8::Context> context((v8::Context *) pContext);
  v8::Context::Scope context_scope(context);

  const std::string script = getString(pEnv, pScript);

  v8::Handle<v8::String> source = v8::String::New(script.c_str(), script.size());
  v8::Handle<v8::Script> compiledScript = v8::Script::Compile(source);
  {
    v8::TryCatch try_catch;
    v8::Handle<v8::Value> result = compiledScript->Run();
    if (try_catch.HasCaught())
    {
      reportException(&try_catch);
      return NULL;
    }
    else
    {
      return wrapJSObject(pEnv, result);
    }
  }
}

JNIEXPORT jobject JNICALL Java_org_adblockplus_android_JSEngine_nativeGet
  (JNIEnv *pEnv, jobject pObj, jstring pKey, jlong pContext)
{
  v8::HandleScope handle_scope;

  v8::Persistent<v8::Context> context((v8::Context *) pContext);
  v8::Context::Scope context_scope(context);

  v8::Persistent<v8::Object> obj(v8::Persistent<v8::Object>::New(context->Global()));
  const std::string key = getString(pEnv, pKey);

  {
    v8::TryCatch try_catch;
    v8::Handle<v8::Value> value = obj->Get(v8::String::New(key.c_str(), key.size()));
    if (try_catch.HasCaught())
    {
      reportException(&try_catch);
      return NULL;
    }
    else
    {
      return wrapJSObject(pEnv, value);
    }
  }
}

JNIEXPORT jobject JNICALL Java_org_adblockplus_android_JSEngine_nativePut
  (JNIEnv *pEnv, jobject pObj, jstring pKey, jobject pValue, jlong pContext)
{
  v8::HandleScope handle_scope;

  v8::Persistent<v8::Context> context((v8::Context *) pContext);
  v8::Context::Scope context_scope(context);

  v8::Persistent<v8::Object> obj(v8::Persistent<v8::Object>::New(context->Global()));

  const std::string key = getString(pEnv, pKey);
  v8::Handle<v8::String> name = v8::String::New(key.c_str(), key.size());

//  v8::Handle<v8::Value> value = obj->Get(name);
  obj->Set(name, wrapJavaObject(pEnv, pValue));

  return NULL;
//  return env.HasCaught() ? NULL : env.Wrap(value);
}

JNIEXPORT void JNICALL Java_org_adblockplus_android_JSEngine_nativeCallback
  (JNIEnv *pEnv, jobject pObj, jlong pCallback, jobjectArray pParams, jlong pContext)
{
  v8::HandleScope handle_scope;

  v8::Persistent<v8::Context> context((v8::Context *) pContext);
  v8::Context::Scope context_scope(context);

  v8::Persistent<v8::Function> callback((v8::Function *) pCallback);

  jsize pnum = pEnv->GetArrayLength(pParams);
  v8::Handle<v8::Value> *args = new v8::Handle<v8::Value>[pnum];

  for (int i = 0; i < pnum; i++)
  {
    jobject param = pEnv->GetObjectArrayElement(pParams, i);
    args[i] = wrapJavaObject(pEnv, param);
    pEnv->DeleteLocalRef(param);
  }

  {
    v8::TryCatch try_catch;
    callback->Call(context->Global(), pnum, args);
    callback.Dispose();
    delete [] args;
    if (try_catch.HasCaught())
      reportException(&try_catch);
  }
}

JNIEXPORT jlong JNICALL Java_org_adblockplus_android_JSEngine_nativeRunCallbacks
  (JNIEnv *pEnv, jobject pObj, jlong pContext)
{
  v8::HandleScope handle_scope;

  v8::Persistent<v8::Context> context((v8::Context *) pContext);
  v8::Context::Scope context_scope(context);

  long r = 0;
  while (r == 0)
  {
    v8::TryCatch try_catch;
    r = RunNextCallback(context);
    if (try_catch.HasCaught())
      reportException(&try_catch);
  }
  return (jlong) r;
}
