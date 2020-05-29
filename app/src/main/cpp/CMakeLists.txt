# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

FILE(GLOB cppfiles
        "../cpp/*.h"
        "../cpp/*.cpp"
        )

# 创建和命名一个库，设置为STATIC或者SHARED，并且提供到源代码的相对路径，你能够定义多个libraries，
# 然后CMake会为你构建。Gradle自动将共享库打包进你的APK中。
# 静态库和动态库（共享库）的区别：静态库会将代码打包进可执行文件，动态库直接连接，不会打包进代码。
add_library( # 设置library的名字
             native-lib

             # 设置library为一个共享库
             SHARED

             # 提供到源代码的相对路径
#             native-lib.cpp )
             ${cppfiles})

set(Infomation, "hello zlk")
MESSAGE(STATUS "the cppfiles are ${cppfiles}")

# 寻找指定的预构建的库并且将路径存储为一个变量。因为CMake默认会将搜索路径中的系统libraries包含进来
# 你只需要指定你想要添加的公共的NDK库的名字CMake会在完成构建之前确定库是否存在
find_library( # 设置路径变量的名字
              log-lib

              # 设置你想要CMake去定位的NDK库的名字
              log )

# 设置opencv的路径
set(PATH_OPENCV ~/MyFileRoot/software_source/OpenCV-android-sdk)
# 配置加载native依赖项
include_directories(${PATH_OPENCV}/sdk/native/jni/include)
# 动态方式加载
add_library(lib_opencv STATIC IMPORTED) # 创建一个导入库，静态方式
#引入libopencv_java3.so文件 ,这里使用软链接找不到，所以直接使用源文件路径了。
set_target_properties(lib_opencv
        PROPERTIES
#        IMPORTED_LOCATION ${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so
        IMPORTED_LOCATION /Users/zale/MyFileRoot/software_source/OpenCV-android-sdk/sdk/native/libs/${ANDROID_ABI}/libopencv_java3.so
        )



# 指定CMake应该链接到你的目标库的类库。你可以链接多个库，比如那些libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
# android/bitmap.h这个头文件用于在JNI层操作Bitmap对象的，其包含于jnigraphics库中，所以要在CMakeLists.txt中的target_link_libraries加入-ljnigraphics
target_link_libraries( # Specifies the target library.
                       native-lib

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} lib_opencv -ljnigraphics)