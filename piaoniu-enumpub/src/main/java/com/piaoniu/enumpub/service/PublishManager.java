package com.piaoniu.enumpub.service;

import com.google.common.collect.Lists;
import com.piaoniu.enumpub.annotations.EnumPub;
import com.piaoniu.enumpub.domain.TypeValue;
import com.piaoniu.enumpub.util.ScanUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public abstract class PublishManager {

    public abstract void saveNames(String name,String simpleName);

    public abstract String getRealName(String simpleName);

    public abstract List<String> getSimpleNames();

    public void init(List<String> packageNames){
        packageNames.forEach(
                p -> {
                    ScanUtil.getClasses(p).stream()
                            .filter(c -> c.getAnnotation(EnumPub.class)!=null)
                            .forEach(c -> {
                                saveNames(c.getName(),c.getSimpleName());
                            });
                }
        );
    }

    public List<String> publishEnumNames() {
        return getSimpleNames();
    }

    public List<TypeValue> publishEnum(String enumName) {
        try{
            Class enumClass = getEnumClass(enumName);
            Object[] objects = enumClass.getEnumConstants();
            Method getValue = enumClass.getMethod("getValue");
            Method getDesc = enumClass.getMethod("getDesc");
            return Lists.newArrayList(objects).stream().map(o -> {
                        try{
                            return TypeValue.of(Integer.valueOf(getValue.invoke(o).toString()),getDesc.invoke(o).toString());
                        }catch (Exception e){
                            return null;
                        }
                    }
            ).collect(Collectors.toList());
        } catch (Exception e){
            return Lists.newArrayList();
        }
    }

    private Class getEnumClass(String name) throws ClassNotFoundException{
        String realName = getRealName(name);
        return Class.forName(realName);
    }
}
