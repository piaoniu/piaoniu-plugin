package com.piaoniu.enumpub.plugin;

import com.piaoniu.enumpub.annotations.EnumPub;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;


public class EnumPubTreeTranslator extends TreeTranslator {


    TreeMaker treeMaker;
    Names names;
    EnumPub enumPub;

    public EnumPubTreeTranslator(TreeMaker treeMaker, Names names, EnumPub enumPub) {
        this.treeMaker = treeMaker;
        this.names = names;
        this.enumPub = enumPub;
    }


    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {

        List<JCTree.JCVariableDecl> jcVariableDecls = jcClassDecl.defs.stream()
                .filter(k -> k.getKind().equals(Tree.Kind.VARIABLE))
                .map(tree -> (JCTree.JCVariableDecl) tree)
                .collect(Collectors.toList());

        List<JCTree.JCMethodDecl> jcMethodDecls = jcClassDecl.defs.stream()
                .filter(k -> k.getKind().equals(Tree.Kind.METHOD))
                .map(tree -> (JCTree.JCMethodDecl) tree)
                .collect(Collectors.toList());
        if (!hasMethod(jcMethodDecls, "getValue")) {
            jcClassDecl.defs = jcClassDecl.defs.append(createGetValueMethod(jcClassDecl, jcVariableDecls));
        }
        if (!hasMethod(jcMethodDecls, "getDesc")) {
            jcClassDecl.defs = jcClassDecl.defs.append(createGetDescMethod(jcClassDecl, jcVariableDecls));
        }

        super.visitClassDef(jcClassDecl);
    }

    private boolean hasMethod(List<JCTree.JCMethodDecl> jcMethodDecls, String methodName) {
        return jcMethodDecls.stream().anyMatch(e -> e.getName().toString().equals(methodName));
    }


    private JCTree.JCMethodDecl createGetValueMethod(JCTree.JCClassDecl jcClassDecl, List<JCTree.JCVariableDecl> jcVariableDecls) {
        Optional<JCTree.JCVariableDecl> valueVariableDecl = jcVariableDecls.stream()
                .filter(e -> e.name.toString().equals(enumPub.value()))
                .findAny();
        if (!valueVariableDecl.isPresent()) {
            throw new NoSuchElementException("no param value: " + enumPub.value() + " from class " + jcClassDecl.getKind().name() + jcClassDecl.getSimpleName());
        }
        return createGetMethod(valueVariableDecl.get(), names.fromString("getValue"));
    }

    private JCTree.JCMethodDecl createGetDescMethod(JCTree.JCClassDecl jcClassDecl, List<JCTree.JCVariableDecl> jcVariableDecls) {
        Optional<JCTree.JCVariableDecl> valueVariableDecl = jcVariableDecls.stream()
                .filter(e -> e.name.toString().equals(enumPub.desc()))
                .findAny();
        if (!valueVariableDecl.isPresent()) {
            throw new NoSuchElementException("no param desc" + jcClassDecl.getSimpleName());
        }
        return createGetMethod(valueVariableDecl.get(), names.fromString("getDesc"));
    }


    private JCTree.JCMethodDecl createGetMethod(JCTree.JCVariableDecl jcVariableDecl, Name methodName) {
        //方法的访问级别
        JCTree.JCModifiers modifiers = treeMaker.Modifiers(Flags.PUBLIC);
        //设置返回值类型
        JCTree.JCExpression returnMethodType = jcVariableDecl.vartype;
        //泛型参数列表
        com.sun.tools.javac.util.List<JCTree.JCTypeParameter> typeParameters = com.sun.tools.javac.util.List.nil();
        //参数列表
        com.sun.tools.javac.util.List<JCTree.JCVariableDecl> parameters = com.sun.tools.javac.util.List.nil();
        //异常声明列表
        com.sun.tools.javac.util.List<JCTree.JCExpression> throwsClauses = com.sun.tools.javac.util.List.nil();
        //设置方法体
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<>();
        statements.append(treeMaker.Return(
                treeMaker.Select(
                        treeMaker.Ident(names.fromString("this")),
                        jcVariableDecl.getName())
                )
        );
        JCTree.JCBlock methodBody = treeMaker.Block(0, statements.toList());
        //构建方法
        return treeMaker.MethodDef(modifiers, methodName, returnMethodType, typeParameters, parameters, throwsClauses, methodBody, null);
    }
}
