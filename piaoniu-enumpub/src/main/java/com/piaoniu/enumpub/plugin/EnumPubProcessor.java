package com.piaoniu.enumpub.plugin;

import com.piaoniu.enumpub.annotations.EnumPub;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.piaoniu.enumpub.annotations.EnumPub")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EnumPubProcessor extends AbstractProcessor {

    private Trees trees;
    private TreeMaker treeMaker;
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(EnumPub.class)
                .forEach(element -> {
                    Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) element;
                    EnumPub enumPub = classSymbol.getAnnotation(EnumPub.class);
                    JCTree tree = (JCTree)trees.getTree(element);
                    tree.accept((new EnumPubTreeTranslator(treeMaker,names,enumPub)));
                });
        return true;
    }
}
