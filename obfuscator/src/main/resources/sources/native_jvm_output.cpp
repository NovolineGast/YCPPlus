#include "native_jvm.hpp"
#include "native_jvm_output.hpp"
#include "VMProtectSDK.h"
#include <iostream>

$includes

namespace native_jvm {

    typedef void (* reg_method)(JNIEnv *,jclass);

    reg_method reg_methods[$class_count];

    void register_for_class(JNIEnv *env, jclass, jint id, jclass clazz) {
        reg_methods[id](env, clazz);
    }

    void prepare_lib(JNIEnv *env) {
        utils::init_utils(env);
        if (env->ExceptionCheck())
            return;


$register_code

        if (env->ExceptionCheck())
            return;

        JNINativeMethod loader_methods[] = {
            { const_cast<char*>(VMProtectDecryptStringA("ProtectedByYumeCloud")), const_cast<char*>(VMProtectDecryptStringA("(ILjava/lang/Class;)V")), (void *)&register_for_class }
        };
        env->RegisterNatives(env->FindClass(VMProtectDecryptStringA("YumeCloudProtection/ThisApplicationIsProtectedByYumeCloud")), loader_methods, 1);
    }
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
$watermark
$authorization
    JNIEnv *env = nullptr;
    vm->GetEnv((void **)&env, JNI_VERSION_1_8);
    native_jvm::prepare_lib(env);
    return JNI_VERSION_1_8;
}