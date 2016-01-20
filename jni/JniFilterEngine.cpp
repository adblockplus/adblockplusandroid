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

static jobject SubscriptionsToArrayList(JNIEnv* env, std::vector<AdblockPlus::SubscriptionPtr>& subscriptions)
{
  jobject list = NewJniArrayList(env);

  for (std::vector<AdblockPlus::SubscriptionPtr>::iterator it = subscriptions.begin(), end = subscriptions.end(); it != end; it++)
  {
    JniAddObjectToList(env, list, NewJniSubscription(env, *it));
  }

  return list;
}

static AdblockPlus::FilterEngine::ContentType ConvertContentType(JNIEnv *env,
    jobject jContentType)
{
  JniLocalReference<jclass> contentTypeClass(env,
      env->GetObjectClass(jContentType));
  jmethodID nameMethod = env->GetMethodID(*contentTypeClass, "name",
      "()Ljava/lang/String;");
  JniLocalReference<jstring> jValue(env,
      (jstring) env->CallObjectMethod(jContentType, nameMethod));
  const std::string value = JniJavaToStdString(env, *jValue);
  return AdblockPlus::FilterEngine::StringToContentType(value);
}

static jlong JNICALL JniCtor(JNIEnv* env, jclass clazz, jlong enginePtr)
{
  try
  {
    AdblockPlus::JsEnginePtr& jsEngine = *JniLongToTypePtr<AdblockPlus::JsEnginePtr>(enginePtr);
    return JniPtrToLong(new AdblockPlus::FilterEngine(jsEngine));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniDtor(JNIEnv* env, jclass clazz, jlong ptr)
{
  delete JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
}

static jboolean JNICALL JniIsFirstRun(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

    return engine->IsFirstRun() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE);
}

static jobject JNICALL JniGetFilter(JNIEnv* env, jclass clazz, jlong ptr, jstring jText)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  std::string text = JniJavaToStdString(env, jText);

  try
  {
    AdblockPlus::FilterPtr filter = engine->GetFilter(text);

    return NewJniFilter(env, filter);
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static jobject JNICALL JniGetListedFilters(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  try
  {
    std::vector<AdblockPlus::FilterPtr> filters = engine->GetListedFilters();

    jobject list = NewJniArrayList(env);

    for (std::vector<AdblockPlus::FilterPtr>::iterator it = filters.begin(), end = filters.end(); it != end; it++)
    {
      JniAddObjectToList(env, list, *JniLocalReference<jobject>(env, NewJniFilter(env, *it)));
    }

    return list;
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static jobject JNICALL JniGetSubscription(JNIEnv* env, jclass clazz, jlong ptr, jstring jUrl)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  std::string url = JniJavaToStdString(env, jUrl);

  try
  {
    AdblockPlus::SubscriptionPtr subscription = engine->GetSubscription(url);

    return NewJniSubscription(env, subscription);
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static void JNICALL JniShowNextNotification(JNIEnv* env, jclass clazz, jlong ptr, jstring jUrl)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  std::string url = JniJavaToStdString(env, jUrl);

  try
  {
    engine->ShowNextNotification(url);
  }
  CATCH_AND_THROW(env);
}

static void JNICALL JniSetShowNotificationCallback(JNIEnv* env, jclass clazz,
                                                  jlong ptr, jlong callbackPtr)
{
  AdblockPlus::FilterEngine* const engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  JniShowNotificationCallback* const callback =
      JniLongToTypePtr<JniShowNotificationCallback>(callbackPtr);
  AdblockPlus::FilterEngine::ShowNotificationCallback showNotificationCallback =
      std::bind(&JniShowNotificationCallback::Callback, callback,
                     std::placeholders::_1);
  try
  {
    engine->SetShowNotificationCallback(showNotificationCallback);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveShowNotificationCallback(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  try
  {
    engine->RemoveShowNotificationCallback();
  }
  CATCH_AND_THROW(env);
}

static jobject JNICALL JniGetListedSubscriptions(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  try
  {
    std::vector<AdblockPlus::SubscriptionPtr> subscriptions = engine->GetListedSubscriptions();

    return SubscriptionsToArrayList(env, subscriptions);
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static jobject JNICALL JniFetchAvailableSubscriptions(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  try
  {
    std::vector<AdblockPlus::SubscriptionPtr> subscriptions = engine->FetchAvailableSubscriptions();

    return SubscriptionsToArrayList(env, subscriptions);
  }
  CATCH_THROW_AND_RETURN(env, 0);
}

static void JNICALL JniRemoveUpdateAvailableCallback(JNIEnv* env, jclass clazz,
                                                     jlong ptr)
{
  AdblockPlus::FilterEngine* const engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  try
  {
    engine->RemoveUpdateAvailableCallback();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetUpdateAvailableCallback(JNIEnv* env, jclass clazz,
                                                  jlong ptr, jlong callbackPtr)
{
  AdblockPlus::FilterEngine* const engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  JniUpdateAvailableCallback* const callback =
      JniLongToTypePtr<JniUpdateAvailableCallback>(callbackPtr);
  AdblockPlus::FilterEngine::UpdateAvailableCallback updateAvailableCallback =
      std::bind(&JniUpdateAvailableCallback::Callback, callback,
                     std::placeholders::_1);
  try
  {
    engine->SetUpdateAvailableCallback(updateAvailableCallback);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveFilterChangeCallback(JNIEnv* env, jclass clazz, jlong ptr)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  try
  {
    engine->RemoveFilterChangeCallback();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniSetFilterChangeCallback(JNIEnv* env, jclass clazz,
    jlong ptr, jlong filterPtr)
{
  AdblockPlus::FilterEngine* engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  JniFilterChangeCallback* callback = JniLongToTypePtr<JniFilterChangeCallback>(
      filterPtr);

  AdblockPlus::FilterEngine::FilterChangeCallback filterCallback =
      std::bind(&JniFilterChangeCallback::Callback, callback,
          std::placeholders::_1, std::placeholders::_2);

  try
  {
    engine->SetFilterChangeCallback(filterCallback);
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniForceUpdateCheck(JNIEnv* env, jclass clazz, jlong ptr, jlong updaterPtr)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);
  JniUpdateCheckDoneCallback* callback =
      JniLongToTypePtr<JniUpdateCheckDoneCallback>(updaterPtr);

  AdblockPlus::FilterEngine::UpdateCheckDoneCallback
      updateCheckDoneCallback = 0;

  if (updaterPtr)
  {
    updateCheckDoneCallback =
        std::bind(&JniUpdateCheckDoneCallback::Callback, callback,
                       std::placeholders::_1);
  }

  try
  {
    engine->ForceUpdateCheck(updateCheckDoneCallback);
  }
  CATCH_AND_THROW(env)
}

static jobject JNICALL JniGetElementHidingSelectors(JNIEnv* env, jclass clazz,
    jlong ptr, jstring jDomain)
{
  AdblockPlus::FilterEngine* engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  std::string domain = JniJavaToStdString(env, jDomain);

  try
  {
    std::vector<std::string> selectors = engine->GetElementHidingSelectors(
        domain);

    jobject list = NewJniArrayList(env);

    for (std::vector<std::string>::iterator it = selectors.begin(), end =
        selectors.end(); it != end; it++)
    {
      JniAddObjectToList(env, list,
          *JniLocalReference<jstring>(env, env->NewStringUTF(it->c_str())));
    }

    return list;
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jobject JNICALL JniMatches(JNIEnv* env, jclass clazz, jlong ptr, jstring jUrl, jobject jContentType, jstring jDocumentUrl)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  AdblockPlus::FilterEngine::ContentType contentType =
      ConvertContentType(env, jContentType);
  std::string documentUrl = JniJavaToStdString(env, jDocumentUrl);

  try
  {
    AdblockPlus::FilterPtr filter = engine->Matches(url, contentType, documentUrl);

    return NewJniFilter(env, filter);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JavaStringArrayToStringVector(JNIEnv* env, jobjectArray jArray,
    std::vector<std::string>& out)
{
  if (jArray)
  {
    jsize len = env->GetArrayLength(jArray);

    for (jsize i = 0; i < len; i++)
    {
      out.push_back(
          JniJavaToStdString(env,
              *JniLocalReference<jstring>(env,
                  static_cast<jstring>(
                      env->GetObjectArrayElement(jArray, i)))));
    }
  }
}

static jobject JNICALL JniMatchesMany(JNIEnv* env, jclass clazz, jlong ptr,
    jstring jUrl, jobject jContentType, jobjectArray jDocumentUrls)
{
  AdblockPlus::FilterEngine* engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  AdblockPlus::FilterEngine::ContentType contentType =
      ConvertContentType(env, jContentType);

  std::vector<std::string> documentUrls;
  JavaStringArrayToStringVector(env, jDocumentUrls, documentUrls);

  try
  {
    AdblockPlus::FilterPtr filter = engine->Matches(url, contentType,
        documentUrls);

    return NewJniFilter(env, filter);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jboolean JNICALL JniIsDocumentWhitelisted(JNIEnv* env, jclass clazz, jlong ptr,
    jstring jUrl, jobjectArray jDocumentUrls)
{
  AdblockPlus::FilterEngine* engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  std::vector<std::string> documentUrls;
  JavaStringArrayToStringVector(env, jDocumentUrls, documentUrls);
  try
  {
    return engine->IsDocumentWhitelisted(url, documentUrls) ?
        JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniIsElemhideWhitelisted(JNIEnv* env, jclass clazz, jlong ptr,
    jstring jUrl, jobjectArray jDocumentUrls)
{
  AdblockPlus::FilterEngine* engine =
      JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  std::string url = JniJavaToStdString(env, jUrl);
  std::vector<std::string> documentUrls;
  JavaStringArrayToStringVector(env, jDocumentUrls, documentUrls);
  try
  {
    return engine->IsElemhideWhitelisted(url, documentUrls) ?
        JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jobject JNICALL JniGetPref(JNIEnv* env, jclass clazz, jlong ptr, jstring jPref)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr < AdblockPlus::FilterEngine > (ptr);

  std::string pref = JniJavaToStdString(env, jPref);

  try
  {
    AdblockPlus::JsValuePtr value = engine->GetPref(pref);

    return NewJniJsValue(env, value);
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static void JNICALL JniSetPref(JNIEnv* env, jclass clazz, jlong ptr, jstring jPref, jlong jsValue)
{
  AdblockPlus::FilterEngine* engine = JniLongToTypePtr<AdblockPlus::FilterEngine>(ptr);

  std::string pref = JniJavaToStdString(env, jPref);
  AdblockPlus::JsValuePtr value = JniGetJsValuePtr(jsValue);

  try
  {
    engine->SetPref(pref, value);
  }
  CATCH_AND_THROW(env)
}

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(J)J", (void*)JniCtor },
  { (char*)"isFirstRun", (char*)"(J)Z", (void*)JniIsFirstRun },
  { (char*)"getFilter", (char*)"(JLjava/lang/String;)" TYP("Filter"), (void*)JniGetFilter },
  { (char*)"getListedFilters", (char*)"(J)Ljava/util/List;", (void*)JniGetListedFilters },
  { (char*)"getSubscription", (char*)"(JLjava/lang/String;)" TYP("Subscription"), (void*)JniGetSubscription },
  { (char*)"showNextNotification", (char*)"(JLjava/lang/String;)V", (void*)JniShowNextNotification },
  { (char*)"setShowNotificationCallback", (char*)"(JJ)V", (void*)JniSetShowNotificationCallback },
  { (char*)"removeShowNotificationCallback", (char*)"(J)V", (void*)JniRemoveShowNotificationCallback },
  { (char*)"getListedSubscriptions", (char*)"(J)Ljava/util/List;", (void*)JniGetListedSubscriptions },
  { (char*)"fetchAvailableSubscriptions", (char*)"(J)Ljava/util/List;", (void*)JniFetchAvailableSubscriptions },
  { (char*)"setUpdateAvailableCallback", (char*)"(JJ)V", (void*)JniSetUpdateAvailableCallback },
  { (char*)"removeUpdateAvailableCallback", (char*)"(J)V", (void*)JniRemoveUpdateAvailableCallback },
  { (char*)"setFilterChangeCallback", (char*)"(JJ)V", (void*)JniSetFilterChangeCallback },
  { (char*)"removeFilterChangeCallback", (char*)"(J)V", (void*)JniRemoveFilterChangeCallback },
  { (char*)"forceUpdateCheck", (char*)"(JJ)V", (void*)JniForceUpdateCheck },
  { (char*)"getElementHidingSelectors", (char*)"(JLjava/lang/String;)Ljava/util/List;", (void*)JniGetElementHidingSelectors },
  { (char*)"matches", (char*)"(JLjava/lang/String;" TYP("FilterEngine$ContentType") "Ljava/lang/String;)" TYP("Filter"), (void*)JniMatches },
  { (char*)"matches", (char*)"(JLjava/lang/String;" TYP("FilterEngine$ContentType") "[Ljava/lang/String;)" TYP("Filter"), (void*)JniMatchesMany },
  { (char*)"isDocumentWhitelisted", (char*)"(JLjava/lang/String;[Ljava/lang/String;)Z", (void*)JniIsDocumentWhitelisted },
  { (char*)"isElemhideWhitelisted", (char*)"(JLjava/lang/String;[Ljava/lang/String;)Z", (void*)JniIsElemhideWhitelisted },
  { (char*)"getPref", (char*)"(JLjava/lang/String;)" TYP("JsValue"), (void*)JniGetPref },
  { (char*)"setPref", (char*)"(JLjava/lang/String;J)V", (void*)JniSetPref },
  { (char*)"dtor", (char*)"(J)V", (void*)JniDtor }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_FilterEngine_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
