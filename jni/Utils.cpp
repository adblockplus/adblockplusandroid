/*
 * This file is part of Adblock Plus <http://adblockplus.org/>,
 * Copyright (C) 2006-2014 Eyeo GmbH
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

#include "Utils.h"
#include "Debug.h"

const std::string GetString(JNIEnv *pEnv, jstring str)
{
  D(D_WARN, "getString()");

  if (str == NULL)
    return std::string();

  jboolean iscopy;

  const char *s = pEnv->GetStringUTFChars(str, &iscopy);
  jsize len = pEnv->GetStringUTFLength(str);

  const std::string value(s, len);

  pEnv->ReleaseStringUTFChars(str, s);

  return value;
}
