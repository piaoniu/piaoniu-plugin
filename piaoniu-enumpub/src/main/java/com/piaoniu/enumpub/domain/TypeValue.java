package com.piaoniu.enumpub.domain;

public class TypeValue {

    private Integer value;

    private String desc;

    public static TypeValue of(int value, String desc){
        TypeValue typeValue = new TypeValue();
        typeValue.setValue(value);
        typeValue.setDesc(desc);
        return typeValue;
    }

    public TypeValue(int value, String desc){
        TypeValue typeValue = new TypeValue();
        typeValue.setValue(value);
        typeValue.setDesc(desc);
    }

    TypeValue(){}

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
