#### Android Gradle 插件示例

随着Android的发展，性能优化、组件化等等不少开发工作不得不在编译的过程中依赖对Java字节码的修改，这就要求我们学会基本的Gradle插件的实现。
但编写Android Gradle插件开始会遇到两个障碍，
1. Groovy语言
2. 官方Gradle插件的工作原理  

第一个障碍很好解决，因为我们完全可以使用Java编写，但是由于Gradle使用的Groovy，我们需要了解Groovy，以便阅读其它插件的源码，所以针对这一点，我们可以不用，但是要看得懂。
可以过一遍语法特性：https://www.ibm.com/developerworks/cn/education/java/j-groovy/j-groovy.html

第二个障碍不好解决，因为这方面的文档确实很少，需要自己去阅读源码，API Source的文档。但是不要被它吓到，这个毕竟是工具，我们只要会用就够了，所以我们看几个关键部分，理解它的工作流程就足够了。
拿到源码最快的方式是添加这个依赖
```implement 'com.android.tools.build:gradle-core:x.x.x'```
然后我们找到TaskManager.java类，关注createPostCompilationTasks这个函数。

```
/**
     * Creates the post-compilation tasks for the given Variant.
     *
     * These tasks create the dex file from the .class files, plus optional intermediary steps like
     * proguard and jacoco
     */
    public void createPostCompilationTasks(
            @NonNull TaskFactory tasks,
            @NonNull final VariantScope variantScope) {

        ...

        // ----- External Transforms -----
        // apply all the external transforms.
        List<Transform> customTransforms = extension.getTransforms();
        List<List<Object>> customTransformsDependencies = extension.getTransformsDependencies();

        for (int i = 0, count = customTransforms.size(); i < count; i++) {
            Transform transform = customTransforms.get(i);

            List<Object> deps = customTransformsDependencies.get(i);
            transformManager
                    .addTransform(tasks, variantScope, transform)
                    .ifPresent(t -> {
                        if (!deps.isEmpty()) {
                            t.dependsOn(tasks, deps);
                        }

                        // if the task is a no-op then we make assemble task depend on it.
                        if (transform.getScopes().isEmpty()) {
                            variantScope.getAssembleTask().dependsOn(tasks, t);
                        }
                    });
        }

        ...
    }
```  

首先我们需要知道，根据Android Moudule类型的不同，分为AppExtension和LibraryExtension，源码将编译过程中的一个个流程封装成若干个Transform，我们自定义插件就是为了在原来流程上添加我们自己的Transform。
例如，我在Application编译流程上添加一个流程，  

```
    public class MyPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AppExtension appExtension = (AppExtension) project.getExtensions().getByName("android");
        appExtension.registerTransform(new CustomTransform(project), Collections.EMPTY_LIST);
    }
}
```  

根据上述源码可知，自定义的Transform的工作位置在生成dex之前，生成class文件之后，这样，我们就可以在Transform里实现我们的字节码修改了。
