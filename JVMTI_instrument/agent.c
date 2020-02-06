#include <jvmti.h>
#include <stdio.h>
#include <string.h>

// モニターに入ろうとしたけど，ロックが取れなかったときに呼び出される関数
JNIEXPORT void JNICALL MonitorContendedEnter(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread, jobject object) {
    printf("MonitorContendedEnter\n");
}

// モニター開始
// スレッドがモニターに入ったときに呼び出される関数
JNIEXPORT void JNICALL MonitorContendedEntered(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread, jobject object) {
    printf("MonitorContendedEntered\n");
}

// モニター待機
// Object.wait ()を入る時に呼び出される関数
JNIEXPORT void JNICALL MonitorWait(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread, jobject object, jlong timeout) {
    printf("MonitorWait\n");
    jvmtiFrameInfo frames[5];
    jint count;
    jvmtiError err;

    err = (*jvmti_env)->GetStackTrace(jvmti_env, thread, 0, 5, frames, &count);
    if (err == JVMTI_ERROR_NONE && count >= 1) {
        char *methodName;
        char *className;
        jclass methodClass;
        FILE *fp;
        fp = fopen("JVMTILog.log", "a");
        if (fp == NULL) {
            printf("file open Error");
        }
        for (int i = 0; i < 2; i++) {
            err = (*jvmti_env)->GetMethodName(jvmti_env, frames[i].method, &methodName, NULL, NULL);
            err = (*jvmti_env)->GetMethodDeclaringClass(jvmti_env, frames[i].method, &methodClass);
            err = (*jvmti_env)->GetClassSignature(jvmti_env, methodClass, &className, NULL);
            if (err == JVMTI_ERROR_NONE) {
                printf("Executing method:%d %s %s\n", i, methodName, className);
                fprintf(fp, "Executing method:%d %s %s\n", i, methodName, className);
            }
        }
        fclose(fp);
    }
}

// モニター待機終了
// Object.wait ()を出る時に呼び出される関数
JNIEXPORT void JNICALL MonitorWaited(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread, jobject object, jboolean timed_out) {
    printf("MonitorWaited\n");
}

// JavaVM初期化後に呼び出される関数
JNIEXPORT void JNICALL VMInit(jvmtiEnv *jvmti, JNIEnv *env, jthread thread) {
    printf("VMInit\n");

    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTER, NULL);
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_MONITOR_CONTENDED_ENTERED, NULL);
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAIT, NULL);
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_MONITOR_WAITED, NULL);
}

// JVMTIエージェントのライブラリのロード時に呼び出される関数
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
    jvmtiEnv *jvmti;
    jvmtiCapabilities capabilities;
    jvmtiEventCallbacks callbacks;

    FILE *fp;
    fp = fopen("JVMTILog.log", "w");
    if (fp == NULL) {
        printf("file open Error");
    }
    fclose(fp);

    printf("Hello, JVMTI.\n");

    (*vm)->GetEnv(vm, (void **)&jvmti, JVMTI_VERSION_1);  // jvmtiEnv の獲得

    memset(&capabilities, 0, sizeof(capabilities));  // jvmtiCapabilities の初期化
    capabilities.can_generate_monitor_events = 1;    // monitor event の追加
    (*jvmti)->AddCapabilities(jvmti, &capabilities);

    memset(&callbacks, 0, sizeof(callbacks));  // jvmtiEventCallbacks の初期化
    callbacks.VMInit = VMInit;
    callbacks.MonitorContendedEnter = MonitorContendedEnter;
    callbacks.MonitorContendedEntered = MonitorContendedEntered;
    callbacks.MonitorWait = MonitorWait;
    callbacks.MonitorWaited = MonitorWaited;

    (*jvmti)->SetEventCallbacks(jvmti, &callbacks, (jint)sizeof(callbacks));
    (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    return JNI_OK;
}

// JavaVM終了際に呼び出される関数
JNIEXPORT void JNICALL Agent_OnUnload(JavaVM *vm) {
    printf("Good-bye, JVMTI.\n");
}

// GetObjectMonitorUsage を使えば キューの中身とかモニタの状態を得られるはず
