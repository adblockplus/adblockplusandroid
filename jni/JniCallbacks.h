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

#ifndef JNICALLBACKS_H
#define JNICALLBACKS_H

#include <AdblockPlus.h>
#include "Utils.h"
#include "JniJsValue.h"

class JniCallbackBase
{
public:
  JniCallbackBase(JNIEnv* env, jobject callbackObject);
  virtual ~JniCallbackBase();
  void LogException(JNIEnv* env, jthrowable throwable) const;
  void CheckAndLogJavaException(JNIEnv* env) const;

  JavaVM* GetJavaVM() const
  {
    return javaVM;
  }

  jobject GetCallbackObject() const
  {
    return callbackObject->Get();
  }

private:
  JavaVM* javaVM;
  const JniGlobalReference<jobject>::Ptr callbackObject;
  const JniGlobalReference<jclass>::Ptr exceptionLoggerClass;
};

class JniEventCallback : public JniCallbackBase
{
public:
  JniEventCallback(JNIEnv* env, jobject callbackObject);
  void Callback(AdblockPlus::JsValueList& params);
};

class JniUpdateAvailableCallback : public JniCallbackBase
{
public:
  JniUpdateAvailableCallback(JNIEnv* env, jobject callbackObject);
  void Callback(const std::string& arg);
};

class JniUpdateCheckDoneCallback : public JniCallbackBase
{
public:
  JniUpdateCheckDoneCallback(JNIEnv* env, jobject callbackObject);
  void Callback(const std::string& arg);
};

class JniFilterChangeCallback : public JniCallbackBase
{
public:
  JniFilterChangeCallback(JNIEnv* env, jobject callbackObject);
  void Callback(const std::string& arg, const AdblockPlus::JsValuePtr jsValue);

private:
  const JniGlobalReference<jclass>::Ptr jsValueClass;
};

class JniLogSystemCallback : public JniCallbackBase, public AdblockPlus::LogSystem
{
public:
  JniLogSystemCallback(JNIEnv* env, jobject callbackObject);
  void operator()(AdblockPlus::LogSystem::LogLevel logLevel, const std::string& message, const std::string& source);

private:
  const JniGlobalReference<jclass>::Ptr logLevelClass;
};

class JniShowNotificationCallback : public JniCallbackBase
{
public:
  JniShowNotificationCallback(JNIEnv* env, jobject callbackObject);
  void Callback(const AdblockPlus::NotificationPtr&);

private:
  const JniGlobalReference<jclass>::Ptr notificationClass;
};

class JniWebRequest : public JniCallbackBase, public AdblockPlus::WebRequest
{
public:
  JniWebRequest(JNIEnv* env, jobject callbackObject);
  AdblockPlus::ServerResponse GET(const std::string& url, const AdblockPlus::HeaderList& requestHeaders) const;

private:
  jobject NewTuple(JNIEnv* env, const std::string& a, const std::string& b) const;

  const JniGlobalReference<jclass>::Ptr tupleClass;
  const JniGlobalReference<jclass>::Ptr serverResponseClass;
};

#endif /* JNICALLBACKS_H */
