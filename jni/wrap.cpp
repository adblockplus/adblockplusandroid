#include <math.h>
#include <android/log.h>
#include <jni.h>
#include <v8.h>

jobject NewBoolean(JNIEnv *pEnv, jboolean value)
{
	static jclass cls = pEnv->FindClass("java/lang/Boolean");
	static jmethodID cid = pEnv->GetMethodID(cls, "<init>", "(Z)V");
	return pEnv->NewObject(cls, cid, value);
}

jobject NewInt(JNIEnv *pEnv, jint value)
{
	static jclass cls = pEnv->FindClass("java/lang/Integer");
	static jmethodID cid = pEnv->GetMethodID(cls, "<init>", "(I)V");
	return pEnv->NewObject(cls, cid, value);
}

jobject NewLong(JNIEnv *pEnv, jlong value)
{
	static jclass cls = pEnv->FindClass("java/lang/Long");
	static jmethodID cid = pEnv->GetMethodID(cls, "<init>", "(J)V");
	return pEnv->NewObject(cls, cid, value);
}

jobject NewDouble(JNIEnv *pEnv, jdouble value)
{
	static jclass cls = pEnv->FindClass("java/lang/Double");
	static jmethodID cid = pEnv->GetMethodID(cls, "<init>", "(D)V");
	return pEnv->NewObject(cls, cid, value);
}

jstring NewString(JNIEnv *pEnv, v8::Handle<v8::String> str)
{
  return pEnv->NewStringUTF(*v8::String::Utf8Value(str));
}

jobject NewDate(JNIEnv *pEnv, v8::Handle<v8::Date> date)
{
	static jclass cls = pEnv->FindClass("java/lang/Double");
	static jmethodID cid = pEnv->GetMethodID(cls, "<init>", "(J)V");
	jlong value = floor(date->NumberValue());
	return pEnv->NewObject(cls, cid, value);
}

jobject wrap(JNIEnv *pEnv, v8::Handle<v8::Value> value)
{
  v8::HandleScope handle_scope;

  if (value.IsEmpty() || value->IsNull() || value->IsUndefined()) return NULL;
  if (value->IsTrue()) return NewBoolean(pEnv, JNI_TRUE);
  if (value->IsFalse()) return NewBoolean(pEnv, JNI_FALSE);

  if (value->IsInt32()) return NewInt(pEnv, value->Int32Value());
  if (value->IsUint32()) return NewLong(pEnv, value->IntegerValue());
  if (value->IsString()) return NewString(pEnv, v8::Handle<v8::String>::Cast(value));
  if (value->IsDate()) return NewDate(pEnv, v8::Handle<v8::Date>::Cast(value));
  if (value->IsNumber()) return NewDouble(pEnv, value->NumberValue());

//  return wrap(value->ToObject());
  return NULL;
}

v8::Handle<v8::Value> wrap(JNIEnv *pEnv, jobject value)
{
	v8::HandleScope handle_scope;
	v8::TryCatch try_catch;

	if (value == NULL)
		return handle_scope.Close(v8::Null());

	v8::Handle < v8::Value > result;

	jclass cls = pEnv->GetObjectClass(value);

	if (pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/String")) == JNI_TRUE)
	{
		jstring str = (jstring) value;
		const char *p = pEnv->GetStringUTFChars(str, NULL);

		result = v8::String::New(p, pEnv->GetStringUTFLength(str));

		pEnv->ReleaseStringUTFChars(str, p);
	}
	else if (pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Long")) == JNI_TRUE
			|| pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Integer")) == JNI_TRUE
			|| pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Short")) == JNI_TRUE
			|| pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Byte")) == JNI_TRUE)
	{
		static jmethodID mid = pEnv->GetMethodID(pEnv->FindClass("java/lang/Number"), "intValue", "()I");

		result = v8::Integer::New(pEnv->CallIntMethod(value, mid));
	}
	else if (pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Double")) == JNI_TRUE
			|| pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Float")) == JNI_TRUE)
	{
		static jmethodID mid = pEnv->GetMethodID(pEnv->FindClass("java/lang/Number"), "doubleValue", "()D");

		result = v8::Number::New(pEnv->CallDoubleMethod(value, mid));
	}
	else if (pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/Boolean")) == JNI_TRUE)
	{
		static jmethodID mid = pEnv->GetMethodID(pEnv->FindClass("java/lang/Boolean"), "booleanValue", "()Z");

		result = v8::Boolean::New(pEnv->CallBooleanMethod(value, mid));
	}
	else if (pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/util/Date")) == JNI_TRUE)
	{
		static jmethodID mid = pEnv->GetMethodID(pEnv->FindClass("java/util/Date"), "getTime", "()J");

		result = v8::Date::New(pEnv->CallLongMethod(value, mid));
	}
	/*
	 else if (pEnv->IsAssignableFrom(cls, pEnv->FindClass("java/lang/reflect/Method")) == JNI_TRUE)
	 {
	 result = jni::CJavaFunction::Wrap(pEnv, value);
	 }
	 */
	else
	{
		static jmethodID mid = pEnv->GetMethodID(pEnv->FindClass("java/lang/Class"), "isArray", "()Z");

		if (pEnv->CallBooleanMethod(pEnv->GetObjectClass(value), mid))
		{
			size_t len = pEnv->GetArrayLength((jarray) value);
			v8::Handle<v8::Array> items = v8::Array::New(len);

			for (size_t i=0; i<len; i++)
			{
				jobject item = pEnv->GetObjectArrayElement((jobjectArray) value, i);
				items->Set(i, wrap(pEnv, item));
				pEnv->DeleteLocalRef(item);
			}

			result = items;
		}
	}

	return try_catch.HasCaught() ? v8::Handle<v8::Value>() : handle_scope.Close(result);
//  return ThrowIf(try_catch) ? v8::Handle<v8::Value>() : handle_scope.Close(result);
}
