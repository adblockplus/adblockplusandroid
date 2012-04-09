#include <unistd.h>
#include <list>
#include "ops.h"

typedef struct __QueueEntry
{
  v8::Persistent<v8::Function> callback;
  int64_t delay;
} QueueEntry;

static std::list<QueueEntry*> queue;

v8::Handle<v8::Value> setTimeoutImpl(const v8::Arguments& args)
{
  v8::HandleScope handle_scope;

  if (args.Length() < 2)
    return v8::ThrowException(v8::String::New("Not enough parameters"));

  if (!args[0]->IsFunction())
    return v8::ThrowException(v8::String::New("Parameter 0 must be a function"));
  if (!args[1]->IsInt32())
    return v8::ThrowException(v8::String::New("Parameter 1 must be an integer"));

  v8::Handle<v8::Function> callback = v8::Handle<v8::Function>::Cast(args[0]);

  QueueEntry* entry = new QueueEntry;
  entry->callback = v8::Persistent<v8::Function>::New(callback);
  entry->delay = (v8::Handle<v8::Integer>::Cast(args[1]))->Value();

  queue.push_back(entry);

  jlong jnum = entry->delay;

  static jclass cls = jniEnv->GetObjectClass(jniCallback);
  static jmethodID mid = jniEnv->GetMethodID(cls, "notify", "(J)V");
  if (mid)
    jniEnv->CallVoidMethod(jniCallback, mid, jnum);

  return v8::Undefined();
}

long RunNextCallback(v8::Handle<v8::Context> context)
{
  if (queue.size() == 0)
    return -1;

  std::list<QueueEntry*>::iterator minEntry = queue.begin();
  for (std::list<QueueEntry*>::iterator it = minEntry; it != queue.end(); it++)
    if ((*it)->delay < (*minEntry)->delay)
      minEntry = it;

  int64_t delay = (*minEntry)->delay;
  if (delay > 0)
  {
	// Here we assume that we will be called next time after the specified delay,
	// but it can happen earlier - should fix this.
    for (std::list<QueueEntry*>::iterator it = queue.begin(); it != queue.end(); it++)
      (*it)->delay -= delay;
    return delay;
  }

  QueueEntry* entry = *minEntry;
  queue.erase(minEntry);

  entry->callback->Call(context->Global(), 0, NULL);
  entry->callback.Dispose();
  delete entry;

  return 0;
}

void ClearQueue()
{
	queue.clear();
}
