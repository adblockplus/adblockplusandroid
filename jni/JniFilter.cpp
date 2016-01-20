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

#include <AdblockPlus.h>
#include "Utils.h"
#include "JniJsValue.h"

static AdblockPlus::Filter* GetFilterPtr(jlong ptr)
{
  return JniLongToTypePtr<AdblockPlus::FilterPtr>(ptr)->get();
}

static jlong JNICALL JniCtor(JNIEnv* env, jclass clazz, jlong jsValue)
{
  try
  {
    return JniPtrToLong(new AdblockPlus::FilterPtr(new AdblockPlus::Filter(JniGetJsValuePtr(jsValue))));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniGetType(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::Filter::Type type;
  try
  {
    type = GetFilterPtr(ptr)->GetType();
  }
  CATCH_THROW_AND_RETURN(env, 0)

  const char* enumName = 0;

  switch (type)
  {
  case AdblockPlus::Filter::TYPE_BLOCKING:
    enumName = "BLOCKING";
    break;
  case AdblockPlus::Filter::TYPE_COMMENT:
    enumName = "COMMENT";
    break;
  case AdblockPlus::Filter::TYPE_ELEMHIDE:
    enumName = "ELEMHIDE";
    break;
  case AdblockPlus::Filter::TYPE_ELEMHIDE_EXCEPTION:
    enumName = "ELEMHIDE_EXCEPTION";
    break;
  case AdblockPlus::Filter::TYPE_EXCEPTION:
    enumName = "EXCEPTION";
    break;
  default:
    enumName = "INVALID";
    break;
  }

  JniLocalReference<jclass> enumClass(env, env->FindClass(PKG("Filter$Type")));
  jfieldID enumField = env->GetStaticFieldID(*enumClass, enumName,
      TYP("Filter$Type"));
  return env->GetStaticObjectField(*enumClass, enumField);
}

static jboolean JNICALL JniIsListed(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return GetFilterPtr(ptr)->IsListed() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static void JNICALL JniAddToList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetFilterPtr(ptr)->AddToList();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveFromList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetFilterPtr(ptr)->RemoveFromList();
  }
  CATCH_AND_THROW(env)
}

static jboolean JNICALL JniOperatorEquals(JNIEnv* env, jclass clazz, jlong ptr, jlong otherPtr)
{
  AdblockPlus::Filter* me = GetFilterPtr(ptr);
  AdblockPlus::Filter* other = GetFilterPtr(otherPtr);

  try
  {
    return *me == *other ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(J)J", (void*)JniCtor },
  { (char*)"getType", (char*)"(J)" TYP("Filter$Type"), (void*)JniGetType },
  { (char*)"isListed", (char*)"(J)Z", (void*)JniIsListed },
  { (char*)"addToList", (char*)"(J)V", (void*)JniAddToList },
  { (char*)"removeFromList", (char*)"(J)V", (void*)JniRemoveFromList },
  { (char*)"operatorEquals", (char*)"(JJ)Z", (void*)JniOperatorEquals }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_Filter_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
