/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Monitor <https://github.com/itsdrnoob/DataMonitor>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.ui.activities;

import static com.drnoob.datamonitor.Common.isReadPhoneStateGranted;
import static com.drnoob.datamonitor.Common.isUsageAccessGranted;
import static com.drnoob.datamonitor.Common.setLanguage;
import static com.drnoob.datamonitor.core.Values.APP_COUNTRY_CODE;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_CODE;
import static com.drnoob.datamonitor.core.Values.READ_PHONE_STATE_DISABLED;
import static com.drnoob.datamonitor.core.Values.REQUEST_READ_PHONE_STATE;
import static com.drnoob.datamonitor.core.Values.SETUP_COMPLETED;
import static com.drnoob.datamonitor.core.Values.SETUP_VALUE;
import static com.drnoob.datamonitor.core.Values.USAGE_ACCESS_DISABLED;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.databinding.ActivitySetupBinding;
import com.drnoob.datamonitor.utils.CrashReporter;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

public class SetupActivity extends AppCompatActivity {
    private static final String TAG = SetupActivity.class.getSimpleName();

    com.drnoob.datamonitor.databinding.ActivitySetupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen.installSplashScreen(this);
        }
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new CrashReporter(SetupActivity.this));
        String languageCode = SharedPreferences.getUserPrefs(this).getString(APP_LANGUAGE_CODE, "null");
        String countryCode = SharedPreferences.getUserPrefs(this).getString(APP_COUNTRY_CODE, "");
        if (languageCode.equals("null")) {
            setLanguage(this, "en", countryCode);
        }
        else {
            setLanguage(this, languageCode, countryCode);
        }
        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setTheme(R.style.Theme_DataMonitor);
        setContentView(binding.getRoot());
        setSupportActionBar(binding.setupToolbar);
        getSupportActionBar().setTitle(null);
//        getWindow().setStatusBarColor(getColor(R.color.background));
//        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding.setupProgress.setSaveEnabled(true);
        binding.setupProgress.setProgressFromPrevious(true);
//        binding.appBar.setStatusBarForeground(MaterialShapeDrawable
//                .createWithElevationOverlay(SetupActivity.this));

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentStarted(@NonNull @NotNull FragmentManager fm, @NonNull @NotNull Fragment f) {
                super.onFragmentStarted(fm, f);
                if (f instanceof SetupWelcomeFragment) {
                    binding.setupProgress.setProgress(25);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                else if (f instanceof DisableBatteryOptimisationFragment) {
                    binding.setupProgress.setProgress(60);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
                else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        if (f instanceof RequestUsagePermissionFragment) {
                            binding.setupProgress.setProgress(80);
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                        else if (f instanceof RequestPhoneStatePermissionFragment) {
                            binding.setupProgress.setProgress(100);
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                    else {
                        if (f instanceof RequestUsagePermissionFragment) {
                            binding.setupProgress.setProgress(100);
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                }
            }
        }, true);

        int value = getIntent().getIntExtra(SETUP_VALUE, 0);
        if (value == USAGE_ACCESS_DISABLED ||
                SharedPreferences.getUserPrefs(this).getBoolean(SETUP_COMPLETED, false)) {
            binding.setupProgress.setVisibility(View.GONE);
        }
        else {
            binding.setupProgress.setVisibility(View.VISIBLE);
        }

        if (value == USAGE_ACCESS_DISABLED) {
            getSupportFragmentManager().beginTransaction().replace(R.id.setup_fragment_host,
                    new RequestUsagePermissionFragment()).commit();
        }
        else if (value == READ_PHONE_STATE_DISABLED) {
            getSupportFragmentManager().beginTransaction().replace(R.id.setup_fragment_host,
                    new RequestPhoneStatePermissionFragment()).commit();
        }
        else {
            if (SharedPreferences.getUserPrefs(this).getBoolean(SETUP_COMPLETED, false)) {
                try {
                    if (isUsageAccessGranted(this)) {

                    }
                    else {
                        getSupportFragmentManager().beginTransaction().replace(R.id.setup_fragment_host,
                                new RequestUsagePermissionFragment()).commit();
                    }
                    if (isReadPhoneStateGranted(this)) {

                    }
                    else {
                        getSupportFragmentManager().beginTransaction().replace(R.id.setup_fragment_host,
                                new RequestPhoneStatePermissionFragment()).commit();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                getSupportFragmentManager().beginTransaction().replace(R.id.setup_fragment_host, new SetupWelcomeFragment()).commit();
            }
        }

        if (SharedPreferences.getUserPrefs(this).getBoolean(SETUP_COMPLETED, false)) {
            try {
                if (isUsageAccessGranted(this)) {

                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (isUsageAccessGranted(this) && isReadPhoneStateGranted(this)) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            }
            else {
                if (isUsageAccessGranted(this)) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public static class SetupWelcomeFragment extends Fragment {
        public SetupWelcomeFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_welcome, container, false);

            TextView getStarted = view.findViewById(R.id.get_started);
            TextView skip = view.findViewById(R.id.skip_setup);

            getStarted.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                            R.anim.slide_in_left, R.anim.slide_out_right)
                            .replace(R.id.setup_fragment_host,
                                    new DisableBatteryOptimisationFragment()).addToBackStack("battery").commit();
                }
            });

            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        try {
                            if (isUsageAccessGranted(getContext())) {
                                if (isReadPhoneStateGranted(getContext())) {
                                    startActivity(new Intent(getContext(), MainActivity.class));
                                    getActivity().finish();
                                }
                                else {
                                    getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                                            R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                            .replace(R.id.setup_fragment_host,
                                                    new RequestPhoneStatePermissionFragment()).addToBackStack("usage_access").commit();
                                }
                            }
                            else {
                                getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                                        R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                        .replace(R.id.setup_fragment_host,
                                                new RequestUsagePermissionFragment()).addToBackStack("usage_access").commit();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            if (isUsageAccessGranted(getContext())) {
                                startActivity(new Intent(getContext(), MainActivity.class));
                                getActivity().finish();
                            }
                            else {
                                getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                        R.anim.slide_in_left, R.anim.slide_out_right)
                                        .replace(R.id.setup_fragment_host,
                                                new RequestUsagePermissionFragment()).addToBackStack("usage_access").commit();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            return view;
        }
    }

    public static class DisableBatteryOptimisationFragment extends Fragment {
        public DisableBatteryOptimisationFragment() {
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_disable_battery_optimisation, container, false);

            MaterialTextView disableBatteryOptimisation = view.findViewById(R.id.disable_battery_optimisation);
            MaterialTextView extraOptimisation = view.findViewById(R.id.extra_optimisation);
            MaterialTextView oemSettings = view.findViewById(R.id.oem_battery_settings);
            MaterialTextView oemSkinWarning = view.findViewById(R.id.oem_skin_warning);
            MaterialButton next = view.findViewById(R.id.next);

            if (getActivity() instanceof SetupActivity) {
                // Do nothing :)
            }
            else {
                next.setVisibility(View.GONE);
            }

            if (TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.code"))) {
                oemSkinWarning.setVisibility(View.GONE);
            }
            else {
                oemSkinWarning.setVisibility(View.VISIBLE);
                oemSkinWarning.setText(getContext().getString(R.string.oem_skin_warning, getContext().getString(R.string.oem_skin_miui)));
            }


            Intent OemSettingsIntent = new Intent("android.settings.APP_BATTERY_SETTINGS");
            Uri OemSettingsUri = Uri.fromParts("package", getContext().getPackageName(), null);
            OemSettingsIntent.setData(OemSettingsUri);
            OemSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (OemSettingsIntent.resolveActivity(getActivity().getPackageManager()) == null) {
                // No OEM Battery Settings Intent found
                oemSettings.setVisibility(View.GONE);
            }
            else {

            }

            String oem = Build.BRAND;

            extraOptimisation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = null;
                    if (oem.equalsIgnoreCase("android") || oem.equalsIgnoreCase("generic")) {
                        uri = Uri.parse("https://dontkillmyapp.com" + "/google");
                    }
                    else if (oem.equalsIgnoreCase("redmi")) {
                        uri = Uri.parse("https://dontkillmyapp.com" + "/xiaomi");
                    }
                    else {
                        uri = Uri.parse("https://dontkillmyapp.com" + "/" + oem.toLowerCase());
                    }

                    intent.setData(uri);
                    startActivity(intent);
                }
            });

            oemSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(OemSettingsIntent);
                }
            });

            disableBatteryOptimisation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });

            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        try {
                            if (isUsageAccessGranted(getContext())) {
                                if (isReadPhoneStateGranted(getContext())) {
                                    startActivity(new Intent(getContext(), MainActivity.class));
                                    getActivity().finish();
                                }
                                else {
                                    getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                                            R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                            .replace(R.id.setup_fragment_host,
                                                    new RequestPhoneStatePermissionFragment()).addToBackStack("usage_access").commit();
                                }
                            }
                            else {
                                getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                                        R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                        .replace(R.id.setup_fragment_host,
                                                new RequestUsagePermissionFragment()).addToBackStack("usage_access").commit();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            if (isUsageAccessGranted(getContext())) {
                                startActivity(new Intent(getContext(), MainActivity.class));
                                getActivity().finish();
                            }
                            else {
                                getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                        R.anim.slide_in_left, R.anim.slide_out_right)
                                        .replace(R.id.setup_fragment_host,
                                                new RequestUsagePermissionFragment()).addToBackStack("usage_access").commit();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            TextView disableBatteryOptimisation = getView().findViewById(R.id.disable_battery_optimisation);
            PowerManager powerManager = (PowerManager) getContext().getSystemService(POWER_SERVICE);
            if (powerManager.isIgnoringBatteryOptimizations(getContext().getPackageName())) {
                disableBatteryOptimisation.setEnabled(false);
                disableBatteryOptimisation.setText(R.string.battery_optimisation_disabled);
                disableBatteryOptimisation.setTextColor(getResources().getColor(R.color.text_secondary, null));
            }
        }

        public String getSystemProperty(String property) {
            String value = null;
            try {
                value = (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class)
                        .invoke(null, property);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return value;
        }
    }

    public static class RequestUsagePermissionFragment extends Fragment {
        MaterialButton next;
        MaterialButton btnPrimary;
        TextView btnSecondary;
        public RequestUsagePermissionFragment() {

        }

        @Override
        public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
//                try {
//                    if (isUsageAccessGranted(getContext())) {
//                        next.setVisibility(View.VISIBLE);
////                        if (!isReadPhoneStateGranted(getContext())) {
////                            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
////                                    R.anim.slide_in_left, R.anim.slide_out_right)
////                                    .replace(R.id.setup_fragment_host,
////                                            new RequestPhoneStatePermissionFragment()).addToBackStack("usage_access").commit();
////                        }
//                    }
//                } catch (PackageManager.NameNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
//            else {
//                try {
//                    if (isUsageAccessGranted(getContext())) {
//                        startActivity(new Intent(getContext(), MainActivity.class));
//                        getActivity().finish();
//                    }
//                } catch (PackageManager.NameNotFoundException e) {
//                    e.printStackTrace();
//                }
//            }
            onResume();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_request_usage_permission, container, false);

            btnPrimary = view.findViewById(R.id.btn_primary);
            btnSecondary = view.findViewById(R.id.btn_secondary);
            TextView previous = view.findViewById(R.id.previous);
            next = view.findViewById(R.id.next);
            FrameLayout footer = view.findViewById(R.id.usage_permission_footer);

            if (getActivity().getIntent().getIntExtra(SETUP_VALUE, 0) == USAGE_ACCESS_DISABLED ||
                    SharedPreferences.getUserPrefs(getContext()).getBoolean(SETUP_COMPLETED, false)) {
                footer.setVisibility(View.GONE);
            }
            else {
                footer.setVisibility(View.VISIBLE);
            }

            btnPrimary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });

            btnSecondary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });

            previous.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });

            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        try {
                            if (isUsageAccessGranted(getContext())) {
                                if (isReadPhoneStateGranted(getContext())) {
                                    startActivity(new Intent(getContext(), MainActivity.class));
                                    getActivity().finish();
                                }
                                else {
                                    getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                                            R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                            .replace(R.id.setup_fragment_host,
                                                    new RequestPhoneStatePermissionFragment()).addToBackStack("usage_access").commit();
                                }
                            }
                            else {
                                getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right,
                                        R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                                        .replace(R.id.setup_fragment_host,
                                                new RequestUsagePermissionFragment()).addToBackStack("usage_access").commit();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            if (isUsageAccessGranted(getContext())) {
                                startActivity(new Intent(getContext(), MainActivity.class));
                                getActivity().finish();
                            }
                            else {
                                getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                                        R.anim.slide_in_left, R.anim.slide_out_right)
                                        .replace(R.id.setup_fragment_host,
                                                new RequestUsagePermissionFragment()).addToBackStack("usage_access").commit();
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                try {
                    if (isUsageAccessGranted(getContext())) {
                        next.setVisibility(View.VISIBLE);
                        btnSecondary.setVisibility(View.GONE);
                        btnPrimary.setEnabled(false);
                        btnPrimary.setBackgroundResource(R.drawable.button_primary_default_disabled);
                        btnPrimary.setText(R.string.action_permission_granted);
                        btnPrimary.setTextColor(getResources().getColor(R.color.text_secondary, null));

//                        if (!isReadPhoneStateGranted(getContext())) {
//                            getActivity().getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
//                                    R.anim.slide_in_left, R.anim.slide_out_right)
//                                    .replace(R.id.setup_fragment_host,
//                                            new RequestPhoneStatePermissionFragment()).addToBackStack("usage_access").commit();
//                        }

                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    if (isUsageAccessGranted(getContext())) {
                        startActivity(new Intent(getContext(), MainActivity.class));
                        getActivity().finish();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static class RequestPhoneStatePermissionFragment extends Fragment {
        MaterialButton btnPrimary;
        TextView btnSecondary;
        MaterialButton next;
        public RequestPhoneStatePermissionFragment() {

        }

        @Override
        public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (isReadPhoneStateGranted(getContext())) {
                startActivity(new Intent(getContext(), MainActivity.class));
                getActivity().finish();
            }
            else {

            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_request_phone_state_permission, container, false);

            btnPrimary = view.findViewById(R.id.btn_primary);
            btnSecondary = view.findViewById(R.id.btn_secondary);
            TextView previous = view.findViewById(R.id.previous);
            next = view.findViewById(R.id.next);
            FrameLayout footer = view.findViewById(R.id.usage_permission_footer);

            if (getActivity().getIntent().getIntExtra(SETUP_VALUE, 0) == USAGE_ACCESS_DISABLED ||
                    SharedPreferences.getUserPrefs(getContext()).getBoolean(SETUP_COMPLETED, false)) {
                footer.setVisibility(View.GONE);
            }
            else {
                footer.setVisibility(View.VISIBLE);
            }

            btnPrimary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
                }
            });

            btnSecondary.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().finish();
                }
            });

            previous.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });

            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isReadPhoneStateGranted(getContext())) {
                        startActivity(new Intent(getContext(), MainActivity.class));
                        getActivity().finish();
                    }
                }
            });

            return view;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions, @NonNull @NotNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == REQUEST_READ_PHONE_STATE) {
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    onResume();
                }
                else {
                    Snackbar snackbar = Snackbar.make(getView(), R.string.error_permission_denied, Snackbar.LENGTH_SHORT);
                    FrameLayout footer = getView().findViewById(R.id.usage_permission_footer);
                    if (footer.getVisibility() == View.VISIBLE) {
                        snackbar.setAnchorView(R.id.usage_permission_footer);
                    }
                    snackbar.show();
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (isReadPhoneStateGranted(getContext())) {
                next.setVisibility(View.VISIBLE);
                btnSecondary.setVisibility(View.GONE);
                btnPrimary.setEnabled(false);
                btnPrimary.setBackgroundResource(R.drawable.button_primary_default_disabled);
                btnPrimary.setText(R.string.action_permission_granted);
                btnPrimary.setTextColor(getResources().getColor(R.color.text_secondary, null));
                startActivity(new Intent(getContext(), MainActivity.class));
                getActivity().finish();
            }
        }
    }
}