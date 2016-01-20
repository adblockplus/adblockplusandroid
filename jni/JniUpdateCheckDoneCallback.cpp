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
    return JniPtrToLong(new JniUpdateCheckDoneCallback(env, callbackObject));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<JniUpdateCheckDoneCallback>(ptr);
}

JniUpdateCheckDoneCallback::JniUpdateCheckDoneCallback(
  JNIEnv* env, jobject callbackObject)
  : JniCallbackBase(env, callbackObject)
{
}

void JniUpdateCheckDoneCallback::Callback(const std::string& arg)
{
  JNIEnvAcquire env(GetJavaVM());

  jmethodID method = env->GetMethodID(
      *JniLocalReference<jclass>(*env,
          env->GetObjectClass(GetCallbackObject())),
      "updateCheckDoneCallback",
      "(Ljava/lang/String;)V");

  if (method)
  {
    JniLocalReference<jstring> jArg(*env, env->NewStringUTF(arg.c_str()));
    env->CallVoidMethod(GetCallbackObject(), method, *jArg);
  }

  CheckAndLogJavaException(*env);
}

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(Ljava/lang/Object;)J", (void*)JniCtor },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor }
};

extern "C" JNIEXPORT void JNICALL
Java_org_adblockplus_libadblockplus_UpdateCheckDoneCallback_registerNatives(
  JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
