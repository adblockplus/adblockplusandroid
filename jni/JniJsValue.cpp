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

static jboolean JNICALL JniIsUndefined(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsUndefined() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsNull(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsNull() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsNumber(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsNumber() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsString(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsString() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsBoolean(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsBool() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsObject(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsObject() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsArray(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsArray() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsFunction(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->IsFunction() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jstring JNICALL JniAsString(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return env->NewStringUTF(JniGetJsValue(ptr)->AsString().c_str());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jlong JNICALL JniAsLong(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return static_cast<jlong>(JniGetJsValue(ptr)->AsInt());
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jboolean JNICALL JniAsBoolean(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return JniGetJsValue(ptr)->AsBool() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jobject JNICALL JniAsList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::JsValueList list = JniGetJsValue(ptr)->AsList();

    return JniJsValueListToArrayList(env, list);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniGetProperty(JNIEnv* env, jclass clazz, jlong ptr, jstring name)
{
  try
  {
    return NewJniJsValue(env, JniGetJsValue(ptr)->GetProperty(JniJavaToStdString(env, name)));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::JsValuePtr>(ptr);
}

jobject NewJniJsValue(JNIEnv* env, const AdblockPlus::JsValuePtr& jsValue, jclass jsValueClass)
{
  if (!jsValue.get())
  {
    return 0;
  }

  jclass clazz = jsValueClass ? jsValueClass : env->FindClass(PKG("JsValue"));
  jmethodID ctor = env->GetMethodID(clazz, "<init>", "(J)V");
  jlong ptr = JniPtrToLong(new AdblockPlus::JsValuePtr(jsValue));
  jobject ret = env->NewObject(clazz, ctor, ptr);

  if (!jsValueClass)
  {
    env->DeleteLocalRef(clazz);
  }

  return ret;
}

AdblockPlus::JsValue* JniGetJsValue(jlong ptr)
{
  return JniLongToTypePtr<AdblockPlus::JsValuePtr>(ptr)->get();
}

AdblockPlus::JsValuePtr& JniGetJsValuePtr(jlong ptr)
{
  return *JniLongToTypePtr<AdblockPlus::JsValuePtr>(ptr);
}

jobject JniJsValueListToArrayList(JNIEnv* env, AdblockPlus::JsValueList& list)
{
  jobject arrayList = NewJniArrayList(env);

  for (AdblockPlus::JsValueList::iterator it = list.begin(), end = list.end(); it != end; ++it)
  {
    JniAddObjectToList(env, arrayList,
        *JniLocalReference<jobject>(env, NewJniJsValue(env, *it)));
  }

  return arrayList;
}

// TODO: List of functions that lack JNI bindings
//std::vector<std::string> GetOwnPropertyNames() const;
//void SetProperty(const std::string& name, const std::string& val);
//void SetProperty(const std::string& name, int64_t val);
//void SetProperty(const std::string& name, bool val);
//void SetProperty(const std::string& name, JsValuePtr value);
//void SetProperty(const std::string& name, const char* val);
//inline void SetProperty(const std::string& name, int val);
//std::string GetClass() const;
//JsValuePtr Call(const JsValueList& params = JsValueList(), AdblockPlus::JsValuePtr thisPtr = AdblockPlus::JsValuePtr()) const;

static JNINativeMethod methods[] =
{
  { (char*)"isUndefined", (char*)"(J)Z", (void*)JniIsUndefined },
  { (char*)"isNull", (char*)"(J)Z", (void*)JniIsNull },
  { (char*)"isNumber", (char*)"(J)Z", (void*)JniIsNumber },
  { (char*)"isString", (char*)"(J)Z", (void*)JniIsString },
  { (char*)"isBoolean", (char*)"(J)Z", (void*)JniIsBoolean },
  { (char*)"isObject", (char*)"(J)Z", (void*)JniIsObject },
  { (char*)"isArray", (char*)"(J)Z", (void*)JniIsArray },
  { (char*)"isFunction", (char*)"(J)Z", (void*)JniIsFunction },
  { (char*)"asString", (char*)"(J)Ljava/lang/String;", (void*)JniAsString },
  { (char*)"asLong", (char*)"(J)J", (void*)JniAsLong },
  { (char*)"asBoolean", (char*)"(J)Z", (void*)JniAsBoolean },
  { (char*)"asList", (char*)"(J)Ljava/util/List;", (void*)JniAsList },
  { (char*)"getProperty", (char*)"(JLjava/lang/String;)" TYP("JsValue"), (void*)JniGetProperty },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_JsValue_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
