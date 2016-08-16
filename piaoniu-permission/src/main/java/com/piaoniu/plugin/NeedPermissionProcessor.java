package com.piaoniu.plugin;

import com.piaoniu.permission.annotations.NeedPermission;
import com.piaoniu.permission.annotations.NoPermission;
import com.sun.tools.javac.code.Symbol;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Set;

@SupportedAnnotationTypes("org.springframework.web.bind.annotation.RequestMapping")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class NeedPermissionProcessor extends AbstractProcessor {

    public static final String PATH = "org.springframework.web.bind.annotation.RequestMapping";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
    }

    public void handle(Element element) {
        if (! (element.getKind() == ElementKind.METHOD)) return;
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
        if (!(methodSymbol.getEnclosingElement() instanceof Symbol.ClassSymbol)){
            System.out.println("[PN permission] Unknown parent " + methodSymbol.getEnclosingElement().toString());
            return;
        }
        Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) methodSymbol.getEnclosingElement();
        if (!containsAnnotation(methodSymbol,classSymbol,NeedPermission.class)&&!containsAnnotation(methodSymbol,classSymbol,NoPermission.class)){
            throw new RuntimeException("No permission configured for " + classSymbol + "." + methodSymbol);
        }
    }

    private boolean containsAnnotation(Symbol.MethodSymbol methodSymbol,Symbol.ClassSymbol classSymbol,Class<? extends Annotation> clazz){
        if (methodSymbol.getAnnotation(clazz) != null) {
            return true;
        }
        if (classSymbol.getAnnotation(clazz) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.stream()
                .filter(typeElement -> typeElement.toString().equals(PATH))
                .forEach(typeElement -> roundEnv.getElementsAnnotatedWith(typeElement)
                        .forEach((this::handle)));
        return true;
    }
}
