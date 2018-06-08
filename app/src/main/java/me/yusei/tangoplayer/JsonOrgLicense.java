package me.yusei.tangoplayer;

import android.content.Context;

import de.psdev.licensesdialog.licenses.License;

/**
 * Created by yuseisako on 2018/06/05.
 */

public class JsonOrgLicense extends License{
    @Override
    public String getName() {
        return "JSON.org";
    }

    @Override
    public String readSummaryTextFromResources(final Context context) {
        return getContent(context, R.raw.json_org_license);
    }

    @Override
    public String readFullTextFromResources(final Context context) {
        return getContent(context, R.raw.json_org_license);
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getUrl() {
        return "https://www.json.org/license.html";
    }
}
