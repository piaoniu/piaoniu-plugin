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

    private void setValue(int value){
        this.value = value;
    }

    private void setDesc(String desc){
        this.desc = desc;
    }
}
