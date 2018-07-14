package me.yusei.tangoplayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.licenses.CreativeCommonsAttribution30Unported;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense20;
import de.psdev.licensesdialog.licenses.GnuGeneralPublicLicense30;
import de.psdev.licensesdialog.licenses.GnuLesserGeneralPublicLicense21;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.licenses.MozillaPublicLicense11;
import de.psdev.licensesdialog.licenses.MozillaPublicLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;

public class SettingsFragment extends PreferenceFragment {


    public static final int LICENSE_APACHE2 = 0;
    public static final int LICENSE_MOZILLA11 = 1;
    public static final int LICENSE_MOZILLA2 = 2;
    public static final int LICENSE_JSON = 3;
    public static final int LICENSE_GNU3 = 4;
    public static final int LICENSE_LGPL21 = 5;
    public static final int LICENSE_CC3 = 6;
    public static final int LICENSE_GPL20 = 7;
    public static final int LICENSE_MIT = 8;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_setting);
        Preference.OnPreferenceClickListener licenseClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, new AboutPreferenceFragment())
                        .addToBackStack(null).commit();
                return true;
            }
        };
        Preference license = findPreference(getResources().getString(R.string.key_license));
        license.setOnPreferenceClickListener(licenseClickListener);

        Preference.OnPreferenceClickListener sendFeedbackListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                String email = "profical" + "@" + "gmai.com";
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto",email, null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.email_subject));
                emailIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.email_content));
                startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.email_intent_title)));
                return true;
            }
        };
        Preference sendFeedback = findPreference(getResources().getString(R.string.key_send_feedback));
        sendFeedback.setOnPreferenceClickListener(sendFeedbackListener);

        Preference.OnPreferenceClickListener versionListener = new Preference.OnPreferenceClickListener() {
            int clickCounter = 0;
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(clickCounter++ > 10){
                    Toast.makeText(getActivity(), getResources().getString(R.string.language_connect), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        };
        Preference version = findPreference(getResources().getString(R.string.key_version));
        version.setOnPreferenceClickListener(versionListener);

//        Preference versionPref = findPreference( "version" );
//        try {
//            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
//            versionPref.setSummary(pInfo.versionName);
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((SettingsActivity)getActivity()).setActionBarTitle(getResources().getString(R.string.activity_settings));
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class AboutPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_licenses);
            setHasOptionsMenu(true);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ((SettingsActivity)getActivity()).setActionBarTitle(getResources().getString(R.string.pref_licenses));
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                             Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);
            if(preference.getTitle() == null){
                return false;
            }

            String title = preference.getKey();
            if(title.compareTo(getResources().getString(R.string.flowlayout)) == 0){
                showLicense(getResources().getString(R.string.flowlayout),
                        "https://github.com/nex3z/FlowLayout",
                        "Copyright 2016 nex3z", LICENSE_APACHE2);
            }else if(title.compareTo(getResources().getString(R.string.subtitle_converter)) == 0){
                    showLicense(getResources().getString(R.string.subtitle_converter),
                            "https://github.com/JDaren/subtitleConverter",
                            "Copyrigt (c) 2012 J. David REQUEJO", LICENSE_MIT);
            }else if(title.compareTo(getResources().getString(R.string.material_design_icon)) == 0){
                showLicense(getResources().getString(R.string.material_design_icon),
                        "http://www.google.com",
                        "Copyright 2018 Google", LICENSE_CC3);
            }else if(title.compareTo(getResources().getString(R.string.android_support_library)) == 0){
                showLicense(getResources().getString(R.string.android_support_library),
                        "http://developer.android.com/tools/extras/support-library.html",
                        "Copyright (c) 2005-2018, The Android Open Source Project", LICENSE_APACHE2);
            }else if(title.compareTo(getResources().getString(R.string.android_constraint_layout)) == 0){
                showLicense(getResources().getString(R.string.android_constraint_layout),
                        "http://tools.android.com", "Copyright 2018 Google", LICENSE_APACHE2);
            }else if(title.compareTo(getResources().getString(R.string.MaterialFilePicker)) == 0){
                showLicense(getResources().getString(R.string.MaterialFilePicker),
                        "https://github.com/nbsp-team/MaterialFilePicker",
                        "Copyright 2018 &nbsp;", LICENSE_APACHE2);
            }else if(title.compareTo(getResources().getString(R.string.juniversalchardet)) == 0){
                showLicense(getResources().getString(R.string.juniversalchardet),
                        "https://code.google.com/archive/p/juniversalchardet/",
                        "Copyright 2018 Mozilla", LICENSE_MOZILLA11);
            }else if(title.compareTo(getResources().getString(R.string.licenses_dialog)) == 0){
                showLicense(getResources().getString(R.string.licenses_dialog),
                        "https://github.com/PSDev/LicensesDialog",
                        "Copyright 2013-2017 Philip Schiffer", LICENSE_APACHE2);
            }else if(title.compareTo(getResources().getString(R.string.json_in_java)) == 0){
                showLicense(getResources().getString(R.string.json_in_java),
                        "https://github.com/douglascrockford/JSON-java",
                        "Copyright (c) 2002 JSON.org", LICENSE_JSON);
            }else if(title.compareTo(getResources().getString(R.string.ankidroid_api)) == 0){
                showLicense(getResources().getString(R.string.ankidroid_api),
                        "https://github.com/ankidroid/Anki-Android",
                        "Copyright (C) 2007 Free Software Foundation, Inc.", LICENSE_LGPL21);
            }else if(title.compareTo(getResources().getString(R.string.lib_vlc)) == 0){
                showLicense(getResources().getString(R.string.lib_vlc),
                        "https://github.com/videolan/vlc-android",
                        "Copyright (c) 2018 VideoLAN", LICENSE_LGPL21);
            }else if(title.compareTo(getResources().getString(R.string.material_show_case))==0){
                showLicense(getResources().getString(R.string.material_show_case),
                        "https://github.com/webianks/EasyFeedback",
                        "Copyright (c) 2015 Dean Wild", LICENSE_APACHE2);
            }else if(title.compareTo(getResources().getString(R.string.tango_player))==0){
                showLicense(getResources().getString(R.string.tango_player),
                        "https://github.com/yuseisako/TangoPlayer",
                        "Copyright (c) 2018 Yusei Sako", LICENSE_MIT);
            }

            return true;
        }

        private void showLicense(String name, String url, String copyright, int licenseType){
            if(name==null || url==null || copyright==null){
                return;
            }
            Notices notices = new Notices();
            switch (licenseType){
                case LICENSE_APACHE2:
                    notices.addNotice(new Notice(name, url, copyright, new ApacheSoftwareLicense20()));
                    break;
                case LICENSE_MIT:
                    notices.addNotice(new Notice(name, url, copyright, new MITLicense()));
                    break;
                case LICENSE_MOZILLA11:
                    notices.addNotice(new Notice(name, url, copyright, new MozillaPublicLicense11()));
                    break;
                case LICENSE_MOZILLA2:
                    notices.addNotice(new Notice(name, url, copyright, new MozillaPublicLicense20()));
                    break;
                case LICENSE_JSON:
                    notices.addNotice(new Notice(name, url, copyright, new JsonOrgLicense()));
                    break;
                case LICENSE_GNU3:
                    notices.addNotice(new Notice(name, url, copyright, new GnuGeneralPublicLicense30()));
                    break;
                case LICENSE_LGPL21:
                    notices.addNotice(new Notice(name, url, copyright, new GnuLesserGeneralPublicLicense21()));
                    break;
                case LICENSE_CC3:
                    notices.addNotice(new Notice(name, url, copyright, new CreativeCommonsAttribution30Unported()));
                    break;
                case LICENSE_GPL20:
                    notices.addNotice(new Notice(name, url, copyright, new GnuGeneralPublicLicense20()));
                    break;
            }

            new LicensesDialog.Builder(getActivity()).setNotices(notices).build().show();


        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

}