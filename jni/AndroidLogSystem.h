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

#ifndef ADBLOCK_PLUS_ANDROID_LOG_SYSTEM_H
#define ADBLOCK_PLUS_ANDROID_LOG_SYSTEM_H

#include <AdblockPlus/LogSystem.h>

class AndroidLogSystem : public AdblockPlus::LogSystem
{
public:
  void operator()(LogLevel logLevel, const std::string& message,
        const std::string& source);
};

#endif
