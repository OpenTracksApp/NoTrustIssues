package de.dennisguse.notrustissues.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionRequester {

    private final List<String> permissions;

    public PermissionRequester(List<String> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission(Context context) {
        return permissions.stream()
                .map(p -> ContextCompat.checkSelfPermission(context, p))
                .allMatch(r -> r == PackageManager.PERMISSION_GRANTED);
    }

    public void requestPermissionsIfNeeded(Context context, ActivityResultCaller caller, @Nullable Runnable onGranted, @Nullable RejectedCallback onRejected) {
        if (!hasPermission(context)) {
            requestPermission(caller, onGranted, onRejected);
        }
    }

    public boolean shouldShowRequestPermissionRationale(Fragment context) {
        return permissions.stream()
                .anyMatch(context::shouldShowRequestPermissionRationale);
    }

    private void requestPermission(ActivityResultCaller context, @Nullable Runnable onGranted, @Nullable RejectedCallback onRejected) {
        ActivityResultLauncher<String[]> locationPermissionRequest = context.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean isGranted = permissions.stream()
                            .allMatch(p -> result.getOrDefault(p, false));
                    if (isGranted && onGranted != null) {
                        onGranted.run();
                    }
                    if (!isGranted && onRejected != null) {
                        onRejected.rejected(this);
                    }
                }
        );

        locationPermissionRequest.launch(permissions.toArray(new String[0]));
    }

    private static final List<String> BLUETOOTH_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS = List.of(Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            BLUETOOTH_PERMISSIONS = List.of(Manifest.permission.BLUETOOTH_ADMIN);
        }
    }

    private static final List<String> NOTIFICATION_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NOTIFICATION_PERMISSIONS = List.of(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            NOTIFICATION_PERMISSIONS = Collections.emptyList();
        }
    }

    private static final List<String> ALL_PERMISSIONS;

    static {
        ArrayList<String> permissions = new ArrayList<>(BLUETOOTH_PERMISSIONS);
        permissions.addAll(NOTIFICATION_PERMISSIONS);

        ALL_PERMISSIONS = Collections.unmodifiableList(permissions);
    }
    public final static PermissionRequester BLUETOOTH = new PermissionRequester(BLUETOOTH_PERMISSIONS);
    public final static PermissionRequester NOTIFICATION = new PermissionRequester(NOTIFICATION_PERMISSIONS);

    public final static PermissionRequester ALL = new PermissionRequester(ALL_PERMISSIONS);

    public interface RejectedCallback {
        void rejected(PermissionRequester permissionRequester);
    }
}
