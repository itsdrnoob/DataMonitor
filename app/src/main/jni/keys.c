//
// Created by drnoob on 04/05/22.
//

#include <jni.h>

JNIEXPORT jstring JNICALL
Java_com_drnoob_datamonitor_ui_fragments_NetworkDiagnosticsFragment_getApiKey(JNIEnv *env,
jobject instance) {
    // TODO: implement getApiKey()
    return (*env)->  NewStringUTF(env, "NDIwYTI1Yzk4ZDk3MTc="); // F-Droid // Replace with Base64 encoded Api key from https://ipinfo.io/
}

JNIEXPORT jstring JNICALL
Java_com_drnoob_datamonitor_ui_fragments_NetworkDiagnosticsFragment_getToken(JNIEnv *env,
jobject instance) {
    return (*env)->  NewStringUTF(env, "YjU4NTg2NDctYWEzMS00NTkxLWFjYWQtNWYxMzNiM2QyYmI2");
}