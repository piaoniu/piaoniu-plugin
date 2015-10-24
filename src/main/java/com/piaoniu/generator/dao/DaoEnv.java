package com.piaoniu.generator.dao;

import com.google.common.collect.Sets;
import com.piaoniu.annotations.DaoGen;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.List;

import java.util.Set;

public class DaoEnv {
    private DaoGen daoGen;
    private String daoClassName;
    private String tableName;
    private Set<String> createTimeSet;
    private Set<String> updateTimeSet;
    Type typeParameter;

    public DaoEnv(DaoGen daoGen,Symbol.ClassSymbol classSymbol) {
        this.daoGen = daoGen;
        createTimeSet = Sets.newHashSet(daoGen.createTime());
        updateTimeSet = Sets.newHashSet(daoGen.updateTime());
        daoClassName = classSymbol.getSimpleName().toString();
        if (!daoGen.tableName().isEmpty())
            tableName = daoGen.tablePrefix()+daoGen.tableName();
            //"Activity{Dao}"
        else tableName = daoGen.tablePrefix()+daoClassName.subSequence(0,daoClassName.length()-3);
        if (classSymbol.getInterfaces() != null){
            classSymbol.getInterfaces().forEach(i->{
                if (i.getTypeArguments()!=null && !i.getTypeArguments().isEmpty())
                    typeParameter = i.getTypeArguments().get(0);

            });
        }
    }

    public String getTableName() {
        return tableName;
    }

    public Set<String> getUpdateTimeSet() {
        return updateTimeSet;
    }

    public Set<String> getCreateTimeSet() {
        return createTimeSet;
    }

    public Symbol.TypeSymbol getRealTypeByTypeParameter(Type type) {
        return typeParameter.tsym;
    }
}
