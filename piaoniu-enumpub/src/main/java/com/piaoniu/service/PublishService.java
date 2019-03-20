package com.piaoniu.service;

import com.piaoniu.domain.TypeValue;

import java.util.List;

public interface PublishService {

    List<String> publishEnumNames();

    List<TypeValue> publishEnum(String enumName);

}
