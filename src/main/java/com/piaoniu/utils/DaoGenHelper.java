package com.piaoniu.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.piaoniu.annotations.DaoGen;
import com.piaoniu.generator.dao.DaoEnv;
import com.piaoniu.generator.dao.MapperMethod;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Context;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import javax.lang.model.element.ElementKind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DaoGenHelper {

    private Types types;
    private Trees trees;

    public DaoGenHelper(Trees trees, Context context) {
        this.trees = trees;
        this.types = Types.instance(context);
    }

    public String mixMethodToData(DaoGen daoGen, String namespace, Map<String, MapperMethod> methodMap, String data) {
        if (data.isEmpty())
            data="<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "\n" +
                    "<!DOCTYPE mapper\n" +
                    "PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
                    "\n" +
                    "<mapper namespace=\""+ namespace +"\">\n" +
                    "</mapper>\n";
        try {
            Document document = DocumentHelper.parseText(data);
            Element element = document.getRootElement();
            element.elements().forEach(sqlEle -> {
                String id = sqlEle.attribute("id").getText();
                methodMap.remove(id);
            });
            methodMap.forEach(getGenFunc(daoGen, element));

            OutputFormat format = OutputFormat.createPrettyPrint();
            try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
                XMLWriter writer  = new XMLWriter(outputStream,format);
                writer.write(document);

                return outputStream.toString("UTF-8");
            }
        } catch (DocumentException | IOException e) {
            throw new Error(e);
        }
    }

    private BiConsumer<String,MapperMethod> getGenFunc(DaoGen daoGen, Element root) {
        return (key,method)->{
            if (! ( handledWithThisPrefix(key, daoGen::insertPrefix, addInsert(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::updatePrefix,addUpdate(daoGen,key,method,root))
                    || handledWithThisPrefix(key, daoGen::findPrefix,addQueryOrFind(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::queryPrefix,addQueryOrFind(daoGen, key, method, root))
                    )){
                throw new Error("unknown method to be auto gen:"+key);
            }
        };
    }

    private boolean handledWithThisPrefix(String key, Supplier<String[]> prefixs, Consumer<String> genWithPrefix) {
        for (String prefix : prefixs.get()){
            if (key.startsWith(prefix)){
                genWithPrefix.accept(prefix);
                return true;
            }
        }
        return false;
    }

    public <T extends Symbol> List<T> getMember(Class<T> type, ElementKind kind, Symbol classSymbol) {
        List<T> results = Lists.newArrayList();
        if (classSymbol.type == null || classSymbol.type.isPrimitiveOrVoid()) {
            return results;
        }
        for (Type t : types.closure(classSymbol.type)) {
            Scope scope = t.tsym.members();
            scope.getElements(symbol -> symbol.getKind() == kind)
                    .forEach(symbol1 -> results.add(type.cast(symbol1)));
        }
        if (classSymbol.owner != null && classSymbol != classSymbol.owner
                && classSymbol.owner instanceof Symbol.ClassSymbol) {
            results.addAll(getMember(type, kind, classSymbol.owner));
        }
        if (classSymbol.hasOuterInstance()) {
            results.addAll(getMember(type, kind, classSymbol.type.getEnclosingType().asElement()));
        }
        return results;
    }

    private Map<String,List<String>> fieldsMap = Maps.newHashMap();

    private List<String> getFields(Symbol.TypeSymbol type){
        String typeStr = type.toString();
        if (!fieldsMap.containsKey(typeStr)){
            List<Symbol.VarSymbol> varSymbols  = getMember(Symbol.VarSymbol.class, ElementKind.FIELD, type);
            fieldsMap.put(typeStr, Lists.transform(varSymbols, (Symbol.VarSymbol::toString)));
        }
        return fieldsMap.get(typeStr);
    }
    private String lowerFirst(String word){
        return Character.toLowerCase(word.charAt(0)) + word.substring(1);
    }

    private static final String COMMENT  = "added by pn-plugin";

    private Consumer<String> addQueryOrFind(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix)->{
            Element sql = root.addElement("select");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);
            sql.addAttribute("resultType", method.getReturnType().toString());
            sql.addText("");
            String left = key.replaceFirst(prefix,"");
            String[] params = left.split(daoGen.separator());
            if (params.length == 0)
                throw new Error("At least need one param");
            StringBuilder select = new StringBuilder(50);
            List<String> fields = getFields(method.getReturnType());
            select.append("select ")
                    .append(Joiner.on(", ").join(fields))
                    .append(" from ")
                    .append(method.getDaoEnv().getTableName())
                    .append(" where ");
            int len = params.length;
            int cur = 0;
            for (String param : params){
                cur++;
                String realParam = lowerFirst(param);
                select.append(realParam)
                        .append(" = ")
                        .append("#{")
                        .append(realParam)
                        .append("}");
                if (cur<len) select.append(" and ");
            }
            sql.addText(select.toString());
        };
    }

    private Consumer<String> addUpdate(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix)->{
            Element sql = root.addElement("update");
            sql.addComment(COMMENT);
            sql.addAttribute("id",key);

            StringBuilder updateSql = new StringBuilder(50);
            updateSql.append("update ")
                    .append(method.getDaoEnv().getTableName())
                    .append(" set \n");

            List<String> fields = getFields(method.getFirstParamType());
            String pk = daoGen.primaryKey();
            updateSql.append(
                    Joiner.on(", ").join(
                            fields.stream().filter((field -> !field.equals(pk) &&
                                    !method.getDaoEnv().getCreateTimeSet().contains(field)))
                                    .map((field -> {
                                        if (method.getDaoEnv().getUpdateTimeSet().contains(field))
                                            return field + " = " + "now()";
                                        else return field + " = " + "#{" + field + "} ";
                                    }))
                                    .iterator()));

            updateSql.append("Where ")
                    .append(pk)
                    .append(" = ")
                    .append("#{")
                    .append(pk)
                    .append("}");
            sql.addText(updateSql.toString());
        };
    }

    private Stream<String> getInsertFieldsStream(String pk, List<String> fields){
        return fields.stream().filter((field) -> !pk.equals(field));
    }
    private Consumer<String> addInsert(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix)->{
            Element sql = root.addElement("insert");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);

            StringBuilder insertSql = new StringBuilder(50);
            insertSql.append("insert into ")
                    .append(method.getDaoEnv().getTableName())
                    .append("\n");

            String pk = daoGen.primaryKey();
            List<String> fields = getFields(method.getFirstParamType());
            insertSql.append("(")
                    .append(Joiner.on(", ").join(getInsertFieldsStream(pk,fields).iterator()))
                    .append(")\n");

            insertSql.append("values (");
            insertSql.append(Joiner.on(", ").join(getInsertFieldsStream(pk,fields).map(field -> {
                if (method.getDaoEnv().getCreateTimeSet().contains(field)
                        || method.getDaoEnv().getUpdateTimeSet().contains(field))
                    return "now()";
                else return "#{" + field + "}";
            }).iterator()));
            insertSql.append(")");
            sql.addText(insertSql.toString());

            Element selectKey = sql.addElement("selectKey");
            selectKey.addAttribute("resultType", "int");
            selectKey.addAttribute("keyProperty", pk);
            selectKey.addText("SELECT @@IDENTITY AS "+pk);
        };
    }

    public static MapperMethod toMapperMethod(DaoEnv daoEnv,Symbol.MethodSymbol methodSymbol){
        return new MapperMethod(daoEnv, methodSymbol);
    }

    public static String getMethodName(Symbol.MethodSymbol methodSymbol) {
        return methodSymbol.getSimpleName().toString();
    }
}
