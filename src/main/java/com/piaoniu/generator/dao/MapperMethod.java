package com.piaoniu.generator.dao;

import com.piaoniu.utils.DaoGenHelper;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

import java.util.List;

public class MapperMethod {
    private Symbol.TypeSymbol returnType;
    private String methodName;
    private DaoEnv daoEnv;
    private List<Symbol.VarSymbol> params;

    public MapperMethod(DaoEnv daoEnv, Symbol.MethodSymbol methodSymbol) {
        this.daoEnv = daoEnv;
        if (!methodSymbol.getTypeParameters().isEmpty()){
            throw new RuntimeException("不支持有类型参数的方法生成");
        }
        this.returnType = getRealType(methodSymbol.getReturnType());
        this.methodName = DaoGenHelper.getMethodName(methodSymbol);
        this.params = methodSymbol.getParameters();
    }

    private Symbol.TypeSymbol getRealType(Type type) {
        //List<String> to return String
        if (type.allparams().size()>0)
            return getRealType(type.allparams().get(0));

        if (type instanceof Type.TypeVar){
            return daoEnv.getRealTypeByTypeParameter(type);
        }

        return type.tsym;
    }

    public Symbol.TypeSymbol getReturnType() {
        return returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public DaoEnv getDaoEnv() {
        return daoEnv;
    }

    public List<Symbol.VarSymbol> getParams(){
        return params;
    }

    public Symbol.TypeSymbol getFirstParamType(){
        return getRealType(params.get(0).asType());
    }

}
