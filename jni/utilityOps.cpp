/*
 * This file is part of the Adblock Plus,
 * Copyright (C) 2006-2012 Eyeo GmbH
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
#include <android/log.h>
#include "ops.h"
#include "wrap.h"

v8::Handle<v8::Value> loadImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;
  if (args.Length() < 1)
    return v8::ThrowException(v8::String::New("File name expected"));

  v8::String::Utf8Value str(args[0]);
  if (!*str)
    return v8::ThrowException(v8::String::New("File name isn't a string"));

  jstring jstr = jniEnv->NewStringUTF(*str);

  jstring result;
  jclass cls = jniEnv->GetObjectClass(jniCallback);
  jmethodID mid = jniEnv->GetMethodID(cls, "readJSFile", "(Ljava/lang/String;)Ljava/lang/String;");
  if (mid)
    result = (jstring) jniEnv->CallObjectMethod(jniCallback, mid, jstr);
  jniEnv->DeleteLocalRef(jstr);

  /*
  const char* src = jniEnv->GetStringUTFChars(result, 0);
  v8::Handle<v8::String> source = v8::String::New(src, jniEnv->GetStringLength(result));
  jniEnv->ReleaseStringUTFChars(result, src);
  */
  v8::Handle<v8::String> source = v8::Handle<v8::String>::Cast(wrapJavaObject(jniEnv, result));
  v8::Handle<v8::Script> script = v8::Script::Compile(source, args[0]);
  if (!script.IsEmpty())
    script->Run();

  return v8::Undefined();
}

v8::Handle<v8::Value> setStatusImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 2)
    return v8::ThrowException(v8::String::New("Not enough parameters"));

  if (!args[0]->IsString())
    return v8::ThrowException(v8::String::New("Parameter 0 must be a string"));
  if (!args[1]->IsNumber())
    return v8::ThrowException(v8::String::New("Parameter 1 must be a number"));

  v8::String::Utf8Value str(args[0]);
  if (!*str)
    return v8::ThrowException(v8::String::New("Parameter cannot be converted to string"));
  jstring jstr = jniEnv->NewStringUTF(*str);

  jlong jnum = (v8::Handle<v8::Integer>::Cast(args[1]))->Value();

  static jclass cls = jniEnv->GetObjectClass(jniCallback);
  static jmethodID mid = jniEnv->GetMethodID(cls, "setStatus", "(Ljava/lang/String;J)V");
  if (mid)
    jniEnv->CallVoidMethod(jniCallback, mid, jstr, jnum);
  jniEnv->DeleteLocalRef(jstr);

  return v8::Undefined();
}

v8::Handle<v8::Value> canAutoupdateImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  jboolean result;
  jclass cls = jniEnv->GetObjectClass(jniCallback);
  jmethodID mid = jniEnv->GetMethodID(cls, "canAutoupdate", "()Z");
  if (mid)
    result = jniEnv->CallBooleanMethod(jniCallback, mid);
  return wrapJavaObject(jniEnv, NewBoolean(jniEnv, result));
}

v8::Handle<v8::Value> showToastImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;
  if (args.Length() < 1)
    return v8::ThrowException(v8::String::New("String expected"));

  v8::String::Utf8Value str(args[0]);
  if (!*str)
      return v8::ThrowException(v8::String::New("Parameter cannot be converted to string"));
  __android_log_print(ANDROID_LOG_INFO, "ST", *str);
  jstring jstr = jniEnv->NewStringUTF(*str);

  static jclass cls = jniEnv->GetObjectClass(jniCallback);
  static jmethodID mid = jniEnv->GetMethodID(cls, "showToast", "(Ljava/lang/String;)V");
  if (mid)
      jniEnv->CallVoidMethod(jniCallback, mid, jstr);
  jniEnv->DeleteLocalRef(jstr);

    return v8::Undefined();
}

v8::Handle<v8::Value> printImpl(const v8::Arguments& args)
{
  bool first = true;
  std::string msg = "";
  for (int i = 0; i < args.Length(); i++)
  {
    v8::HandleScope handle_scope;
    if (first)
      first = false;
    else
      msg += " ";
    v8::String::Utf8Value str(args[i]);
    if (!*str)
      return v8::ThrowException(v8::String::New("Parameter cannot be converted to string"));
    msg += *str;
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", msg.c_str());
  return v8::Undefined();
}
