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

static AdblockPlus::Subscription* GetSubscriptionPtr(jlong ptr)
{
  return JniLongToTypePtr<AdblockPlus::SubscriptionPtr>(ptr)->get();
}

static jlong JNICALL JniCtor(JNIEnv* env, jclass clazz, jlong jsValue)
{
  try
  {
    return JniPtrToLong(new AdblockPlus::SubscriptionPtr(new AdblockPlus::Subscription(JniGetJsValuePtr(jsValue))));
  }
  CATCH_THROW_AND_RETURN(env, 0)
}

static jboolean JNICALL JniIsListed(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return GetSubscriptionPtr(ptr)->IsListed() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static void JNICALL JniAddToList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetSubscriptionPtr(ptr)->AddToList();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniRemoveFromList(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetSubscriptionPtr(ptr)->RemoveFromList();
  }
  CATCH_AND_THROW(env)
}

static void JNICALL JniUpdateFilters(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    GetSubscriptionPtr(ptr)->UpdateFilters();
  }
  CATCH_AND_THROW(env)
}

static jboolean JNICALL JniIsUpdating(JNIEnv* env, jclass clazz, jlong ptr)
{
  try
  {
    return GetSubscriptionPtr(ptr)->IsUpdating() ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static jboolean JNICALL JniOperatorEquals(JNIEnv* env, jclass clazz, jlong ptr, jlong otherPtr)
{
  AdblockPlus::Subscription* me = GetSubscriptionPtr(ptr);
  AdblockPlus::Subscription* other = GetSubscriptionPtr(otherPtr);

  try
  {
    return *me == *other ? JNI_TRUE : JNI_FALSE;
  }
  CATCH_THROW_AND_RETURN(env, JNI_FALSE)
}

static JNINativeMethod methods[] =
{
  { (char*)"ctor", (char*)"(J)J", (void*)JniCtor },
  { (char*)"isListed", (char*)"(J)Z", (void*)JniIsListed },
  { (char*)"addToList", (char*)"(J)V", (void*)JniAddToList },
  { (char*)"removeFromList", (char*)"(J)V", (void*)JniRemoveFromList },
  { (char*)"updateFilters", (char*)"(J)V", (void*)JniUpdateFilters },
  { (char*)"isUpdating", (char*)"(J)Z", (void*)JniIsUpdating },
  { (char*)"operatorEquals", (char*)"(JJ)Z", (void*)JniOperatorEquals }
};

extern "C" JNIEXPORT void JNICALL Java_org_adblockplus_libadblockplus_Subscription_registerNatives(JNIEnv *env, jclass clazz)
{
  env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0]));
}
