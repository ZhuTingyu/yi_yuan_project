package com.yuan.house.application;



import com.yuan.house.activities.IPSettingActivity;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.activities.MapActivity;
import com.yuan.house.activities.SplashActivity;
import com.yuan.house.activities.TutorialActivity;
import com.yuan.house.activities.WebViewActivity;
import com.yuan.house.activities.WebViewBasedActivity;
import com.yuan.house.ui.dialog.PopUpDialog;
import com.yuan.house.ui.fragment.AgencyMainFragment;
import com.yuan.house.ui.fragment.AgencyMessageFragment;
import com.yuan.house.ui.fragment.LoginFragment;
import com.yuan.house.ui.fragment.UserMainFragment;
import com.yuan.house.ui.fragment.UserMessageFragment;
import com.yuan.house.ui.fragment.UserProposalFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;
import com.yuan.house.utils.JsonParse;

import dagger.Module;

/**
 * Dagger module for setting up provides statements.
 * Register all of your entry points below.
 */
@Module
        (
                complete = false,

                injects = {
                        DMApplication.class,
                        IPSettingActivity.class,
                        SplashActivity.class,
                        TutorialActivity.class,
                        MainActivity.class,
                        WebViewBasedActivity.class,
                        WebViewActivity.class,

                        MapActivity.class,

                        WebViewBaseFragment.class,
                        WebViewFragment.class,
                        UserMainFragment.class,
                        AgencyMainFragment.class,
                        AgencyMessageFragment.class,
                        UserMessageFragment.class,
                        LoginFragment.class,
                        UserProposalFragment.class,

                        PopUpDialog.class,
                        JsonParse.class
                }

        )

public class DMModule {

}
