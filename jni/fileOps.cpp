#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <sys/stat.h>
#include <android/log.h>
#include "ops.h"

v8::Handle<v8::Value> fileExistsImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 1)
  {
    return v8::ThrowException(v8::String::New("File name expected"));
  }
  v8::String::AsciiValue fileName(args[0]);
  if (!*fileName)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", "fileExists(%s)", *fileName);

  struct stat buf;
  int result = stat(*fileName, &buf);

  return v8::Boolean::New(result == 0 && S_ISREG(buf.st_mode));
}

v8::Handle<v8::Value> fileLastModifiedImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 1)
  {
    return v8::ThrowException(v8::String::New("File name expected"));
  }
  v8::String::AsciiValue fileName(args[0]);
  if (!*fileName)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", "fileLastModified(%s)", *fileName);

  struct stat buf;
  int result = stat(*fileName, &buf);

  return v8::Number::New((double)buf.st_mtime * 1000);
}

v8::Handle<v8::Value> fileRemoveImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 1)
  {
    return v8::ThrowException(v8::String::New("File name expected"));
  }
  v8::String::AsciiValue fileName(args[0]);
  if (!*fileName)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", "fileRemove(%s)", *fileName);

  int result = unlink(*fileName);

  if (result == 0)
    return v8::Undefined();
  else
    return v8::ThrowException(v8::String::New("File couldn't be removed"));
}

v8::Handle<v8::Value> fileRenameImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 2)
  {
    return v8::ThrowException(v8::String::New("File names expected"));
  }
  v8::String::AsciiValue fileName(args[0]);
  if (!*fileName)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }

  v8::String::AsciiValue newPath(args[1]);
  if (!*newPath)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", "fileRename(%s, %s)", *fileName, *newPath);

  int result = rename(*fileName, *newPath);

  if (result == 0)
    return v8::Undefined();
  else
    return v8::ThrowException(v8::String::New("File couldn't be renamed"));
}

v8::Handle<v8::Value> fileReadImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 1)
  {
    return v8::ThrowException(v8::String::New("File name expected"));
  }
  v8::String::AsciiValue fileName(args[0]);
  if (!*fileName)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", "fileRead(%s)", *fileName);

  FILE* file = fopen(*fileName, "rb");
  if (!file)
  {
    v8::Handle<v8::String> fileNameString = v8::String::New(*fileName);
    return v8::ThrowException(v8::String::Concat(v8::String::New("Failed opening file: "), fileNameString));
  }

  fseek(file, 0, SEEK_END);
  long size = ftell(file);
  rewind(file);

  char* buffer = new char[size];
  if (!buffer)
  {
    fclose(file);
    return v8::ThrowException(v8::String::New("Out of memory"));
  }
  size_t readSize = fread(buffer, 1, size, file);
  fclose(file);
  if (size != readSize)
  {
    delete buffer;
    return v8::ThrowException(v8::String::New("File read error"));
  }

  v8::Handle<v8::String> data = v8::String::New(buffer, size);
  delete buffer;
  return data;
}

v8::Handle<v8::Value> fileWriteImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 1)
  {
    return v8::ThrowException(v8::String::New("File name expected"));
  }
  v8::String::AsciiValue fileName(args[0]);
  if (!*fileName)
  {
    return v8::ThrowException(v8::String::New("File name isn't a string"));
  }
  __android_log_print(ANDROID_LOG_INFO, "JS", "fileWrite(%s)", *fileName);

  if (args.Length() < 2)
  {
    return v8::ThrowException(v8::String::New("Data to write expected"));
  }
  v8::String::Utf8Value data(args[1]);
  if (!*data)
  {
    return v8::ThrowException(v8::String::New("Data to write is not a string"));
  }

  FILE* file = fopen(*fileName, "wb");
  if (!file)
  {
    v8::Handle<v8::String> fileNameString = v8::String::New(*fileName);
    return v8::ThrowException(v8::String::Concat(v8::String::New("Failed opening file: "), fileNameString));
  }

  size_t writeSize = fwrite(*data, 1, data.length(), file);
  fclose(file);

  if (data.length() != writeSize)
  {
    fileRemoveImpl(args);
    return v8::ThrowException(v8::String::New("File write error"));
  }

  return v8::Undefined();
}
