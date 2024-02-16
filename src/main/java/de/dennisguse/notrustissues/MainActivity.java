package de.dennisguse.notrustissues;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import de.dennisguse.notrustissues.databinding.MainActivityBinding;
import de.dennisguse.notrustissues.service.SensorService;
import de.dennisguse.notrustissues.util.PermissionRequester;

public class MainActivity extends AppCompatActivity {

    private MainActivityBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PermissionRequester.ALL.requestPermissionsIfNeeded(this, this, () -> {
        }, (requester) -> {
            Toast.makeText(this, R.string.permissions_not_granted, Toast.LENGTH_LONG).show();
            finish();
        });

        viewBinding = MainActivityBinding.inflate(getLayoutInflater());
        viewBinding.serviceStart.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                ContextCompat.startForegroundService(this, new Intent(this, SensorService.class));
            } else {
                stopService(new Intent(this, SensorService.class));
            }
        });

        setContentView(viewBinding.getRoot());
    }

    @Override
    protected void onResume() {
        super.onResume();

        //TODO Update viewBinding state to reflect if service is running or not.
        //viewBinding.serviceStart
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }
}
