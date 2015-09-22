package com.yuan.skeleton.application;



import com.yuan.skeleton.activities.MainActivity;
import com.yuan.skeleton.activities.SignInActivity;
import com.yuan.skeleton.activities.SplashActivity;
import com.yuan.skeleton.activities.TutorialActivity;
import com.yuan.skeleton.activities.WebViewActivity;
import com.yuan.skeleton.activities.WebViewBasedActivity;
import com.yuan.skeleton.ui.dialog.PopUpDialog;
import com.yuan.skeleton.ui.fragment.WebViewBaseFragment;
import com.yuan.skeleton.ui.fragment.WebViewFragment;

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

                        WebViewBaseFragment.class,
                        WebViewFragment.class,

                        PopUpDialog.class
                }

        )

public class DMModule {

}
