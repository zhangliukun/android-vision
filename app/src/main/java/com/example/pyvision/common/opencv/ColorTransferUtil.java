package com.example.pyvision.common.opencv;

import android.graphics.Bitmap;

/**
 * Created by zale.zhang on 2020/7/7.
 *
 * @author zale.zhang
 */

public class ColorTransferUtil {
    public static native void transfer_color(Bitmap src_bitmap, Bitmap tar_bitmap, Bitmap result_bitmap);

    public static native void transfer_color_same_size(Bitmap src_bitmap, Bitmap tar_bitmap, Bitmap result_bitmap);
}
