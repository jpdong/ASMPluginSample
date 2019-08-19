package com.dong.myplugin;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Collections;

/**
 * Created by dongjiangpeng on 2019/8/16 0016.
 */
public class MyPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AppExtension appExtension = (AppExtension) project.getExtensions().getByName("android");
        appExtension.registerTransform(new CustomTransform(project), Collections.EMPTY_LIST);
    }
}
