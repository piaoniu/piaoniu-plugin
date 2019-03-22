package com.piaoniu.enumpub.domain;

import java.io.Serializable;

public class TypeValue implements Serializable {

    private String value;

    private String desc;

    public static TypeValue of(String value, String desc){
        TypeValue typeValue = new TypeValue();
        typeValue.setValue(value);
        typeValue.setDesc(desc);
        return typeValue;
    }

    public TypeValue(String value, String desc){
        TypeValue typeValue = new TypeValue();
        typeValue.setValue(value);
        typeValue.setDesc(desc);
    }

    TypeValue(){}

    private void setValue(String value){
        this.value = value;
    }

    private void setDesc(String desc){
        this.desc = desc;
    }

    private String getValue(String value){
        return this.value;
    }

    private String getDesc(String desc){
        return this.desc;
    }
}
