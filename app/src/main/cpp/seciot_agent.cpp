#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_wrlus_seciot_hook_FridaServerAgent_requestRootPermission(JNIEnv *env, jclass type) {
    int checkValue = system("su -c whoami");
    printf("%d", checkValue);
    if(checkValue == 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}