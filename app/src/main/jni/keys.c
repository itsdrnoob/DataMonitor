//
// Created by drnoob on 04/05/22.
//

#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_drnoob_datamonitor_ui_fragments_NetworkDiagnosticsFragment_getApiKey(JNIEnv *env,
                                                                              jobject instance) {
    // TODO: implement getApiKey()
    return (*env)->  NewStringUTF(env, "<API_KEY>"); // Base64 encoded Api key from https://ipinfo.io/
}