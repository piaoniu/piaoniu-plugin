package com.piaoniu.enumpub.service;

import com.piaoniu.enumpub.annotations.EnumPub;
import com.piaoniu.enumpub.util.ScanUtil;

import java.util.List;

public abstract class PublishManager {

    abstract void saveNames(String name,String simpleName);

    abstract String getRealName(String simpleName);

    abstract List<String> getSimpleNames();

    public void init(List<String> packageNames){
        packageNames.forEach(
                p -> {
                    ScanUtil.getClasses(p).stream()
                            .filter(c -> c.getAnnotation(EnumPub.class)!=null)
                            .forEach(c -> {
                                saveNames(c.getSimpleName(),c.getName());
                            });
                }
        );
    }
}
