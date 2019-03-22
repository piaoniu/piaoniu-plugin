package com.piaoniu.enumpub.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.piaoniu.enumpub.annotations.EnumPub;
import com.piaoniu.enumpub.util.ScanUtil;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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

    public List<Map<String, String>> publishEnum(String enumName) {
        try{
            Class enumClass = getEnumClass(enumName);
            Object[] objects = enumClass.getEnumConstants();
            Method getValue = enumClass.getMethod("getValue");
            Method getDesc = enumClass.getMethod("getDesc");
            return Lists.newArrayList(objects).stream().map(o -> {
                        try{
                            Map<String, String> map = Maps.newHashMap();
                            map.put("value", getValue.invoke(o).toString());
                            map.put("desc", getDesc.invoke(o).toString());
                            return map;
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
