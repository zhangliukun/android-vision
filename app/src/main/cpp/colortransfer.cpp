//
// Created by zale on 2020/5/29.
//

#include "colortransfer.h"
using namespace std;
using namespace cv;

Mat color_transfer(Mat &color_source,Mat &target,bool preserve_paper){

//    source = source.clone();
//    Mat source = imread("../images/autumn.jpg",IMREAD_COLOR);
//    Mat target = imread("../images/storm.jpg",IMREAD_COLOR);
    Mat source = color_source.clone();

    // 将图像从RGB到LAB颜色空间，需要确保使用浮点数据类型，其中OpenCV期望floats为32bit的，所以替换掉64bit的
    cvtColor(source,source,COLOR_BGR2Lab);
    cvtColor(target,target,COLOR_BGR2Lab);

    source.convertTo(source,CV_32F);
    target.convertTo(target,CV_32F);

    // 计算原图像和目标图像的颜色统计信息
    //(lMeanSrc, lStdSrc, aMeanSrc, aStdSrc, bMeanSrc, bStdSrc)
    vector<float> src_mean_std;
    vector<float> tar_mean_std;
    src_mean_std = image_status(source);
    tar_mean_std = image_status(target);

    // 从目标图片减去均值
    vector<Mat> tar_labChannels; // (l,a,b)
    split(target,tar_labChannels);
    tar_labChannels[0] -= tar_mean_std[0];
    tar_labChannels[1] -= tar_mean_std[2];
    tar_labChannels[2] -= tar_mean_std[4];

    // 使用true的效果更好一点
    //preserve_paper = true;
    if(preserve_paper){
        //使用论文提出的缩放因子来对标准差进行缩放
        tar_labChannels[0] = (tar_mean_std[1] / src_mean_std[1]) * tar_labChannels[0]; // l = (l_std_tar / l_std_src) * l
        tar_labChannels[1] = (tar_mean_std[3] / src_mean_std[3]) * tar_labChannels[1];
        tar_labChannels[2] = (tar_mean_std[5] / src_mean_std[5]) * tar_labChannels[2];
    }else{
        //使用论文提出的缩放因子的导数来对标准差进行缩放
        tar_labChannels[0] = (src_mean_std[1] / tar_mean_std[1] ) * tar_labChannels[0]; // l = (l_std_src / l_std_tar ) * l
        tar_labChannels[1] = (src_mean_std[3] / tar_mean_std[3] ) * tar_labChannels[1];
        tar_labChannels[2] = (src_mean_std[5] / tar_mean_std[5] ) * tar_labChannels[2];
    }

    // 添加到原图的mean
    tar_labChannels[0] += src_mean_std[0];
    tar_labChannels[1] += src_mean_std[2];
    tar_labChannels[2] += src_mean_std[4];

    // 将像素值裁剪或者归一化到[0,255] ，不知道为什么加了这个效果就不好
//    normalize(tar_labChannels[0],tar_labChannels[0],0,255,NORM_MINMAX);
//    normalize(tar_labChannels[1],tar_labChannels[1],0,255,NORM_MINMAX);
//    normalize(tar_labChannels[2],tar_labChannels[2],0,255,NORM_MINMAX);


    // 将通道合并起来然后转回RGB颜色空间，需要确保使用8位无符号整型数据
    Mat result_image;
    merge(tar_labChannels,result_image);
    result_image.convertTo(result_image,CV_8U);
    cvtColor(result_image,result_image,COLOR_Lab2BGR);

//    namedWindow("result",WINDOW_AUTOSIZE);
//    imshow("result",result_image);
//    waitKey(0);

    return result_image;

}

/**
 * 分别返回L,a,b通道的均值和标准差。
 * @param image
 */
vector<float > image_status(const Mat& image){
    //计算每个通道的均值和标准差
    int channel = 3;
    vector<Mat> labChannels(channel);
    // l,a,b
    split(image,labChannels);
    vector<float> result;
    for(int i=0;i< channel;i++){
        Scalar mean;
        Scalar dev;
        meanStdDev(labChannels[i],mean,dev);
        result.push_back(mean.val[0]);
        result.push_back(dev.val[0]);
    }
    return result;
}