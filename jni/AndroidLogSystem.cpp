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

#include <android/log.h>
#include "AndroidLogSystem.h"

void AndroidLogSystem::operator()(AdblockPlus::LogSystem::LogLevel logLevel,
    const std::string& message, const std::string& source)
{
  int lvl = ANDROID_LOG_DEFAULT;
  switch (logLevel)
  {
    case LOG_LEVEL_TRACE:
      lvl = ANDROID_LOG_VERBOSE;
      break;
    case LOG_LEVEL_LOG:
      lvl = ANDROID_LOG_DEBUG;
      break;
    case LOG_LEVEL_INFO:
      lvl = ANDROID_LOG_INFO;
      break;
    case LOG_LEVEL_WARN:
      lvl = ANDROID_LOG_WARN;
      break;
    case LOG_LEVEL_ERROR:
      lvl = ANDROID_LOG_ERROR;
      break;
  }
  __android_log_print(lvl, "JS", message.c_str());
  if (source.size())
  {
	__android_log_print(lvl, "JS", "\n");
	__android_log_print(lvl, "JS", " at ");
	__android_log_print(lvl, "JS", source.c_str());
  }
  __android_log_print(lvl, "JS", "\n");
}
