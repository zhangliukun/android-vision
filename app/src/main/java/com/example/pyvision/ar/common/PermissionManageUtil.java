/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Permission helper, provide the method of camera authority application and judgment.
 *
 * @author HW
 * @since 2020-03-20
 */
public class PermissionManageUtil {
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private static final String[] PERMISSIONS_ARRAYS = new String[]{
        Manifest.permission.CAMERA};

    // Permission list to request.
    private static List<String> permissionsList = new ArrayList<>();

    private PermissionManageUtil() {
    }

    /**
     * Determine whether the current application has some necessary
     * permissions(currently we need camera permissions by default).
     * If not, apply for permissions.
     * This method should be called when the main Activity onResume
     *
     * @param activity Activity
     */
    public static void onResume(final Activity activity) {
        boolean isHasPermission = true;
        for (String permission : PERMISSIONS_ARRAYS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                isHasPermission = false;
                break;
            }
        }
        if (!isHasPermission) {
            for (String permission : PERMISSIONS_ARRAYS) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsList.add(permission);
                }
            }
            ActivityCompat.requestPermissions(activity,
                permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    /**
     * Check whether the current application has the required permissions.
     * This method should be called from {@link ChooseActivity#onRequestPermissionsResult}.
     *
     * @param activity Activity.
     * @return Has permission or not.
     */
    public static boolean hasPermission(final Activity activity) {
        for (String permission : PERMISSIONS_ARRAYS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}