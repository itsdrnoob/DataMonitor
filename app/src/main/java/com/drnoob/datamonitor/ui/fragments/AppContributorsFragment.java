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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.adapters.TranslatorAdapter;
import com.drnoob.datamonitor.adapters.data.TranslatorModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AppContributorsFragment extends Fragment {
    private LinearLayout mContribute;
    private RecyclerView mTranslatorsView;
    private List<TranslatorModel> translators;
    private TranslatorAdapter mAdapter;

    public AppContributorsFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        translators = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contributors, container, false);

        mContribute = view.findViewById(R.id.contribute);
        mTranslatorsView = view.findViewById(R.id.translator_view);

        if (translators.isEmpty()) {
            translators.addAll(getTranslators());
        }

        mTranslatorsView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new TranslatorAdapter(translators, requireActivity());
        mTranslatorsView.setAdapter(mAdapter);

        mContribute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(getString(R.string.github_contribute))));
            }
        });

        return view;
    }

    private List<TranslatorModel> getTranslators() {
        List<TranslatorModel> translators = new ArrayList<>();

        translators.add(new TranslatorModel(getContext().getString(R.string.ygorigor),
                getContext().getString(R.string.ygorigor_summary),
                R.drawable.ygorigor, true,
                getContext().getString(R.string.github_ygorigor)));

        translators.add(new TranslatorModel(getContext().getString(R.string.johnsonran),
                getContext().getString(R.string.johnsonran_summary),
                R.drawable.johnsonran, true,
                getContext().getString(R.string.github_johnsonran)));

        translators.add(new TranslatorModel(getContext().getString(R.string.cracky5322),
                getContext().getString(R.string.cracky5322_summary),
                R.drawable.cracky5322, true,
                getContext().getString(R.string.github_cracky5322)));

        translators.add(new TranslatorModel(getContext().getString(R.string.louis_leblanc),
                getContext().getString(R.string.louis_leblanc_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.hudaifa_abdullah),
                getContext().getString(R.string.hudaifa_abdullah_summary),
                R.drawable.hudaifa_abdullah, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.holydemon),
                getContext().getString(R.string.holydemon_summary),
                R.drawable.holydemon, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.liviogasp),
                getContext().getString(R.string.liviogasp_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.comradekingu),
                getContext().getString(R.string.comradekingu_translation_summary),
                R.drawable.comradekingu, true,
                getContext().getString(R.string.github_comradekingu)));

        translators.add(new TranslatorModel(getContext().getString(R.string.metezd),
                getContext().getString(R.string.metezd_summary),
                R.drawable.metezd, true,
                getContext().getString(R.string.github_metezd)));

        translators.add(new TranslatorModel(getContext().getString(R.string.ersen0),
                getContext().getString(R.string.ersen0_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_ersen0)));

        translators.add(new TranslatorModel(getContext().getString(R.string.eUgEntOptIc44),
                getContext().getString(R.string.eUgEntOptIc44_summary),
                R.drawable.eugentoptic44, true,
                getContext().getString(R.string.github_eUgEntOptIc44)));

        translators.add(new TranslatorModel(getContext().getString(R.string.AxusWizix),
                getContext().getString(R.string.AxusWizix_summary),
                R.drawable.axuswizix, true,
                getContext().getString(R.string.github_AxusWizix)));

        translators.add(new TranslatorModel(getContext().getString(R.string.hector_gamer),
                getContext().getString(R.string.hectorgamer_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.BLENICeI),
                getContext().getString(R.string.BLENICeI_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.Igor),
                getContext().getString(R.string.Igor_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.Denis),
                getContext().getString(R.string.Denis_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.friedrich),
                getContext().getString(R.string.friedrich_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.helmut),
                getContext().getString(R.string.helmut_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.kyoya),
                getContext().getString(R.string.kyoya_summary),
                R.drawable.kyoya, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.paul),
                getContext().getString(R.string.paul_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.tobias),
                getContext().getString(R.string.tobias_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.MarongHappy),
                getContext().getString(R.string.MarongHappy_summary),
                R.drawable.maronghappy, true,
                getContext().getString(R.string.github_MarongHappy)));

        translators.add(new TranslatorModel(getContext().getString(R.string.Atalanttore),
                getContext().getString(R.string.Atalanttore_summary),
                R.drawable.atalanttore, true,
                getContext().getString(R.string.github_Atalanttore)));

        translators.add(new TranslatorModel(getContext().getString(R.string.gallegonovato),
                getContext().getString(R.string.gallegonovato_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_gallegonovato)));

        translators.add(new TranslatorModel(getContext().getString(R.string.Kefir2105),
                getContext().getString(R.string.Kefir2105_summary),
                R.drawable.kefir2105, true,
                getContext().getString(R.string.github_Kefir2105)));

        translators.add(new TranslatorModel(getContext().getString(R.string.rex07),
                getContext().getString(R.string.rex07_summary),
                R.drawable.rex07, true,
                getContext().getString(R.string.github_rex07)));

        translators.add(new TranslatorModel(getContext().getString(R.string.shailendramaurya),
                getContext().getString(R.string.shailendramaurya_summary),
                R.drawable.shailendramaurya, true,
                getContext().getString(R.string.github_shailendramaurya)));

        translators.add(new TranslatorModel(getContext().getString(R.string.iqb4lsp),
                getContext().getString(R.string.iqb4lsp_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_iqb4lsp)));

        translators.add(new TranslatorModel(getContext().getString(R.string.Master2050),
                getContext().getString(R.string.Master2050_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_Master2050)));

        translators.add(new TranslatorModel(getContext().getString(R.string.bonbonboi),
                getContext().getString(R.string.bonbonboi_summary),
                R.drawable.bonbonboi, true,
                getContext().getString(R.string.github_bonbonboi)));

        translators.add(new TranslatorModel(getContext().getString(R.string.U1M450W),
                getContext().getString(R.string.U1M450W_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.atharvshinde),
                getContext().getString(R.string.atharvshinde_translation_summary),
                R.drawable.atharvshinde, true,
                getContext().getString(R.string.github_atharvshinde)));

        translators.add(new TranslatorModel(getContext().getString(R.string.graphixmedia),
                getContext().getString(R.string.graphixmedia_summary),
                R.drawable.graphixmedia, true,
                getContext().getString(R.string.github_graphixmedia)));

        translators.add(new TranslatorModel(getContext().getString(R.string.hinamechan),
                getContext().getString(R.string.hinamechan_summary),
                R.drawable.hinamechan, true,
                getContext().getString(R.string.github_hinamechan)));

        translators.add(new TranslatorModel(getContext().getString(R.string.tadekdudek),
                getContext().getString(R.string.tadekdudek_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_tadekdudek)));

        translators.add(new TranslatorModel(getContext().getString(R.string.fjuro),
                getContext().getString(R.string.fjuro_summary),
                R.drawable.fjuro, true,
                getContext().getString(R.string.github_fjuro)));

        translators.add(new TranslatorModel(getContext().getString(R.string.croxz900),
                getContext().getString(R.string.croxz900_summary),
                R.drawable.croxz900, true,
                getContext().getString(R.string.github_croxz900)));

        translators.add(new TranslatorModel(getContext().getString(R.string.ngocanhtve),
                getContext().getString(R.string.ngocanhtve_summary),
                R.drawable.ngocanhtve, true,
                getContext().getString(R.string.github_ngocanhtve)));

        translators.add(new TranslatorModel(getContext().getString(R.string.bluehomewu),
                getContext().getString(R.string.bluehomewu_summary),
                R.drawable.bluehomewu, true,
                getContext().getString(R.string.github_bluehomewu)));

        translators.add(new TranslatorModel(getContext().getString(R.string.rezaalmanda),
                getContext().getString(R.string.rezaalmanda_summary),
                R.drawable.rezaalmanda, true,
                getContext().getString(R.string.github_rezaalmanda)));

        translators.add(new TranslatorModel(getContext().getString(R.string.gnu_ewm),
                getContext().getString(R.string.gnu_ewm_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_gnu_ewm)));

        translators.add(new TranslatorModel(getContext().getString(R.string.rintan),
                getContext().getString(R.string.rintan_summary),
                R.drawable.rintan, true,
                getContext().getString(R.string.github_rintan)));

        translators.add(new TranslatorModel(getContext().getString(R.string.dan),
                getContext().getString(R.string.dan_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.jean_mareilles),
                getContext().getString(R.string.jean_mareilles_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.timtcg),
                getContext().getString(R.string.timtcg_summary),
                R.drawable.timtcg, true,
                getContext().getString(R.string.github_timtcg)));

        translators.add(new TranslatorModel(getContext().getString(R.string.zahiruddinrushdi),
                getContext().getString(R.string.zahiruddinrushdi_summary),
                R.drawable.zahiruddinrushdi, true,
                getContext().getString(R.string.github_zahiruddinRushdi)));

        translators.add(new TranslatorModel(getContext().getString(R.string.detrimental_god),
                getContext().getString(R.string.detrimental_god_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.chin_housin),
                getContext().getString(R.string.chin_housin_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.jonnysemon),
                getContext().getString(R.string.jonnysemon_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.flix),
                getContext().getString(R.string.flix_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.xxx),
                getContext().getString(R.string.xxx_summary),
                R.drawable.ic_person, false));

        translators.add(new TranslatorModel(getContext().getString(R.string.ofermar),
                getContext().getString(R.string.ofermar_summary),
                R.drawable.ersen0, true,
                getContext().getString(R.string.github_ofermar)));

        return translators;
    }

    public static class Contributors extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setPreferencesFromResource(R.xml.contributors_list, rootKey);
                }
            });
        }
    }
}
