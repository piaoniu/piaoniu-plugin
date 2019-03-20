package com.piaoniu.service;

import java.util.List;

public interface PublishManager {

    void saveNames(String name,String simpleName);

    String getRealName(String simpleName);

    List<String> getSimpleNames();
}
