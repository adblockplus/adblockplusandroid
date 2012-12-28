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

#include <v8.h>
#include <jni.h>

extern JavaVM* globalJvm;
extern jobject jniCallback;

extern v8::Handle<v8::Value> loadImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> printImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> setStatusImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> canAutoupdateImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> showToastImpl(const v8::Arguments& args);

extern v8::Handle<v8::Value> fileExistsImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileLastModifiedImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileRemoveImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileRenameImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileReadImpl(const v8::Arguments& args);
extern v8::Handle<v8::Value> fileWriteImpl(const v8::Arguments& args);

extern v8::Handle<v8::Value> httpSendImpl(const v8::Arguments& args);

extern v8::Handle<v8::Value> setTimeoutImpl(const v8::Arguments& args);
extern long RunNextCallback(v8::Handle<v8::Context> context);
extern void ClearQueue();
