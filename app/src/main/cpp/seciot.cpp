#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jint JNICALL
Java_com_wrlus_fridahooker_util_NativeRootShell_execute(JNIEnv *env, jclass type, jstring cmd) {
    const char * ccmd = env->GetStringUTFChars(cmd, nullptr);
    int ret_code = system(ccmd);
    env->ReleaseStringUTFChars(cmd, ccmd);
    return ret_code;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_wrlus_fridahooker_util_NativeRootShell_executeRoot(JNIEnv *env, jclass type, jstring cmd) {
    const char su_prefix[7] = "su -c ";
    const char * ccmd = env->GetStringUTFChars(cmd, nullptr);
    jsize cmd_len = env->GetStringUTFLength(cmd);
    char *scmd = new char[strlen(su_prefix) + cmd_len + 1];
    strlcpy(scmd, su_prefix, sizeof(scmd));
    strcat(scmd, ccmd);
    int ret_code = system(scmd);
    delete[] scmd;
    env->ReleaseStringUTFChars(cmd, ccmd);
    return ret_code;
}