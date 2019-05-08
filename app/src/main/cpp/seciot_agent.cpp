#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wrlus_seciot_hook_FridaServerAgent_requestRootPermission(JNIEnv *env, jclass type) {
    int checkValue = system("su -c ls /data");
    if(checkValue == 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}