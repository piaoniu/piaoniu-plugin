package com.piaoniu.enumpub.service;

import com.piaoniu.enumpub.domain.TypeValue;

import java.util.List;

public interface PublishService {

    List<String> publishEnumNames();

    List<TypeValue> publishEnum(String enumName);

}
