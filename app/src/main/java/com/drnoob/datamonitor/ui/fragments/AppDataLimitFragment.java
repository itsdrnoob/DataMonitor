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

package com.drnoob.datamonitor.ui.fragments;

import static com.drnoob.datamonitor.Common.dismissOnClick;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.AppDataLimitAdapter;
import com.drnoob.datamonitor.adapters.data.AppDataUsageModel;
import com.drnoob.datamonitor.core.task.DatabaseHandler;
import com.drnoob.datamonitor.utils.AppDataUsageMonitor;
import com.drnoob.datamonitor.utils.SharedPreferences;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppDataLimitFragment extends Fragment {
    private static final String TAG = AppDataLimitFragment.class.getSimpleName();
    private static List<AppDataUsageModel> mList = new ArrayList<>();
    private static List<AppDataUsageModel> mAppsList = new ArrayList<>();
    private static List<AppDataUsageModel> mSystemAppsList = new ArrayList<>();
    private static List<AppDataUsageModel> list = new ArrayList<>();
    private static List<AppDataUsageModel> toRemoveList = new ArrayList<>();
    private static RecyclerView mAppsView;
    private TextInputEditText mDataLimit;
    private TabLayout mDataType;
    private TabItem mDataTypeMB, mDataTypeGB;
    private FrameLayout mDeleteComfirm;
    private ItemTouchHelper itemTouchHelper;
    private ProgressBar mProgress;
    AppDataLimitAdapter mAdapter;
    AppDataLimitAdapter adapter;
    AppDataLimitAdapter searchAdapter;
    RecyclerView mListView;
    TextView mEmptyList;
    ExtendedFloatingActionButton mAddApp;
    LoadAppsData loadAppsData = new LoadAppsData();
    Type type = new TypeToken<List<AppDataUsageModel>>() {}.getType();

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_data_limit, container, false);

        mListView = view.findViewById(R.id.list);
        mEmptyList = view.findViewById(R.id.app_data_limit_empty_list);
        mAddApp = view.findViewById(R.id.add_app_fab);
        mDeleteComfirm = view.findViewById(R.id.delete_confirm);
        mProgress = view.findViewById(R.id.load_apps_progress);

        itemTouchHelper = null;

        DatabaseHandler handler = new DatabaseHandler(getContext());
        try {
            if (handler.getUsageList() != null && handler.getUsageList().size() > 0) {
                list = handler.getUsageList();
            }
//            else if (handler.getUsageList() != null && handler.getUsageList().size() > 0) {
//                list = handler.getUsageList();
//            }
            else {
                GetInstalledApplications getInstalledApplications = new GetInstalledApplications();
                getInstalledApplications.execute();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (mList.size() < 1) {
            loadAppsData.execute();
        }
        else {
            mProgress.setVisibility(View.GONE);
        }

//        for (int i = 0; i <= 20; i++) {
//            AppDataUsageModel model1 = new AppDataUsageModel();
//            model1.setAppName("Data Monitor");
//            model1.setPackageName("com.drnoob.datamonitor");
//            model1.setIsAppsList(false);
//            mList.add(model1);
//        }

        mAdapter = new AppDataLimitAdapter(mList, getContext());

        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));

        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                if (dy != 0) {
                    if (mDeleteComfirm.getVisibility() == View.VISIBLE) {
                        mDeleteComfirm.setVisibility(View.GONE);
                    }
                }
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        String s = SharedPreferences.getAppDataLimitPrefs(getContext()).getString("monitor_apps_list", null);
        Gson gson = new Gson();
        List<AppDataUsageModel> l = gson.fromJson(s, type);
        if (mList.size() > 0) {
            getContext().startService(new Intent(getContext(), AppDataUsageMonitor.class));
            Log.e(TAG, "onCreateView: started " );
        }

        mListView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                mListView.setBackgroundColor(getResources().getColor(R.color.warning, null));
//                Log.e(TAG, "onScrollChange: " + scrollY );
                if (oldScrollY < -15 && mAddApp.isExtended()) {
                    mAddApp.shrink();
                }
                else if (oldScrollY > 15 && !mAddApp.isExtended()) {
                    mAddApp.extend();
                }
                else if (mListView.computeVerticalScrollOffset() == 0 && !mAddApp.isExtended()) {
                    mAddApp.extend();
                }
            }
        });

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            Drawable background;
            Drawable xMark;
            int xMarkMargin;
            boolean initiated;
            Boolean isUndoPressed = false;
            Boolean vibrate = true;
            Boolean isSwipable = true;
            Boolean isSnackBarShowing = false;
            Float oldDx = 0F;
            private Paint paint = new Paint();
            Snackbar snackbar;
//            Boolean isCurrentlyActive = false;

            private void init() {
                background = new ColorDrawable(Color.RED);
                xMark = ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_close_24);
                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                xMarkMargin = (int) getContext().getResources().getDimension(R.dimen.margin_small);
                initiated = true;
            }

            @Override
            public boolean onMove(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, @NonNull @NotNull RecyclerView.ViewHolder target) {
                Toast.makeText(getContext(), "on Move", Toast.LENGTH_SHORT).show();
                return false;
            }

            @Override
            public void onSwiped(@NonNull @NotNull RecyclerView.ViewHolder viewHolder, int direction) {
                Log.e(TAG, "onSwiped: " + direction);
                vibrate = true;
                isSwipable = false;
                isSnackBarShowing = true;
                oldDx = 0F;
                final int position = viewHolder.getAdapterPosition();
                final AppDataUsageModel swipedModel = mList.get(position);
                float orig = mAddApp.getTranslationY();
                snackbar = Snackbar.make(view, swipedModel.getAppName() +
                        " has been removed from list", Snackbar.LENGTH_SHORT);
                SpannableStringBuilder buildetTextRight = new SpannableStringBuilder();
                buildetTextRight.append(" ");
                buildetTextRight.setSpan(new ImageSpan(getContext(), R.drawable.ic_baseline_close_24),
                        buildetTextRight.length()-1, buildetTextRight.length(), 0);
                buildetTextRight.append(" Undo");
                Drawable drawable = getResources().getDrawable(R.drawable.ic_baseline_close_24, null);
                drawable.setTint(Color.RED);
                drawable.setTintMode(PorterDuff.Mode.SRC_ATOP);
                Button action = snackbar.getView().findViewById(R.id.snackbar_action);
//                action.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_close_24, 0, 0, 0);
//                action.setCompoundDrawablePadding(getResources().getDimensionPixelOffset(R.dimen.margin_small));
//                action.setGravity(Gravity.CENTER);
//                action.setPadding(0, 0, 0, 25);
                snackbar.setAction("Undo", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isUndoPressed = true;
                        mListView.getAdapter().notifyItemChanged(position);
                        mList.add(position, swipedModel);
                        mAdapter.notifyItemInserted(position);
                        mListView.scrollToPosition(position);
                        if (mEmptyList.getVisibility() == View.VISIBLE) {
                            mEmptyList.setVisibility(View.GONE);
                        }
                    }
                });
                action.setTextColor(getContext().getColor(R.color.primary));
                action.setCompoundDrawables(getResources().getDrawable(R.drawable.ic_baseline_close_24, null),
                        null, null, null);

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        if (!isSnackBarShowing) {
                            mAddApp.animate().translationY(0).setDuration(200);
                        }
//                        mAddApp.animate().translationY(0);
                        isSnackBarShowing = false;
                        if (!isUndoPressed) {
                            toRemoveList.add(swipedModel);
                            try {
                                SharedPreferences.getAppDataLimitPrefs(getActivity()).edit().remove(swipedModel.getPackageName()).apply();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            toRemoveList.remove(swipedModel);
                        }
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!snackbar.isShown()) {
                                    mAddApp.animate().translationY(0).setDuration(200);
                                    if (mList.size() < 1) {
                                        mEmptyList.setVisibility(View.VISIBLE);
                                    }
                                }
                            }
                        }, 100);
                        super.onDismissed(transientBottomBar, event);
                    }

                    @Override
                    public void onShown(Snackbar sb) {
                        float translation = snackbar.getView().getBottom() - snackbar.getView().getTop() + 20;
                        mAddApp.animate().translationY(translation * -1).setDuration(200);
                        if (isSnackBarShowing) {
//                            mAddApp.animate().translationY(translation * -1);
                        }
                        Log.e(TAG, "onShown: " + mAddApp.getTranslationY() );
                        super.onShown(sb);
                    }
                });
//                TextView delete = mDeleteComfirm.findViewById(R.id.delete);
//                TextView undo = mDeleteComfirm.findViewById(R.id.undo);

//                delete.animate().alpha(1.0f);
//                undo.animate().alpha(1.0f);
//
//                undo.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Toast.makeText(getContext(), "undo", Toast.LENGTH_SHORT).show();
//                    }
//                });

                dismissOnClick(snackbar);
//                itemTouchHelper.attachToRecyclerView(mListView);
                mList.remove(position);
                mAdapter.notifyItemRemoved(position);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mDeleteComfirm.setVisibility(View.GONE);
                        isSwipable = true;
                    }
                }, 500);
                toRemoveList.add(swipedModel);
//                itemTouchHelper.attachToRecyclerView(null);

                snackbar.show();
            }

            @Override
            public int getMovementFlags(@NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder) {
                if (isSwipable) {
                    return super.getMovementFlags(recyclerView, viewHolder);
                }
                else {
                    return 0;
                }
            }

            @Override
            public void onChildDraw(@NonNull @NotNull Canvas c, @NonNull @NotNull RecyclerView recyclerView, @NonNull @NotNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
//                isCurrentlyActive = true;
                View itemView = viewHolder.itemView;
                mListView.setBackgroundColor(Color.TRANSPARENT);
                mDeleteComfirm.setY(itemView.getTop());

//                Log.e(TAG, "onChildDraw: " + itemView.getTranslationX() + " " + dX );
//
//                TextView delete = mDeleteComfirm.findViewById(R.id.delete);
//                TextView undo = mDeleteComfirm.findViewById(R.id.undo);
                ImageView imageView = mDeleteComfirm.findViewById(R.id.img_delete);

                imageView.setTranslationX(dX / 5);
//                imageView.animate().scaleXBy(dX / 5).scaleYBy(dX / 5);
//                imageView.setScaleY(dX * 0.02f);

//                DisplayMetrics metrics = new DisplayMetrics();
//                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
//                if (Math.abs(new Float(dX).intValue()) == (metrics.widthPixels - 100) / 2) {
////                        Toast.makeText(getContext(), "f", Toast.LENGTH_SHORT).show();
//                    Log.e(TAG, "onChildDraw: "  );
//                }
//
                if (isCurrentlyActive) {
//                    mDeleteComfirm.setAlpha(1);
                    mDeleteComfirm.setVisibility(View.VISIBLE);

                    if (Math.abs(new Float(dX).intValue()) >= (c.getWidth() / 2)) {
                        Log.e(TAG, "onChildDraw: " + dX + "  " + oldDx );
                        if (dX < oldDx) {
                            // Sliding back
//                            vibrate = true;
                        }
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_delete_24, null));
//                        if (vibrate) {
//                            oldDx = dX;
//                            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
//                            long[] VIBRATE_PATTERN = {20};
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
//                            }
//                            else {
//                                vibrator.vibrate(VIBRATE_PATTERN, 0);
//                            }
//                            vibrate = false;
//                        }

                    }
                    else if (Math.abs(new Float(dX).intValue()) <= (c.getWidth() / 2) - 50) {
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_trail_arrow_left, null));
                        if (dX > oldDx) {
                            // Sliding back
//                            vibrate = true;
                        }
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_trail_arrow_left, null));
//                        if (vibrate) {
//                            oldDx = dX;
//                            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
//                            long[] VIBRATE_PATTERN = {20};
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
//                            }
//                            else {
//                                vibrator.vibrate(VIBRATE_PATTERN, 0);
//                            }
//                            vibrate = false;
//                        }
                    }

                    if (Math.abs(new Float(dX).intValue()) >= (c.getWidth()) / 2) {
//                        Toast.makeText(getContext(), "f", Toast.LENGTH_SHORT).show();
//                        Log.e(TAG, "onChildDraw: "  );
                        if (oldDx == 0F) {
                            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                            long[] VIBRATE_PATTERN = {20};
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                            }
                            else {
                                vibrator.vibrate(VIBRATE_PATTERN, 0);
                            }
                            vibrate = false;
                            oldDx = dX * -1;
                        }

                    }

//                        if (Math.abs(new Float(dX).intValue()) <= (c.getWidth() / 2) - 50) {
//                                Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
//                                long[] VIBRATE_PATTERN = {20};
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
//                            }
//                            else {
//                                vibrator.vibrate(VIBRATE_PATTERN, 0);
//                            }
//                            vibrate = true;
//                        }

                    if (Math.abs(new Float(dX).intValue()) <= (c.getWidth() / 2) - 50) {
                        Log.e(TAG, "onChildDraw: " + dX + " " + oldDx );
                        if ((dX * -1) < oldDx) {
                            Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                            long[] VIBRATE_PATTERN = {20};
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                            }
                            else {
                                vibrator.vibrate(VIBRATE_PATTERN, 0);
                            }
                            vibrate = false;
                            oldDx = 0F;
                        }
                    }


//                    imageView.animate().scaleX(1.5f);
//                    imageView.animate().scaleY(1.5f);
                }
                else {
                    if (dX == 0) {
                        oldDx = 0F;
                        mDeleteComfirm.setVisibility(View.GONE);
                        vibrate = true;
                    }

                }
//
//                imageView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Toast.makeText(getContext(), "f", Toast.LENGTH_SHORT).show();
//                    }
//                });

//
//                // not sure why, but this method get's called for viewholder that are already swiped away
//                if (viewHolder.getAdapterPosition() == -1) {
//                    // not interested in those
//                    return;
//                }
//
//                if (!initiated) {
//                    init();
//                }
//
//                // draw red background
//                background.setBounds(itemView.getRight() - 100 + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
//                background.draw(c);
//
//                // draw x mark
//                int itemHeight = itemView.getBottom() - itemView.getTop();
//                int intrinsicWidth = xMark.getIntrinsicWidth();
//                int intrinsicHeight = xMark.getIntrinsicWidth();
//
//                int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
//                int xMarkRight = itemView.getRight() - xMarkMargin;
//                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
//                int xMarkBottom = xMarkTop + intrinsicHeight;
//                xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

//                xMark.draw(c);
                Float newDx = dX;
                if (newDx <= -250f) {
                    newDx = -250f;
                }

//                Collections.sort(list, new Comparator<AppDataUsageModel>() {
//                    @Override
//                    public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
//                        return o1.getUid();
//                    }
//                });


//                xMark = ContextCompat.getDrawable(getContext(), R.drawable.ic_baseline_close_24);
//                xMark.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
//                xMarkMargin = (int) getContext().getResources().getDimension(R.dimen.margin_small);
//
//                float translationX = dX;
////                View itemView = viewHolder.itemView;
//                float height = (float)itemView.getBottom() - (float)itemView.getTop();
//
//                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX <= 0) // Swiping Left
//                {
//                    translationX = -Math.min(-dX, height * 2);
//
//                    paint.setColor(Color.RED);
//                    RectF background = new RectF((float)itemView.getRight() + dX - 100 ,(float)itemView.getTop(), (float)itemView.getRight(), (float)itemView.getBottom());
//                    c.drawRoundRect(background, 45, 45,  paint);
//
//                    int itemHeight = itemView.getBottom() - itemView.getTop();
//                    int intrinsicWidth = xMark.getIntrinsicWidth();
//                    int intrinsicHeight = xMark.getIntrinsicHeight();
//
//                    int xMarkLeft = itemView.getRight() - xMarkMargin - intrinsicWidth;
//                    int xMarkRight = itemView.getRight() - xMarkMargin;
//                    int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
//                    int xMarkBottom = xMarkTop + intrinsicHeight;
//                    xMark.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
//                    xMark.draw(c);


//                    viewHolder.itemView.setTranslationX(translationX);
//                }
//                else if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX > 0) // Swiping Right
//                {
//                    translationX = Math.min(dX, height * 2);
//                    paint.setColor(Color.RED);
//                    RectF background = new RectF((float)itemView.getRight() + 100, (float)itemView.getTop(), (float)itemView.getRight(), (float)itemView.getBottom());
//                    c.drawRect(background, paint);
//                }

                super.onChildDraw(c, recyclerView, viewHolder, dX , dY, actionState, isCurrentlyActive);
            }


        };

        itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mListView);

        mAddApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_app_data_limit_add, null);

                LinearLayout appPicker = dialogView.findViewById(R.id.app_picker);
                CardView appIconView = dialogView.findViewById(R.id.app_icon_view);
                ImageView appIcon = appPicker.findViewById(R.id.app_icon);
                TextView appName = appPicker.findViewById(R.id.app_name);
                TextInputEditText dataLimitInput = dialogView.findViewById(R.id.data_limit);
                TabLayout dataTypeSwitcher = dialogView.findViewById(R.id.data_type_switcher);
                ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                TextView cancel = footer.findViewById(R.id.cancel);
                TextView ok = footer.findViewById(R.id.ok);
                mDataType = dialogView.findViewById(R.id.data_type_switcher);
                mDataTypeMB = dialogView.findViewById(R.id.data_type_mb);
                mDataTypeGB = dialogView.findViewById(R.id.data_type_gb);

                appPicker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_apps, null);

                        mProgress = dialogView.findViewById(R.id.apps_progress);
                        mAppsView = dialogView.findViewById(R.id.apps_view);
                        TextInputEditText search = dialogView.findViewById(R.id.search_app);
                        TabLayout appTypeSwitcher = dialogView.findViewById(R.id.app_type_switcher);

                        if (mAppsList.size() < 1 || mSystemAppsList.size() < 1) {
                            AppDataUsageModel model = null;
                            for (int i = 0; i < list.size(); i++) {
                                model = list.get(i);
                                model.setIsAppsList(true);
                                if (!model.isSystemApp()) {
                                    mAppsList.add(model);
                                } else {
                                    mSystemAppsList.add(model);
                                }
                            }
                        }

                        Collections.sort(mAppsList, new Comparator<AppDataUsageModel>() {
                            @Override
                            public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                                return o1.getAppName().compareTo(o2.getAppName());
                            }
                        });

                        Collections.sort(mSystemAppsList, new Comparator<AppDataUsageModel>() {
                            @Override
                            public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                                return o1.getAppName().compareTo(o2.getAppName());
                            }
                        });

                        adapter = new AppDataLimitAdapter(mAppsList, getContext());
                        adapter.setDialog(dialog);
                        mAppsView.setAdapter(adapter);
                        mAppsView.setLayoutManager(new LinearLayoutManager(getContext()));

                        appTypeSwitcher.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                            @Override
                            public void onTabSelected(TabLayout.Tab tab) {
                                search.setText("");
                                search.clearFocus();
                                if (list.size() > 1) {
                                    if (tab.getPosition() == 0) {
                                        adapter = new AppDataLimitAdapter(mAppsList, getContext());
                                    } else {
                                        adapter = new AppDataLimitAdapter(mSystemAppsList, getContext());
                                    }
                                }
                                mAppsView.setAdapter(adapter);
                                mAppsView.setLayoutManager(new LinearLayoutManager(getContext()));
                            }

                            @Override
                            public void onTabUnselected(TabLayout.Tab tab) {

                            }

                            @Override
                            public void onTabReselected(TabLayout.Tab tab) {

                            }
                        });

                        search.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                List<AppDataUsageModel> searchList = new ArrayList<>();
                                if (s.length() > 0) {
                                    for (int i = 0; i < list.size(); i++) {
                                        AppDataUsageModel model = list.get(i);
                                        if (model.getAppName().toLowerCase().contains(s.toString().toLowerCase())) {
                                            model.setIsAppsList(true);
                                            searchList.add(model);
                                        }
                                    }
                                    searchAdapter = new AppDataLimitAdapter(searchList, getContext());
                                    searchAdapter.setDialog(dialog);
                                    mAppsView.setAdapter(searchAdapter);
                                } else {
                                    mAppsView.setAdapter(adapter);
                                }
                                mAppsView.setLayoutManager(new LinearLayoutManager(getContext()));
                            }

                            @Override
                            public void afterTextChanged(Editable s) {

                            }
                        });

                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                AppDataUsageModel selectedModel;
                                if (searchAdapter != null) {
                                    selectedModel = searchAdapter.getSelectedAppModel();
                                }
                                else {
                                    selectedModel = adapter.getSelectedAppModel();
                                }
                                if (selectedModel != null) {
                                    try {
                                        appIcon.setImageDrawable(getContext().getPackageManager().getApplicationIcon(selectedModel.getPackageName()));
                                    } catch (PackageManager.NameNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    appIconView.setVisibility(View.VISIBLE);
                                    appName.setText(selectedModel.getAppName());
                                    if (SharedPreferences.getAppDataLimitPrefs(getContext()).getString(selectedModel.getPackageName(), null) != null) {

                                    }
                                }
                                else {

                                }
                            }
                        });

                        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
                        dialog.getBehavior().setSkipCollapsed(true);
                        dialog.setContentView(dialogView);
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.show();
                    }
                });

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AppDataUsageModel selectedModel = adapter.getSelectedAppModel();
                        if (selectedModel == null) {
                            selectedModel = searchAdapter.getSelectedAppModel();
                        }
                        InputMethodManager manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (SharedPreferences.getAppDataLimitPrefs(getContext()).getString(selectedModel.getPackageName(), null) != null) {
//                            dialog.dismiss();
                            Snackbar snackbar = Snackbar.make(dialog.getWindow().getDecorView(), "App is already added", Snackbar.LENGTH_LONG);
                            manager.hideSoftInputFromWindow(dialog.getWindow().getDecorView().getWindowToken(), 0);
                            dismissOnClick(snackbar);
                            snackbar.show();
                        }
                        else {
                            if (selectedModel != null) {
                                if (dataLimitInput.getText().toString().length() <= 0) {
                                    dataLimitInput.setBackground(getResources().getDrawable(R.drawable.text_input_error_background, null));
                                    dataLimitInput.requestFocus();
                                    manager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                                }
                                else {
                                    Log.e(TAG, "onClick: " );
                                    Float dataLimit = Float.parseFloat(dataLimitInput.getText().toString());
                                    int dataType;
                                    if (dataTypeSwitcher.getTabAt(0).isSelected()) {
                                        if (dataLimit >= 1024) {
                                            dataType = 1;
                                            dataLimit = dataLimit / 1024;
                                        } else {
                                            dataLimit = dataLimit;
                                            dataType = dataTypeSwitcher.getSelectedTabPosition();
                                        }
                                    } else {
                                        dataLimit = dataLimit * 1024f;
                                        dataType = dataTypeSwitcher.getSelectedTabPosition();
                                    }

                                    String[] data = new String[] {dataLimit.toString(), dataLimitInput.getText().toString(), String.valueOf(dataType)};

                                    Gson gson = new Gson();

                                    String jsonText = gson.toJson(data);

                                    SharedPreferences.getAppDataLimitPrefs(getContext()).edit()
                                            .putString(selectedModel.getPackageName(), jsonText).apply();


                                    dialog.dismiss();

                                    selectedModel.setDataLimit(data[1]);
                                    selectedModel.setDataType(data[2]);
                                    selectedModel.setIsAppsList(false);
                                    mList.add(selectedModel);
                                    Collections.sort(mList, new Comparator<AppDataUsageModel>() {
                                        @Override
                                        public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                                            return o1.getAppName().compareTo(o2.getAppName());
                                        }
                                    });
                                    mAdapter.notifyItemInserted(mList.indexOf(selectedModel));
                                    mEmptyList.setVisibility(View.GONE);
//                                    Set<AppDataUsageModel> set = new HashSet<>();
//                                    set.addAll(mList);
                                    String monitorAppsList = gson.toJson(mList, type);
                                    SharedPreferences.getAppDataLimitPrefs(getContext()).edit()
                                            .putString("monitor_apps_list", monitorAppsList).apply();
                                }

//                                DatabaseHandler handler = new DatabaseHandler(getContext());
//                                handler.createAppDataMonitorList(mAdapter.getSelectedAppModel());

                            }
                        }

                    }
                });

                dialog.setContentView(dialogView);
                dialog.show();
            }
        });

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (loadAppsData.getStatus() == AsyncTask.Status.RUNNING
                || loadAppsData.getStatus() == AsyncTask.Status.PENDING) {
            loadAppsData.cancel(true);
        }
        for (int i = 0; i < toRemoveList.size(); i++) {
            Log.e(TAG, "onPause: " + toRemoveList.get(i).getAppName() );
            SharedPreferences.getAppDataLimitPrefs(getActivity()).edit().remove(toRemoveList.get(i).getPackageName()).apply();
        }
        toRemoveList.clear();
    }

    public class GetInstalledApplications extends AsyncTask<Object, Integer, Object> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            PackageManager manager = getContext().getPackageManager();
            List<ApplicationInfo> apps = manager.getInstalledApplications(PackageManager.GET_META_DATA);
            AppDataUsageModel model = null;
            List<AppDataUsageModel> list = new ArrayList<>();
            for (ApplicationInfo info : apps) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 1) {
                    // System Apps
                    mSystemAppsList.add(new AppDataUsageModel(manager.getApplicationLabel(info).toString(),
                            info.packageName,
                            info.uid,
                            true,
                            true));
                }
                else {
                    // User Apps
                    mAppsList.add(new AppDataUsageModel(manager.getApplicationLabel(info).toString(),
                            info.packageName,
                            info.uid,
                            true,
                            true));
                }

                Collections.sort(mAppsList, new Comparator<AppDataUsageModel>() {
                    @Override
                    public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                        return o1.getAppName().compareTo(o2.getAppName());
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }

    private class LoadAppsData extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            AppDataUsageModel model;
            mList.clear();
            for (int i = 0; i < list.size(); i++) {
                String jsonText = SharedPreferences.getAppDataLimitPrefs(getContext()).getString(list.get(i).getPackageName(), null);
                if (jsonText != null) {
                    Gson gson = new Gson();
                    String[] data = gson.fromJson(jsonText, String[].class);
                    model = list.get(i);
                    model.setDataLimit(data[1]);
                    model.setDataType(data[2]);
                    model.setIsAppsList(false);
                    mList.add(model);
                }
            }

            Collections.sort(mList, new Comparator<AppDataUsageModel>() {
                @Override
                public int compare(AppDataUsageModel o1, AppDataUsageModel o2) {
                    return o1.getAppName().compareTo(o2.getAppName());
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            mAdapter.notifyDataSetChanged();
            mProgress.setVisibility(View.GONE);
            if (mList.size() < 1) {
                mEmptyList.setVisibility(View.VISIBLE);
            }
            else {
                mEmptyList.setVisibility(View.GONE);
            }
        }
    }
}
