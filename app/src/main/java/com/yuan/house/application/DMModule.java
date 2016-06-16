package com.yuan.house.application;


import com.avoscloud.chat.ui.chat.ChatRoomActivity;
import com.yuan.house.activities.CropActivity;
import com.yuan.house.activities.IPSettingActivity;
import com.yuan.house.activities.MainActivity;
import com.yuan.house.activities.MapActivity;
import com.yuan.house.activities.SplashActivity;
import com.yuan.house.activities.SwitchHouseActivity;
import com.yuan.house.activities.TutorialActivity;
import com.yuan.house.activities.WebViewActivity;
import com.yuan.house.activities.WebViewBasedActivity;
import com.yuan.house.helper.AuthHelper;
import com.yuan.house.http.WebService;
import com.yuan.house.ui.dialog.PopUpDialog;
import com.yuan.house.ui.fragment.AgencyMainFragment;
import com.yuan.house.ui.fragment.AgencyMessageFragment;
import com.yuan.house.ui.fragment.FragmentBBS;
import com.yuan.house.ui.fragment.LoginFragment;
import com.yuan.house.ui.fragment.ProposalFragment;
import com.yuan.house.ui.fragment.UserMainFragment;
import com.yuan.house.ui.fragment.UserMessageFragment;
import com.yuan.house.ui.fragment.WebViewBaseFragment;
import com.yuan.house.ui.fragment.WebViewFragment;

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

                        WebViewBaseFragment.class,
                        WebViewFragment.class,
                        UserMainFragment.class,
                        AgencyMainFragment.class,
                        AgencyMessageFragment.class,
                        UserMessageFragment.class,
                        LoginFragment.class,
                        ProposalFragment.class,
                        FragmentBBS.class,

                        WebService.class,

                        IPSettingActivity.class,
                        SplashActivity.class,
                        TutorialActivity.class,
                        MainActivity.class,
                        WebViewBasedActivity.class,
                        WebViewActivity.class,
                        ChatRoomActivity.class,
                        MapActivity.class,
                        SwitchHouseActivity.class,
                        CropActivity.class,

                        PopUpDialog.class
                },
                staticInjections = {
                        AuthHelper.class
                }

        )

public class DMModule {

}
