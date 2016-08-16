package com.piaoniu.plugin;

import com.piaoniu.annotations.NeedPermission;
import com.piaoniu.annotations.NoPermission;
import com.sun.tools.javac.code.Symbol;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
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
        NeedPermission needPermission = methodSymbol.getAnnotation(NeedPermission.class);
        NoPermission noPermission = methodSymbol.getAnnotation(NoPermission.class);
        if (needPermission == null && noPermission == null) {
            throw new RuntimeException("No permission configured for " + methodSymbol);
        }
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
