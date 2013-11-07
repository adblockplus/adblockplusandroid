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

#ifndef ADBLOCK_PLUS_WEB_REQUEST_ANDROID_H
#define ADBLOCK_PLUS_WEB_REQUEST_ANDROID_H

#include <jni.h>
#include <AdblockPlus/WebRequest.h>

class AndroidWebRequest : public AdblockPlus::WebRequest
{
  JavaVM* globalJvm;

  jclass jUrlClass;
  jmethodID jUrlConstructorID;
  jmethodID jUrlOpenConnectionID;
  jclass jConnectionClass;
  jmethodID jConnectionSetMethodID;
  jmethodID jConnectionSetRequestPropertyID;
  jmethodID jConnectionConnectID;
  jmethodID jConnectionGetContentLengthID;
  jmethodID jConnectionGetResponseCodeID;
  jmethodID jConnectionGetContentEncodingID;
  jmethodID jConnectionGetInputStreamID;
  jclass jInputStreamReaderClass;
  jmethodID jInputStreamReaderConstructorID;
  jclass jBufferedReaderClass;
  jmethodID jBufferedReaderConstructorID;
  jclass jStringBuilderClass;
  jmethodID jStringBuilderConstructorID;
  jmethodID jBufferedReaderReadID;
  jmethodID jStringBuilderAppendID;
  jmethodID jStringBuilderToStringID;
  jmethodID jBufferedReaderCloseID;
  jmethodID jConnectionGetHeaderFieldKeyID;
  jmethodID jConnectionGetHeaderFieldID;

public:
  AndroidWebRequest(JavaVM*& gJvm);
  ~AndroidWebRequest();
  AdblockPlus::ServerResponse GET(const std::string& url, const AdblockPlus::HeaderList& requestHeaders) const;
};
#endif
