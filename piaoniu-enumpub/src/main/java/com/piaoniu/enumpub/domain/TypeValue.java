package com.piaoniu.enumpub.domain;

public class TypeValue {

    private Integer value;

    private String desc;

    public static TypeValue of(int value, String desc){
        return new TypeValue(value,desc);
    }

    TypeValue(int value, String desc){
        this.value = value;
        this.desc = desc;
    }

    private void setValue(int value){
        this.value = value;
    }

    private void setDesc(String desc){
        this.desc = desc;
    }

    private int getValue(int value){
        return this.value;
    }

    private String getDesc(String desc){
        return this.desc;
    }
}
