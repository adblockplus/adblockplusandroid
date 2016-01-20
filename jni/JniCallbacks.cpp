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

JniCallbackBase::JniCallbackBase(JNIEnv* env, jobject callbackObject)
  : callbackObject(new JniGlobalReference<jobject>(env, callbackObject)),
    exceptionLoggerClass(new JniGlobalReference<jclass>(env, env->FindClass(PKG("JniExceptionHandler"))))
{
  env->GetJavaVM(&javaVM);
}

JniCallbackBase::~JniCallbackBase()
{

}

void JniCallbackBase::LogException(JNIEnv* env, jthrowable throwable) const
{
  jmethodID logMethod = env->GetStaticMethodID(exceptionLoggerClass->Get(), "logException", "(Ljava/lang/Throwable;)V");
  if (logMethod)
  {
    env->CallStaticVoidMethod(exceptionLoggerClass->Get(), logMethod, throwable);
  }
}

void JniCallbackBase::CheckAndLogJavaException(JNIEnv* env) const
{
  if (env->ExceptionCheck())
  {
    JniLocalReference<jthrowable> throwable(env, env->ExceptionOccurred());
    env->ExceptionClear();
    LogException(env, *throwable);
  }
}
