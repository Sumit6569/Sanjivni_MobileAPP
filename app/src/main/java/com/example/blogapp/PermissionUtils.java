package com.example.blogapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.AlertDialog;

public class PermissionUtils {
    
    public static boolean checkAndRequestPermissions(Activity activity, String[] permissions, int requestCode) {
        boolean allPermissionsGranted = true;
        
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }
        
        if (!allPermissionsGranted) {
            boolean shouldShowRationale = false;
            for (String permission : permissions) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    shouldShowRationale = true;
                    break;
                }
            }
            
            if (shouldShowRationale) {
                new AlertDialog.Builder(activity)
                    .setTitle("Permission Required")
                    .setMessage("Camera and storage permissions are required for this feature. Please grant them in the next dialog.")
                    .setPositiveButton("OK", (dialog, which) -> 
                        ActivityCompat.requestPermissions(activity, permissions, requestCode))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
            } else {
                // First time asking or user checked "Never ask again"
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
            }
            return false;
        }
        return true;
    }
    
    public static void showSettingsDialog(Activity activity) {
        new AlertDialog.Builder(activity)
            .setTitle("Permissions Required")
            .setMessage("This app needs camera and storage permissions to function properly. Please grant them in Settings.")
            .setPositiveButton("Settings", (dialog, which) -> {
                dialog.dismiss();
                openAppSettings(activity);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .create()
            .show();
    }
    
    public static void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}
