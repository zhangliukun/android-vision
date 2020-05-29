package com.example.pyvision.utils;

import android.util.Log;

/**
 * Created by zale.zhang on 2020/5/29.
 *
 * @author zale.zhang
 */
public class TimeUtils {

    public interface runtimeCallback{
        void doSomething();
    }

    public static void countTime(String Tag,runtimeCallback callback){
        if(Tag == null){
            Tag = "nog Tag";
        }
        long startTime= System.currentTimeMillis(); //起始时间
        callback.doSomething(); ///进行回调操作
        long endTime = System.currentTimeMillis(); //结束时间
        Log.i(Tag,String.format("方法使用时间 %d ms", endTime - startTime)); //打印使用时间
    }

}
