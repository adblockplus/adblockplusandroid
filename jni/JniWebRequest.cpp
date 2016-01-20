/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-2016 Eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "JniCallbacks.h"

static jlong JNICALL JniCtor(JNIEnv* env, jclass clazz, jobject callbackObject)
{
  try
  {
    return JniPtrToLong(new AdblockPlus::WebRequestPtr(new JniWebRequest(env, callbackObject)));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::WebRequestPtr>(ptr);
}

JniWebRequest::JniWebRequest(JNIEnv* env, jobject callbackObject)
  : JniCallbackBase(env, callbackObject), AdblockPlus::WebRequest(),
    tupleClass(new JniGlobalReference<jclass>(env, env->FindClass(PKG("HeaderEntry")))),
    serverResponseClass(new JniGlobalReference<jclass>(env, env->FindClass(PKG("ServerResponse"))))
{
}

AdblockPlus::ServerResponse JniWebRequest::GET(const std::string& url,
    const AdblockPlus::HeaderList& requestHeaders) const
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env,
          env->GetObjectClass(GetCallbackObject())),
      "httpGET",
      "(Ljava/lang/String;Ljava/util/List;)" TYP("ServerResponse"));

  AdblockPlus::ServerResponse sResponse;
  sResponse.status = AdblockPlus::WebRequest::NS_ERROR_FAILURE;

  if (method)
  {
    JniLocalReference<jobject> arrayList(*env, NewJniArrayList(*env));

    for (AdblockPlus::HeaderList::const_iterator it = requestHeaders.begin(),
        end = requestHeaders.end(); it != end; it++)
    {
      JniLocalReference<jobject> tuple(*env,
          NewTuple(*env, it->first, it->second));
      JniAddObjectToList(*env, *arrayList, *tuple);
    }

    JniLocalReference<jobject> response(*env,
        env->CallObjectMethod(GetCallbackObject(), method,
            *JniLocalReference<jstring>(*env, env->NewStringUTF(url.c_str())),
            *arrayList));

    if (!env->ExceptionCheck())
    {
      sResponse.status = JniGetLongField(*env, serverResponseClass->Get(),
          *response, "status");
      sResponse.responseStatus = JniGetIntField(*env,
          serverResponseClass->Get(), *response, "responseStatus");
      sResponse.responseText = JniGetStringField(*env,
          serverResponseClass->Get(), *response, "response");
      // TODO: transform Headers
    }
  }

  CheckAndLogJavaException(*env);

  return sResponse;
}

jobject JniWebRequest::NewTuple(JNIEnv* env, const std::string& a,
    const std::string& b) const
{
  jmethodID factory = env->GetMethodID(tupleClass->Get(), "<init>",
      "(Ljava/lang/String;Ljava/lang/String;)V");

  JniLocalReference<jstring> strA(env, env->NewStringUTF(a.c_str()));
  JniLocalReference<jstring> strB(env, env->NewStringUTF(b.c_str()));

  return env->NewObject(tupleClass->Get(), factory, *strA, *strB);
}

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(Ljava/lang/Object;)J", (void*)JniCtor },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_WebRequest_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
