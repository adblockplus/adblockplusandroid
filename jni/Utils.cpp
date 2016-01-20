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

#include <string>

#include "Utils.h"

std::string JniJavaToStdString(JNIEnv* env, jstring str)
{
  if (!str)
  {
    return std::string();
  }

  const char* cStr = env->GetStringUTFChars(str, 0);
  std::string ret(cStr);
  env->ReleaseStringUTFChars(str, cStr);

  return ret;
}

jobject NewJniArrayList(JNIEnv* env)
{
  JniLocalReference<jclass> clazz(env, env->FindClass("java/util/ArrayList"));
  jmethodID ctor = env->GetMethodID(*clazz, "<init>", "()V");
  return env->NewObject(*clazz, ctor);
}

void JniAddObjectToList(JNIEnv* env, jobject list, jobject value)
{
  JniLocalReference<jclass> clazz(env, env->GetObjectClass(list));
  jmethodID add = env->GetMethodID(*clazz, "add", "(Ljava/lang/Object;)Z");
  env->CallBooleanMethod(list, add, value);
}

void JniThrowException(JNIEnv* env, const std::string& message)
{
  JniLocalReference<jclass> clazz(env,
      env->FindClass(PKG("AdblockPlusException")));
  env->ThrowNew(*clazz, message.c_str());
}

void JniThrowException(JNIEnv* env, const std::exception& e)
{
  JniThrowException(env, e.what());
}

void JniThrowException(JNIEnv* env)
{
  JniThrowException(env, "Unknown exception from libadblockplus");
}

JNIEnvAcquire::JNIEnvAcquire(JavaVM* javaVM)
  : javaVM(javaVM), jniEnv(0), attachmentStatus(0)
{
  attachmentStatus = javaVM->GetEnv((void **)&jniEnv, ABP_JNI_VERSION);
  if (attachmentStatus == JNI_EDETACHED)
  {
    if (javaVM->AttachCurrentThread(&jniEnv, 0))
    {
      // This one is FATAL, we can't recover from this (because without a JVM we're dead), so
      // throwing a runtime_exception in a ctor can be tolerated here IMHO
      throw std::runtime_error("Failed to get JNI environment");
    }
  }
}

JNIEnvAcquire::~JNIEnvAcquire()
{
  if (attachmentStatus == JNI_EDETACHED)
  {
    javaVM->DetachCurrentThread();
  }
}

template<typename T>
static jobject NewJniObject(JNIEnv* env, const T& value, const char* javaClass)
{
  if (!value.get())
  {
    return 0;
  }

  JniLocalReference<jclass> clazz(
      env,
      env->FindClass(javaClass));
  jmethodID method = env->GetMethodID(*clazz, "<init>", "(J)V");

  return env->NewObject(
      *clazz,
      method,
      JniPtrToLong(new T(value)));
}

jobject NewJniFilter(JNIEnv* env, const AdblockPlus::FilterPtr& filter)
{
  return NewJniObject(env, filter, PKG("Filter"));
}

jobject NewJniSubscription(JNIEnv* env,
    const AdblockPlus::SubscriptionPtr& subscription)
{
  return NewJniObject(env, subscription, PKG("Subscription"));
}

jobject NewJniNotification(JNIEnv* env,
    const AdblockPlus::NotificationPtr& notification)
{
  return NewJniObject(env, notification, PKG("Notification"));
}
