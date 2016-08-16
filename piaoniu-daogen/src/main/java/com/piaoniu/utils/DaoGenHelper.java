package com.piaoniu.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.piaoniu.permission.annotations.DaoGen;
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
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.lang.model.element.ElementKind;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DaoGenHelper {

    private Types types;
    private Trees trees;

    public DaoGenHelper(Trees trees, Context context) {
        this.trees = trees;
        this.types = Types.instance(context);
    }

    public static Document parseText(String text) throws DocumentException, SAXException {
        SAXReader reader = new SAXReader(false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        reader.setFeature("http://apache.org/xml/features/validation/schema", false);
        String encoding = getEncoding(text);

        InputSource source = new InputSource(new StringReader(text));
        source.setEncoding(encoding);

        Document result = reader.read(source);

        // if the XML parser doesn't provide a way to retrieve the encoding,
        // specify it manually
        if (result.getXMLEncoding() == null) {
            result.setXMLEncoding(encoding);
        }

        return result;
    }

    private static String getEncoding(String text) {
        String result = null;

        String xml = text.trim();

        if (xml.startsWith("<?xml")) {
            int end = xml.indexOf("?>");
            String sub = xml.substring(0, end);
            StringTokenizer tokens = new StringTokenizer(sub, " =\"\'");

            while (tokens.hasMoreTokens()) {
                String token = tokens.nextToken();

                if ("encoding".equals(token)) {
                    if (tokens.hasMoreTokens()) {
                        result = tokens.nextToken();
                    }

                    break;
                }
            }
        }

        return result;
    }

    public String mixMethodToData(DaoGen daoGen, String namespace, Map<String, MapperMethod> methodMap, String data) {
        if (data.isEmpty())
            data = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                    "\n" +
                    "<!DOCTYPE mapper\n" +
                    "PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\n" +
                    "\n" +
                    "<mapper namespace=\"" + namespace + "\">\n" +
                    "</mapper>\n";
        try {
            Document document = parseText(data);
            Element element = document.getRootElement();
            element.elements().forEach(sqlEle -> {
                String id = sqlEle.attribute("id").getText();
                methodMap.remove(id);
            });
            methodMap.forEach(getGenFunc(daoGen, element));

            OutputFormat format = OutputFormat.createPrettyPrint();
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                XMLWriter writer = new XMLWriter(outputStream, format);
                writer.write(document);
                writer.flush();

                return outputStream.toString("UTF-8");
            }
        } catch (DocumentException | IOException | SAXException e) {
            throw new Error(e);
        }
    }

    private BiConsumer<String, MapperMethod> getGenFunc(DaoGen daoGen, Element root) {
        return (key, method) -> {
            if (!(handledWithThisPrefix(key, daoGen::insertPrefix, addInsert(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::updateForPrefix, addUpdateFor(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::updatePrefix, addUpdate(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::findPrefix, addQueryOrFind(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::queryAllPrefix, addQueryAll(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::queryInPrefix, addQueryIn(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::queryPrefix, addQueryOrFind(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::countAllPrefix, addCountAll(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::countPrefix, addCountBy(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::countInPrefix, addCountIn(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::removePrefix, addRemove(daoGen, key, method, root))
                    || handledWithThisPrefix(key, daoGen::batchInsertPrefix, addBatchInsert(daoGen, key, method, root))
            )) {
                throw new Error("unknown method to be auto gen:" + key);
            }
        };
    }

    private Consumer<String> addCountIn(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
            Element sql = root.addElement("select");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);
            sql.addAttribute("resultType", "int");
            String left = key.replaceFirst(prefix, "");
            List<String> params = split(left, daoGen.separator());
            StringBuilder select = new StringBuilder(50);
            select.append("select count(1) from  ")
                    .append(method.getDaoEnv().getTableName());
            int len = params.size();
            if (len != 1)
                throw new Error("count in method only support one param");
            if (!params.isEmpty()) select.append(" where ");
            String param = params.get(0);

            sql.addText(select.toString());
            if (param.endsWith("s")) param = lowerFirst(param.substring(0,param.length()-1));

            Element choose = sql.addElement("choose");
            String collection = param+"s";
            Element when = choose.addElement("when");
            when.addAttribute("test", collection + " !=null and " + collection + ".size() > 0");
            when.addText("`" + param + "` in ");

            Element each = when.addElement("foreach");
            each.addAttribute("item", param);
            each.addAttribute("collection", param+"s");
            each.addAttribute("open", "(");
            each.addAttribute("separator", ",");
            each.addAttribute("close", ")");
            each.addText("#{" + param + "}");

            Element otherwise = choose.addElement("otherwise");
            otherwise.addText(" 1 = 2 ");

            sql.addText(" order by " + daoGen.primaryKey());
        };
    }

    private Consumer<String> addUpdateFor(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
            Element sql = root.addElement("update");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);

            StringBuilder updateSql = new StringBuilder(50);
            updateSql.append("update ")
                    .append(method.getDaoEnv().getTableName())
                    .append(" set \n");

            String left = key.replaceFirst(prefix, "");
            List<String> fields = split(left, daoGen.separator()).stream().map(this::lowerFirst).collect(Collectors.toList());
            fields.add(method.getDaoEnv().getUpdateTime());
            String pk = daoGen.primaryKey();
            updateSql.append(
                    Joiner.on(", ").join(
                            fields.stream().filter((field -> !field.equals(pk) &&
                                    !method.getDaoEnv().getCreateTime().equals(field)))
                                    .map((field -> {
                                        if (method.getDaoEnv().getUpdateTime().equals(field))
                                            return "`" + field + "` = " + "now() ";
                                        else return "`" + field + "` = " + "#{" + field + "} ";
                                    }))
                                    .iterator()));

            updateSql.append("Where `")
                    .append(pk)
                    .append("` = ")
                    .append("#{")
                    .append(pk)
                    .append("}");
            sql.addText(updateSql.toString());
        };
    }

    private Consumer<String> addQueryIn(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
            Element sql = root.addElement("select");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);
            sql.addAttribute("resultType", method.getReturnType().toString());
            String left = key.replaceFirst(prefix, "");
            List<String> params = split(left, daoGen.separator());
            StringBuilder select = new StringBuilder(50);
            List<String> fields = getFields(method.getReturnType());
            select.append("select ")
                    .append(Joiner.on(", ").join(fields.stream().map(f-> "`" + f + "`").iterator()))
                    .append(" from ")
                    .append(method.getDaoEnv().getTableName());
            int len = params.size();
            if (len != 1)
                throw new Error("query in method only support one param");
            if (!params.isEmpty()) select.append(" where ");
            String param = params.get(0);

            sql.addText(select.toString());
            if (param.endsWith("s")) param = lowerFirst(param.substring(0,param.length()-1));

            Element choose = sql.addElement("choose");
            String collection = param+"s";
            Element when = choose.addElement("when");
            when.addAttribute("test", collection + " !=null and " + collection + ".size() > 0");
            when.addText("`" + param + "` in ");

            Element each = when.addElement("foreach");
            each.addAttribute("item", param);
            each.addAttribute("collection", param+"s");
            each.addAttribute("open", "(");
            each.addAttribute("separator", ",");
            each.addAttribute("close", ")");
            each.addText("#{" + param + "}");

            Element otherwise = choose.addElement("otherwise");
            otherwise.addText(" 1 = 2 ");

            sql.addText(" order by " + daoGen.primaryKey());
        };
    }

    private Consumer<String> addRemove(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
            Element sql = root.addElement("delete");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);
            String left = key.replaceFirst(prefix, "");
            List<String> params = split(left, daoGen.separator());
            if (params.isEmpty()) throw new Error("Remove method needs at least  one param");
            StringBuilder select = new StringBuilder(50);
            select.append("delete from ")
                    .append(method.getDaoEnv().getTableName());
            int len = params.size();
            if (!params.isEmpty()) select.append(" where ");
            int cur = 0;
            appendParams(params, select, len, cur);
            sql.addText(select.toString());
        };
    }

    private void appendParams(List<String> params, StringBuilder select, int len, int cur) {
        for (String param : params) {
            cur++;
            String realParam = lowerFirst(param);
            select.append("`")
                    .append(realParam)
                    .append("`")
                    .append(" = ")
                    .append("#{")
                    .append(realParam)
                    .append("}");
            if (cur < len) select.append(" and ");
        }
    }

    private boolean handledWithThisPrefix(String key, Supplier<String[]> prefixs, Consumer<String> genWithPrefix) {
        for (String prefix : prefixs.get()) {
            if (key.startsWith(prefix)) {
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
            if (scope == null) continue;
            scope.getElements(symbol -> symbol.getKind() == kind)
                    .forEach(s->results.add(type.cast(s)));
        }
        if (classSymbol.owner != null && classSymbol != classSymbol.owner
                && classSymbol.owner instanceof Symbol.ClassSymbol) {
            results.addAll(getMember(type, kind, classSymbol.owner));
        }
        if (classSymbol.type.getEnclosingType() != null && classSymbol.hasOuterInstance()) {
            results.addAll(getMember(type, kind, classSymbol.type.getEnclosingType().asElement()));
        }
        return results;
    }

    private Map<String, List<String>> fieldsMap = Maps.newHashMap();

    private List<String> getFields(Symbol.TypeSymbol type) {
        String typeStr = type.toString();
        if (!fieldsMap.containsKey(typeStr)) {
            List<Symbol.VarSymbol> varSymbols = getMember(Symbol.VarSymbol.class, ElementKind.FIELD, type);
            fieldsMap.put(typeStr, varSymbols.stream().filter(s->!s.isStatic()).map(Symbol.VarSymbol::toString).collect(Collectors.toList()));
        }
        return fieldsMap.get(typeStr);
    }

    private String lowerFirst(String word) {
        return Character.toLowerCase(word.charAt(0)) + word.substring(1);
    }

    private static final String COMMENT = "added by pn-plugin";

    private Consumer<String> addCountAll(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return addCount(daoGen,key,method,root,(params)-> {});
    }

    private Consumer<String> addCountBy(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return addCount(daoGen,key,method,root,(params)-> {
            if (params.isEmpty())
                throw new Error("At least need one param");
        });
    }

    private Consumer<String> addCount(DaoGen daoGen, String key, MapperMethod method, Element root,Consumer<List<String>> validator) {
        return (prefix) -> {
            Element sql = root.addElement("select");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);
            sql.addAttribute("resultType", "int");
            String left = key.replaceFirst(prefix, "");
            List<String> params = split(left, daoGen.separator());
            validator.accept(params);
            StringBuilder select = new StringBuilder(50);
            select.append("select count(1) from ")
                    .append(method.getDaoEnv().getTableName());
            int len = params.size();
            if (!params.isEmpty()) select.append(" where ");
            int cur = 0;
            appendParams(params, select, len, cur);
            select.append(" order by ").append(daoGen.primaryKey());
            sql.addText(select.toString());
        };
    }

    private static List<String> split(String left, String separator) {
        List<String> results = new ArrayList<>();
        if (StringUtils.isEmpty(left)) return results;
        for (String str:left.split(separator)) {
            if (!StringUtils.isEmpty(str))
                results.add(str);
        }
        return results;
    }

    private Consumer<String> addQueryAll(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return addQuery(daoGen,key,method,root,(params)-> {});
    }

    private Consumer<String> addQuery(DaoGen daoGen, String key, MapperMethod method, Element root,Consumer<List<String>> validator) {
        return (prefix) -> {
            Element sql = root.addElement("select");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);
            sql.addAttribute("resultType", method.getReturnType().toString());
            String left = key.replaceFirst(prefix, "");
            String orderClause = "";
            if (left.contains(daoGen.orderBy())){
                int index = left.indexOf(daoGen.orderBy());
                orderClause = left.substring(index+daoGen.orderBy().length(),left.length());
                left = left.substring(0,index);
            }
            List<String> params = split(left, daoGen.separator());
            validator.accept(params);
            StringBuilder select = new StringBuilder(50);
            List<String> fields = getFields(method.getReturnType());
            select.append("select ")
                    .append(Joiner.on(", ").join(fields.stream().map(f-> "`" + f + "`").iterator()))
                    .append(" from ")
                    .append(method.getDaoEnv().getTableName());
            int len = params.size();
            if (!params.isEmpty()) select.append(" where ");
            int cur = 0;
            appendParams(params, select, len, cur);
            String order = "ASC";
            String orderKey = daoGen.primaryKey();
            if (!orderClause.isEmpty()){
                if (orderClause.contains(daoGen.orderByWith())){
                    int index = orderClause.indexOf(daoGen.orderByWith());
                    order = orderClause.substring(index+daoGen.orderByWith().length(),orderClause.length());
                    orderKey = orderClause.substring(0,index);
                }else {
                    orderKey = orderClause;
                }
            }
            select.append(" order by ").append(orderKey).append(" ").append(order);
            sql.addText(select.toString());
        };

    }

    private Consumer<String> addQueryOrFind(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return addQuery(daoGen, key, method, root, (params) -> {
            if (params.isEmpty()) throw new Error("At least need one param");
        });
    }

    private Consumer<String> addUpdate(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
            Element sql = root.addElement("update");
            sql.addComment(COMMENT);
            sql.addAttribute("id", key);

            StringBuilder updateSql = new StringBuilder(50);
            updateSql.append("update ")
                    .append(method.getDaoEnv().getTableName())
                    .append(" set \n");

            List<String> fields = getFields(method.getFirstParamType());
            String pk = daoGen.primaryKey();
            String updateByField;
            String entity;
            if (key.startsWith("updateBy")){
                updateByField = lowerFirst(key.substring(8));
                entity = "entity";
            }else{
                updateByField = pk;
                entity = null;
            }
            updateSql.append(
                    Joiner.on(", ").join(
                            fields.stream().filter((field -> !field.equals(pk)
                                    && !field.equals(updateByField)
                                    && !method.getDaoEnv().getCreateTime().equals(field)))
                                    .map((field -> {
                                        if (method.getDaoEnv().getUpdateTime().equals(field))
                                            return "`" + field + "` = " + "now() ";
                                        else
                                            return "`" + field + "` = " + "#{" + (entity != null ?
                                                    entity + "." :
                                                    "") + field + "} ";
                                    }))
                                    .iterator()));

            updateSql.append("Where `")
                    .append(updateByField)
                    .append("` = ")
                    .append("#{")
                    .append(updateByField)
                    .append("}");
            sql.addText(updateSql.toString());
        };
    }

    private Stream<String> getInsertFieldsStream(String pk, List<String> fields) {
        return fields.stream().filter((field) -> !pk.equals(field));
    }


    private Consumer<String> addBatchInsert(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
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
                    .append(Joiner.on(", ").join(getInsertFieldsStream(pk, fields).map(f -> "`"+f + "`").iterator()))
                    .append(")\n");

            insertSql.append("values ");
            sql.addText(insertSql.toString());
            Element foreach = sql.addElement("foreach");
            foreach.addAttribute("collection", "list");
            foreach.addAttribute("item", "item");
            foreach.addAttribute("separator", ",");
            StringBuilder eachSql = new StringBuilder(50);
            eachSql.append("(").append(Joiner.on(", ").join(getInsertFieldsStream(pk, fields).map(field -> {
                if (method.getDaoEnv().getCreateTime().contains(field)
                        || method.getDaoEnv().getUpdateTime().contains(field))
                    return "now()";
                else return "#{item." + field + "}";
            }).iterator()));
            eachSql.append(")");
            foreach.addText(eachSql.toString());
        };
    }

    private Consumer<String> addInsert(DaoGen daoGen, String key, MapperMethod method, Element root) {
        return (prefix) -> {
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
                    .append(Joiner.on(", ").join(getInsertFieldsStream(pk, fields).map(f -> "`"+f + "`").iterator()))
                    .append(")\n");

            insertSql.append("values (");
            insertSql.append(Joiner.on(", ").join(getInsertFieldsStream(pk, fields).map(field -> {
                if (method.getDaoEnv().getCreateTime().contains(field)
                        || method.getDaoEnv().getUpdateTime().contains(field))
                    return "now()";
                else return "#{" + field + "}";
            }).iterator()));
            insertSql.append(")");
            sql.addText(insertSql.toString());

            Element selectKey = sql.addElement("selectKey");
            selectKey.addAttribute("resultType", "int");
            selectKey.addAttribute("keyProperty", pk);
            selectKey.addText("SELECT @@IDENTITY AS " + pk);
        };
    }

    public static MapperMethod toMapperMethod(DaoEnv daoEnv, Symbol.MethodSymbol methodSymbol) {
        return new MapperMethod(daoEnv, methodSymbol);
    }

    public static String getMethodName(Symbol.MethodSymbol methodSymbol) {
        return methodSymbol.getSimpleName().toString();
    }
}
