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
#include "JniCallbacks.h"

static void TransformAppInfo(JNIEnv* env, jobject jAppInfo, AdblockPlus::AppInfo& appInfo)
{
  jclass clazz = env->GetObjectClass(jAppInfo);

  appInfo.application = JniGetStringField(env, clazz, jAppInfo, "application");
  appInfo.applicationVersion = JniGetStringField(env, clazz, jAppInfo, "applicationVersion");
  appInfo.locale = JniGetStringField(env, clazz, jAppInfo, "locale");
  appInfo.name = JniGetStringField(env, clazz, jAppInfo, "name");
  appInfo.version = JniGetStringField(env, clazz, jAppInfo, "version");

  appInfo.developmentBuild = JniGetBooleanField(env, clazz, jAppInfo, "developmentBuild");
}

static jlong JNICALL JniCtor(JNIEnv* env, jclass clazz, jobject jAppInfo)
{
  AdblockPlus::AppInfo appInfo;

  TransformAppInfo(env, jAppInfo, appInfo);

  try
  {
    return JniPtrToLong(new AdblockPlus::JsEnginePtr(AdblockPlus::JsEngine::New(appInfo)));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);
}

static void JNICALL JniSetEventCallback(JNIEnv* env, jclass clazz, jlong ptr, jstring jEventName, jlong jCallbackPtr)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  JniEventCallback* callback = JniLongToTypePtr<JniEventCallback>(jCallbackPtr);
  std::string eventName = JniJavaToStdString(env, jEventName);
  AdblockPlus::JsEngine::EventCallback eCallback = std::bind(&JniEventCallback::Callback, callback, std::placeholders::_1);

  try
  {
    engine->SetEventCallback(eventName, eCallback);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveEventCallback(JNIEnv* env, jclass clazz, jlong ptr, jstring jEventName)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  std::string eventName = JniJavaToStdString(env, jEventName);

  try
  {
    engine->RemoveEventCallback(eventName);
  }
  CATCH_AND_THROW(env)
}

static jobject JNICALL JniEvaluate(JNIEnv* env, jclass clazz, jlong ptr, jstring jSource, jstring jFilename)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  std::string source = JniJavaToStdString(env, jSource);
  std::string filename = JniJavaToStdString(env, jFilename);

  try
  {
    AdblockPlus::JsValuePtr jsValue = engine->Evaluate(source, filename);
    return NewJniJsValue(env, jsValue);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniTriggerEvent(JNIEnv* env, jclass clazz, jlong ptr, jstring jEventName, jarray jJsPtrs)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);
  std::string eventName = JniJavaToStdString(env, jEventName);
  AdblockPlus::JsValueList args;

  if (jJsPtrs)
  {
    jlong* ptrs = (jlong*)env->GetPrimitiveArrayCritical(jJsPtrs, 0);

    jsize length = env->GetArrayLength(jJsPtrs);

    for (jsize i = 0; i < length; i++)
    {
      args.push_back(JniGetJsValuePtr(ptrs[i]));
    }

    env->ReleasePrimitiveArrayCritical(jJsPtrs, ptrs, JNI_ABORT);
  }

  try
  {
    engine->TriggerEvent(eventName, args);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetDefaultFileSystem(JNIEnv* env, jclass clazz, jlong ptr, jstring jBasePath)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::FileSystemPtr fileSystem(new AdblockPlus::DefaultFileSystem());

    std::string basePath = JniJavaToStdString(env, jBasePath);
    reinterpret_cast<AdblockPlus::DefaultFileSystem*>(fileSystem.get())->SetBasePath(basePath);

    engine->SetFileSystem(fileSystem);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetDefaultWebRequest(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::WebRequestPtr webRequest(new AdblockPlus::DefaultWebRequest());

    engine->SetWebRequest(webRequest);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetDefaultLogSystem(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::LogSystemPtr logSystem(new AdblockPlus::DefaultLogSystem());

    engine->SetLogSystem(logSystem);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetLogSystem(JNIEnv* env, jclass clazz, jlong ptr, jlong logSystemPtr)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::LogSystemPtr logSystem(JniLongToTypePtr<JniLogSystemCallback>(logSystemPtr));

    engine->SetLogSystem(logSystem);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetWebRequest(JNIEnv* env, jclass clazz, jlong ptr, jlong webRequestPtr)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::WebRequestPtr& webRequest = *JniLongToTypePtr<AdblockPlus::WebRequestPtr>(webRequestPtr);

    engine->SetWebRequest(webRequest);
  }
  CATCH_AND_THROW(env)
}

static jobject JNICALL JniNewLongValue(JNIEnv* env, jclass clazz, jlong ptr, jlong value)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::JsValuePtr jsValue = engine->NewValue(static_cast<int64_t>(value));
    return NewJniJsValue(env, jsValue);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniNewBooleanValue(JNIEnv* env, jclass clazz, jlong ptr, jboolean value)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    AdblockPlus::JsValuePtr jsValue = engine->NewValue(value == JNI_TRUE ? true : false);
    return NewJniJsValue(env, jsValue);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniNewStringValue(JNIEnv* env, jclass clazz, jlong ptr, jstring value)
{
  AdblockPlus::JsEnginePtr& engine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(ptr);

  try
  {
    std::string strValue = JniJavaToStdString(env, value);
    AdblockPlus::JsValuePtr jsValue = engine->NewValue(strValue);
    return NewJniJsValue(env, jsValue);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

// TODO: List of functions that lack JNI bindings
//JsValuePtr NewObject();
//JsValuePtr NewCallback(v8::InvocationCallback callback);
//static JsEnginePtr FromArguments(const v8::Arguments& arguments);
//JsValueList ConvertArguments(const v8::Arguments& arguments);

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(" TYP("AppInfo") ")J", (void*)JniCtor },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor },

  { (char*)"setEventCallback", (char*)"(JLjava/lang/String;J)V", (void*)JniSetEventCallback },
  { (char*)"removeEventCallback", (char*)"(JLjava/lang/String;)V", (void*)JniRemoveEventCallback },
  { (char*)"triggerEvent", (char*)"(JLjava/lang/String;[J)V", (void*)JniTriggerEvent },

  { (char*)"evaluate", (char*)"(JLjava/lang/String;Ljava/lang/String;)" TYP("JsValue"), (void*)JniEvaluate },

  { (char*)"setDefaultFileSystem", (char*)"(JLjava/lang/String;)V", (void*)JniSetDefaultFileSystem },
  { (char*)"setLogSystem", (char*)"(JJ)V", (void*)JniSetLogSystem },
  { (char*)"setDefaultLogSystem", (char*)"(J)V", (void*)JniSetDefaultLogSystem },
  { (char*)"setWebRequest", (char*)"(JJ)V", (void*)JniSetWebRequest },
  { (char*)"setDefaultWebRequest", (char*)"(J)V", (void*)JniSetDefaultWebRequest },

  { (char*)"newValue", (char*)"(JJ)" TYP("JsValue"), (void*)JniNewLongValue },
  { (char*)"newValue", (char*)"(JZ)" TYP("JsValue"), (void*)JniNewBooleanValue },
  { (char*)"newValue", (char*)"(JLjava/lang/String;)" TYP("JsValue"), (void*)JniNewStringValue }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_JsEngine_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
