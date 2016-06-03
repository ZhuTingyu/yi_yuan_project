package com.yuan.house.application;

import dagger.Module;

/**
 * Add all the other modules to this one.
 */
@Module
        (
                includes = {
                        AndroidModule.class,
                        DMModule.class
                }
        )
public class RootModule {
}
