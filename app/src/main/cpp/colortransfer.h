//
// Created by zale on 2020/5/29.
//

#ifndef PYVISION_COLORTRANSFER_H
#define PYVISION_COLORTRANSFER_H

#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>
#include <opencv2/highgui.hpp>
#include <opencv2/imgproc.hpp>
#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>

cv::Mat color_transfer(cv::Mat& color_from,cv::Mat& color_target,bool preserve_paper = true);
std::vector<float > image_status(const cv::Mat& image);

#endif //PYVISION_COLORTRANSFER_H
