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

package com.drnoob.datamonitor.utils;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.data.OSSLibrary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class OSSLicense {

    public static List<OSSLibrary> getOSSLibraries() {
        List<OSSLibrary> libraries = new ArrayList<>();

        libraries.add(new OSSLibrary("androidx.appcompat:appcompat", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.6.1", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.google.android.material:material", "The Android Open Source Project",
                "Material Components for Android is a static library that you can add to your Android application in order to use APIs that provide implementations of the Material Design specification. Compatible on devices running API 14 or later.",
                "1.8.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.constraintlayout:constraintlayout", "The Android Open Source Project",
                "ConstraintLayout for Android",
                "2.1.4", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.coordinatorlayout:coordinatorlayout", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.1.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.customview:customview", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.1.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.cursoradapter:cursoradapter", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.0.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.documentfile:documentfile", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.0.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.drawerlayout:drawerlayout", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.1.1", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.viewpager:viewpager", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.0.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.viewpager2:viewpager2", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "2.1.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.databinding:viewbinding", "The Android Open Source Project",
                "",
                "4.2.2", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.legacy:legacy-support-v4", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later.",
                "1.0.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.navigation:navigation-fragment", "The Android Open Source Project",
                "Android Navigation-Fragment",
                "2.5.3", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.navigation:navigation-ui", "The Android Open Source Project",
                "Android Navigation-UI",
                "2.5.3", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.lifecycle:lifecycle-extensions", "The Android Open Source Project",
                "Android Lifecycle Extensions",
                "2.2.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.google.code.gson:gson", "Google",
                "A Java serialization/deserialization library to convert Java Objects into JSON and back",
                "2.9.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.preference:preference", "Google",
                "", "1.2.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.github.skydoves:progressview", "Skydoves",
                "A polished and flexible ProgressView, fully customizable with animations.",
                "1.1.2", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.airbnb.android:lottie", "Airbnb",
                "Render After Effects animations natively on Android and iOS, Web, and React Native",
                "5.2.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.github.antonKozyriatskyi:CircularProgressIndicator", "Anton Kozyriatskyi",
                "Customizable circular progress indicator",
                "1.3.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("fr.bmartel:jspeedtest", "Bertrand Martel",
                "JSpeedTest : speed test client library for Java/Android",
                "1.32.1", "MIT License", R.string.jspeedtest_license));

        libraries.add(new OSSLibrary("io.ipinfo:ipinfo-api", "Ipinfo",
                "", "2.1", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.squareup.okhttp3:okhttp", "Square",
                "HTTP is the way modern applications network. It’s how we exchange data & media. Doing HTTP efficiently makes your stuff load faster and saves bandwidth.",
                "4.9.3", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.activity:activity", "The Android Open Source Project",
                "Provides the base Activity subclass and the relevant hooks to build a composable structure on top.",
                "1.2.4", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.activity:activity-ktx", "The Android Open Source Project",
                "Kotlin extensions for 'activity' artifact",
                "1.2.3", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.annotation:annotation", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs.",
                "1.3.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.annotation:annotation-experimental", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs.",
                "1.1.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("android.core:core", "The Android Open Source Project",
                "The Support Library is a static library that you can add to your Android application in order to use APIs that are either not available for older platform versions or utility APIs that aren't a part of the framework APIs. Compatible on devices running API 14 or later",
                "1.7", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("android.core:core-ktx", "The Android Open Source Project",
                "Kotlin extensions for 'core' artifact",
                "1.2.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.kotlin:kotlin-stdlib", "Kotlin Team",
                "Kotlin Standard Library for JVM",
                "1.6.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.kotlin:kotlin-stdlib-common", "Kotlin Team",
                "Kotlin Common Standard Library",
                "1.6.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "Kotlin Team",
                "Kotlin Standard Library JDK 7 extension",
                "1.5.30", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.kotlin:kotlin-stdlib-jdk8", "Kotlin Team",
                "Kotlin Standard Library JDK 8 extension",
                "1.5.30", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-android", "JetBrains Team",
                "Coroutines support libraries for Kotlin",
                "1.5.2", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm", "JetBrains Team",
                "Coroutines support libraries for Kotlin",
                "1.5.2", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("org.jetbrains.annotations", "JetBrains Team",
                "",
                "13.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("Protobuf Nano", "Google",
                "Protocol Buffers (a.k.a., protobuf) are Google's language-neutral, platform-neutral, extensible mechanism for serializing structured data",
                "1.2.0", "BSD License", R.string.protobuf_license));

        libraries.add(new OSSLibrary("androidx.transition:transition", "The Android Open Source Project",
                "Android Transition Support Library",
                "1.4.1", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.recyclerview:recyclerview", "The Android Open Source Project",
                "Android Support RecyclerView v7",
                "1.1.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("androidx.core:core-splashscreen", "The Android Open Source Project",
                "Android core SplashScreen library",
                "1.0.0", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("com.android.volley:volley", "The Android Open Source Project",
                "Volley",
                "1.2.1", "Apache License 2.0", R.string.apache_license_2));

        libraries.add(new OSSLibrary("Glide", "Bumptech",
                "Glide is a fast and efficient open source media management and image loading framework for Android.",
                "1.2.1", "BSD, part MIT and Apache 2.0.", R.string.glide_license));

        Collections.sort(libraries, new Comparator<OSSLibrary>() {
            @Override
            public int compare(OSSLibrary ossLibrary, OSSLibrary t1) {
                return ossLibrary.getLibraryName().toLowerCase().compareTo(t1.getLibraryName().toLowerCase());
            }
        });

        return libraries;
    }
}
