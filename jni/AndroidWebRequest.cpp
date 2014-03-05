/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2013 Eyeo GmbH
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
#include "AndroidWebRequest.h"
#include "Utils.h"
#include "Debug.h"

namespace
{
  std::string ExtractExceptionMessage(JNIEnv* env, jthrowable throwable)
  {
    jclass throwableClass = env->FindClass("java/lang/Throwable");
    jmethodID throwableToStringMethodId = env->GetMethodID(throwableClass, "toString", "()Ljava/lang/String;");
    jstring javaMessage = static_cast<jstring>(env->CallObjectMethod(throwable, throwableToStringMethodId));
    return "Java exception: " + GetString(env, javaMessage);
  }

  class JavaException : public std::exception
  {
  public:
    JavaException(JNIEnv* env)
      : env(env), throwable(env->ExceptionOccurred())
    {
      env->ExceptionClear();
      message = ExtractExceptionMessage(env, throwable);
    }

    virtual ~JavaException() throw()
    {
    }

    const char* what() const throw()
    {
      return message.c_str();
    }

    bool IsInstanceOf(const std::string& className) const
    {
      jclass clazz = env->FindClass(className.c_str());
      if (!clazz)
        return false;
      bool isInstance = env->IsInstanceOf(throwable, clazz);
      env->DeleteLocalRef(clazz);
      return isInstance;
    }

  private:
    JNIEnv* env;
    jthrowable throwable;
    std::string message;
  };

  int64_t ExceptionToStatus(const JavaException& exception)
  {
    if (exception.IsInstanceOf("java/net/MalformedURLException"))
      return AdblockPlus::WebRequest::NS_ERROR_MALFORMED_URI;
    if (exception.IsInstanceOf("java/net/SocketTimeoutException"))
      return AdblockPlus::WebRequest::NS_ERROR_NET_TIMEOUT;
    return AdblockPlus::WebRequest::NS_ERROR_FAILURE;
  }
}

AndroidWebRequest::AndroidWebRequest(JavaVM*& gJvm) : globalJvm(gJvm)
{
  JNIEnv* jniEnv = NULL;
  int stat = globalJvm->GetEnv((void **)&jniEnv, JNI_VERSION_1_6);
  if (stat == JNI_EDETACHED)
  {
    if (globalJvm->AttachCurrentThread(&jniEnv, NULL) != 0)
      throw std::runtime_error("Failed to get JNI environment");
  }

  jUrlClass = reinterpret_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/net/URL")));
  jUrlConstructorID = jniEnv->GetMethodID(jUrlClass, "<init>", "(Ljava/lang/String;)V");
  jUrlOpenConnectionID = jniEnv->GetMethodID(jUrlClass, "openConnection", "()Ljava/net/URLConnection;");
  jConnectionClass = reinterpret_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/net/HttpURLConnection")));
  jConnectionSetMethodID = jniEnv->GetMethodID(jConnectionClass, "setRequestMethod", "(Ljava/lang/String;)V");
  jConnectionSetRequestPropertyID = jniEnv->GetMethodID(jConnectionClass, "setRequestProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
  jConnectionConnectID = jniEnv->GetMethodID(jConnectionClass, "connect", "()V");
  jConnectionGetContentLengthID = jniEnv->GetMethodID(jConnectionClass, "getContentLength", "()I");
  jConnectionGetResponseCodeID = jniEnv->GetMethodID(jConnectionClass, "getResponseCode", "()I");
  jConnectionGetContentEncodingID = jniEnv->GetMethodID(jConnectionClass, "getContentEncoding", "()Ljava/lang/String;");
  jConnectionGetInputStreamID = jniEnv->GetMethodID(jConnectionClass, "getInputStream", "()Ljava/io/InputStream;");
  jInputStreamReaderClass = reinterpret_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/io/InputStreamReader")));
  jInputStreamReaderConstructorID = jniEnv->GetMethodID(jInputStreamReaderClass, "<init>", "(Ljava/io/InputStream;Ljava/lang/String;)V");
  jBufferedReaderClass = reinterpret_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/io/BufferedReader")));
  jBufferedReaderConstructorID = jniEnv->GetMethodID(jBufferedReaderClass, "<init>", "(Ljava/io/Reader;)V");
  jStringBuilderClass = reinterpret_cast<jclass>(jniEnv->NewGlobalRef(jniEnv->FindClass("java/lang/StringBuilder")));
  jStringBuilderConstructorID = jniEnv->GetMethodID(jStringBuilderClass, "<init>", "()V");
  jBufferedReaderReadID = jniEnv->GetMethodID(jBufferedReaderClass, "read", "([CII)I");
  jStringBuilderAppendID = jniEnv->GetMethodID(jStringBuilderClass, "append", "([CII)Ljava/lang/StringBuilder;");
  jStringBuilderToStringID = jniEnv->GetMethodID(jStringBuilderClass, "toString", "()Ljava/lang/String;");
  jBufferedReaderCloseID = jniEnv->GetMethodID(jBufferedReaderClass, "close", "()V");
  jConnectionGetHeaderFieldKeyID = jniEnv->GetMethodID(jConnectionClass, "getHeaderField", "(I)Ljava/lang/String;");
  jConnectionGetHeaderFieldID = jniEnv->GetMethodID(jConnectionClass, "getHeaderFieldKey", "(I)Ljava/lang/String;");

  if (stat == JNI_EDETACHED)
    globalJvm->DetachCurrentThread();
}

AndroidWebRequest::~AndroidWebRequest()
{
  JNIEnv* jniEnv = NULL;
  int stat = globalJvm->GetEnv((void **)&jniEnv, JNI_VERSION_1_6);
  if (stat == JNI_EDETACHED)
  {
    if (globalJvm->AttachCurrentThread(&jniEnv, NULL) != 0)
      throw std::runtime_error("Failed to get JNI environment");
  }

  jniEnv->DeleteGlobalRef(jUrlClass);
  jniEnv->DeleteGlobalRef(jConnectionClass);
  jniEnv->DeleteGlobalRef(jInputStreamReaderClass);
  jniEnv->DeleteGlobalRef(jBufferedReaderClass);
  jniEnv->DeleteGlobalRef(jStringBuilderClass);

  if (stat == JNI_EDETACHED)
    globalJvm->DetachCurrentThread();
}

AdblockPlus::ServerResponse AndroidWebRequest::GET(
  const std::string& url, const AdblockPlus::HeaderList& requestHeaders) const
{
  JNIEnv* jniEnv = NULL;
  int stat = globalJvm->GetEnv((void **)&jniEnv, JNI_VERSION_1_6);
  if (stat == JNI_EDETACHED)
  {
    if (globalJvm->AttachCurrentThread(&jniEnv, NULL) != 0)
      throw std::runtime_error("Failed to get JNI environment");
  }

  AdblockPlus::ServerResponse result;
  try
  {
    // URL jUrl = new URL(url)
    jstring jUrlStr = jniEnv->NewStringUTF(url.c_str());

    jobject jUrl = jniEnv->NewObject(jUrlClass, jUrlConstructorID, jUrlStr);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);
    jniEnv->DeleteLocalRef(jUrlStr);

    // HttpURLConnection connection = (HttpURLConnection) jUrl.openConnection();
    jobject jConnection = jniEnv->CallObjectMethod(jUrl, jUrlOpenConnectionID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    // connection.setRequestMethod("GET");
    jstring jMethod = jniEnv->NewStringUTF("GET");
    jniEnv->CallVoidMethod(jConnection, jConnectionSetMethodID, jMethod);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);
    jniEnv->DeleteLocalRef(jMethod);

    for (int i = 0; i < requestHeaders.size(); i++)
    {
      // connection.setRequestProperty(requestHeaders[i].first, requestHeaders[i].second);
      jstring jHeader = jniEnv->NewStringUTF(requestHeaders[i].first.c_str());
      jstring jValue = jniEnv->NewStringUTF(requestHeaders[i].second.c_str());
      jniEnv->CallVoidMethod(jConnection, jConnectionSetRequestPropertyID, jHeader, jValue);
      if (jniEnv->ExceptionCheck())
        throw JavaException(jniEnv);
      jniEnv->DeleteLocalRef(jHeader);
      jniEnv->DeleteLocalRef(jValue);
    }

    // connection.connect();
    jniEnv->CallVoidMethod(jConnection, jConnectionConnectID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    // int lenghtOfFile = connection.getContentLength();
    jint lenghtOfFile = jniEnv->CallIntMethod(jConnection, jConnectionGetContentLengthID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    D(D_WARN, "Size: %d", lenghtOfFile);

    // result.responseStatus = connection.getResponseCode();
    result.responseStatus = jniEnv->CallIntMethod(jConnection, jConnectionGetResponseCodeID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    /* Read response data */

    // String jEncoding = connection.getContentEncoding();
    jstring jEncoding = (jstring) jniEnv->CallObjectMethod(jConnection, jConnectionGetContentEncodingID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    if (jEncoding == NULL)
      jEncoding = jniEnv->NewStringUTF("utf-8");

    // InputStream jis = connection.getInputStream();
    jobject jis = jniEnv->CallObjectMethod(jConnection, jConnectionGetInputStreamID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    // InputStreamReader jisr = new InputStreamReader(jis, jEncoding);
    jobject jisr = jniEnv->NewObject(jInputStreamReaderClass, jInputStreamReaderConstructorID, jis, jEncoding);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    jniEnv->DeleteLocalRef(jEncoding);

    // BufferedReader jin = new BufferedReader(jisr);
    jobject jin = jniEnv->NewObject(jBufferedReaderClass, jBufferedReaderConstructorID, jisr);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    // char[] jBuffer = new char[0x10000];
    jcharArray jBuffer = jniEnv->NewCharArray(0x10000);

    // StringBuilder jout = new StringBuilder();
    jobject jout = jniEnv->NewObject(jStringBuilderClass, jStringBuilderConstructorID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    jlong total = 0;
    jint read;

    jint jBufferLength = (jint) jniEnv->GetArrayLength(jBuffer);

    do
    {
      // read = jin.read(buffer, 0, buffer.length);
      read = jniEnv->CallIntMethod(jin, jBufferedReaderReadID, jBuffer, 0, jBufferLength);
      if (jniEnv->ExceptionCheck())
        throw JavaException(jniEnv);

      if (read > 0)
      {
        // jout.append(buffer, 0, read);
        jniEnv->CallObjectMethod(jout, jStringBuilderAppendID, jBuffer, 0, jBufferLength);
        if (jniEnv->ExceptionCheck())
          throw JavaException(jniEnv);

        total += read;
      }
    }
    while (read >= 0);

    // String jData = out.toString();
    jstring jData = (jstring) jniEnv->CallObjectMethod(jout, jStringBuilderToStringID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    result.responseText = GetString(jniEnv, jData);

    // jin.close();
    jniEnv->CallVoidMethod(jin, jBufferedReaderCloseID);
    if (jniEnv->ExceptionCheck())
      throw JavaException(jniEnv);

    jint i = 0;
    while (true)
    {
      // String jHeaderName = connection.getHeaderFieldKey(i)
      jstring jHeaderName = (jstring) jniEnv->CallObjectMethod(jConnection, jConnectionGetHeaderFieldKeyID, i);
      if (jniEnv->ExceptionCheck())
        throw JavaException(jniEnv);

      // String jHeaderValue = connection.getHeaderField(i)
      jstring jHeaderValue = (jstring) jniEnv->CallObjectMethod(jConnection, jConnectionGetHeaderFieldID, i);
      if (jniEnv->ExceptionCheck())
        throw JavaException(jniEnv);

      if (!jHeaderValue)
        break;

      std::string headerName = GetString(jniEnv, jHeaderName);
      std::string headerValue = GetString(jniEnv, jHeaderValue);

      headerName = TrimString(headerName);
      headerValue = TrimString(headerValue);

      std::transform(headerName.begin(), headerName.end(), headerName.begin(), ::tolower);

      result.responseHeaders.push_back(std::pair<std::string, std::string>(headerName, headerValue));

      i++;
    }
    D(D_WARN, "Finished downloading");

    result.status = NS_OK;
  }
  catch(JavaException& e)
  {
    result.responseStatus = 0;
    result.status = ExceptionToStatus(e);
    D(D_ERROR, "%s", e.what());
  }
  catch (const std::exception& e)
  {
    D(D_ERROR, "Exception: %s", e.what());
    result.status = AdblockPlus::DefaultWebRequest::NS_ERROR_FAILURE;
  }
  catch (...)
  {
    D(D_ERROR, "Unknown exception");
    result.status = AdblockPlus::DefaultWebRequest::NS_ERROR_FAILURE;
  }

  if (stat == JNI_EDETACHED)
    globalJvm->DetachCurrentThread();

  return result;
}
