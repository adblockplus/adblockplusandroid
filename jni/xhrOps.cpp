#include <string>
#include "ops.h"

v8::Handle<v8::Value> httpSendImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 5)
    return v8::ThrowException(v8::String::New("Not enough parameters"));

  if (!args[0]->IsString())
    return v8::ThrowException(v8::String::New("Parameter 0 must be a string"));
  if (!args[1]->IsString())
    return v8::ThrowException(v8::String::New("Parameter 1 must be a string"));
  if (!args[2]->IsObject())
    return v8::ThrowException(v8::String::New("Parameter 2 must be a hash object"));
  if (!args[3]->IsBoolean())
    return v8::ThrowException(v8::String::New("Parameter 3 must be a boolean"));
  if (!args[4]->IsFunction())
    return v8::ThrowException(v8::String::New("Parameter 4 must be a function"));

  v8::String::Utf8Value method(args[0]);
  if (!*method)
    return v8::ThrowException(v8::String::New("Method must be set"));
  v8::String::Utf8Value url(args[1]);
  if (!*url)
    return v8::ThrowException(v8::String::New("Url must be set"));


  v8::Handle<v8::Object> headerObj = v8::Handle<v8::Object>::Cast(args[2]);
  v8::Handle<v8::Array> headers = headerObj->GetPropertyNames();

//  v8::Handle<v8::BooleanObject> async = v8::Handle<v8::BooleanObject>::Cast(args[3]);
  jboolean jasync = args[3]->IsTrue() ? JNI_TRUE : JNI_FALSE;

  v8::Persistent<v8::Function> callback = v8::Persistent<v8::Function>::New(v8::Handle<v8::Function>::Cast(args[4]));
  if (!*callback)
    return v8::ThrowException(v8::String::New("Callback must be set"));

  jstring jmethod = jniEnv->NewStringUTF(*method);
  jstring jurl = jniEnv->NewStringUTF(*url);

    static jclass stringClass = jniEnv->FindClass("java/lang/String");
    static jclass stringArrayClass = jniEnv->GetObjectClass(stringClass);

    jobjectArray jheaders = jniEnv->NewObjectArray((jsize) headers->Length(), stringArrayClass, NULL);

    for (unsigned int i = 0; i < headers->Length(); i++)
    {
      v8::String::Utf8Value name(headers->Get(v8::Integer::New(i)));
    v8::String::Utf8Value value(headerObj->Get(headers->Get(v8::Integer::New(i))));
        jobjectArray stringArray = jniEnv->NewObjectArray(2, stringClass, NULL);
         jniEnv->SetObjectArrayElement(stringArray, 0, jniEnv->NewStringUTF(*name));
         jniEnv->SetObjectArrayElement(stringArray, 1, jniEnv->NewStringUTF(*value));
        jniEnv->SetObjectArrayElement(jheaders, (jsize) i, stringArray);
        jniEnv->DeleteLocalRef(stringArray);
    }

    jlong jcallback = (jlong) *callback;

  static jclass cls = jniEnv->GetObjectClass(jniCallback);
  static jmethodID mid = jniEnv->GetMethodID(cls, "httpSend", "(Ljava/lang/String;Ljava/lang/String;[[Ljava/lang/String;ZJ)V");
  if (mid)
      jniEnv->CallVoidMethod(jniCallback, mid, jmethod, jurl, jheaders, jasync, jcallback);

  jniEnv->DeleteLocalRef(jmethod);
  jniEnv->DeleteLocalRef(jurl);
  jniEnv->DeleteLocalRef(jheaders);

    return v8::Undefined();
}
