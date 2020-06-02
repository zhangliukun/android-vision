#include <jni.h>
#include <string>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include "opencvutil.h"
#include "colortransfer.h"

#define TAG "PyVision-jni"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型

using namespace std;
using namespace cv;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_pyvision_ui_main_MainFragment_stringFromJNI(JNIEnv *env, jobject thiz) {
    // TODO: implement stringFromJNI()
    string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jintArray JNICALL
Java_com_example_pyvision_ui_main_MainFragment_grayJNI(JNIEnv *env, jobject thiz, jintArray buf,
                                                       jint w, jint h) {
    jint* cbuf = env->GetIntArrayElements(buf,JNI_FALSE);   //获取将java传入的数组本地化
    if(cbuf == NULL){
        return 0;
    }

    Mat imgData(h,w,CV_8UC4,(unsigned char *) cbuf);

    uchar* ptr = imgData.ptr(0);
    for(int i = 0; i < w*h; i ++){
        //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
        //对于一个int四字节，其彩色值存储方式为：BGRA
        int grayScale = (int)(ptr[4*i+2]*0.299 + ptr[4*i+1]*0.587 + ptr[4*i+0]*0.114);
        ptr[4*i+1] = grayScale;
        ptr[4*i+2] = grayScale;
        ptr[4*i+0] = grayScale;
    }

    int size = w * h;
    // 在java中申请整型数组，这个数组可以返回给java使用。这里不能使用new或者malloc，因为这得到的空间还在c中，java不能直接使用
    jintArray result = env->NewIntArray(size);
    // 将c的数组拷贝到java中的数组，不能直接赋值，因为java中的数组c不能直接操作
    env->SetIntArrayRegion(result,0,size,cbuf);
    env->ReleaseIntArrayElements(buf,cbuf,0);
    return result;

}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_pyvision_cameraapi_cameraX_CameraXActivity_process_1bitmap(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jobject bitmap) {
    // TODO: implement process_bitmap()
    Mat mat_image_src;
    BitmapToMat(env,bitmap,mat_image_src);//图片转为Mat
    Mat mat_image_dst;
    blur(mat_image_src,mat_image_dst,Size2i(10,10));
    MatToBitmap(env,mat_image_dst,bitmap);//Mat转图片

}
extern "C"
JNIEXPORT void JNICALL
Java_com_example_pyvision_cameraapi_cameraX_CameraXActivity_transfer_1color(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jobject src_bitmap,
                                                                            jobject tar_bitmap) {
    // TODO: implement transfer_color()
    Mat mat_image_src,mat_image_tar,mat_result;
    BitmapToMat(env,src_bitmap,mat_image_src);
    BitmapToMat(env,tar_bitmap,mat_image_tar);
    mat_result = color_transfer(mat_image_src,mat_image_tar, true);
    MatToBitmap(env,mat_result,tar_bitmap);
    mat_image_src.release();
    mat_image_src.release();
    mat_result.release();


}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_pyvision_colortransfer_ColorTransferActivity2_transfer_1color(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jobject src_bitmap,
                                                                               jobject tar_bitmap,
                                                                               jobject result_bitmap) {
    // TODO: implement transfer_color()
    Mat mat_image_src,mat_image_tar,mat_result;
    BitmapToMat(env,src_bitmap,mat_image_src);
    BitmapToMat(env,tar_bitmap,mat_image_tar);
    mat_result = color_transfer(mat_image_src,mat_image_tar, true);
    transpose(mat_result,mat_result);
    flip(mat_result,mat_result,1);
    resize(mat_result,mat_result,Size(1080,1440));
    MatToBitmap(env,mat_result,result_bitmap);
    mat_image_src.release();
    mat_image_src.release();
    mat_result.release();
}