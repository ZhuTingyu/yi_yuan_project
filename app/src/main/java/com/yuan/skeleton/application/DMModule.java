package com.yuan.skeleton.application;



import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.activities.MapActivity;
import com.yuan.skeleton.activities.SignInActivity;
import com.yuan.skeleton.activities.SplashActivity;
import com.yuan.skeleton.activities.TutorialActivity;
import com.yuan.skeleton.activities.WebViewActivity;
import com.yuan.skeleton.activities.WebViewBasedActivity;
import com.yuan.skeleton.ui.dialog.PopUpDialog;
import com.yuan.skeleton.ui.fragment.AgencyMainFragment;
import com.yuan.skeleton.ui.fragment.AgencyMessageFragment;
import com.yuan.skeleton.ui.fragment.LoginFragment;
import com.yuan.skeleton.ui.fragment.UserMainFragment;
import com.yuan.skeleton.ui.fragment.UserMessageFragment;
import com.yuan.skeleton.ui.fragment.UserProposalFragment;
import com.yuan.skeleton.ui.fragment.WebViewBaseFragment;
import com.yuan.skeleton.ui.fragment.WebViewFragment;
import com.yuan.skeleton.utils.JsonParse;

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

                        SplashActivity.class,
                        TutorialActivity.class,
                        MainActivity.class,
                        WebViewBasedActivity.class,
                        WebViewActivity.class,
                        SignInActivity.class,
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
