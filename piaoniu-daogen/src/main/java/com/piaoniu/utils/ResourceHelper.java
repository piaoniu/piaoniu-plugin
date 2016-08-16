package com.piaoniu.utils;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.function.BiConsumer;

import static com.piaoniu.utils.PrintUtils.note;

public class ResourceHelper {

    public static void doWithOriAndPrintWriter(Filer filer, JavaFileManager.Location location, String relativePath, String filename, BiConsumer<String, PrintWriter> consumer){
        try {
            FileObject resource = filer.getResource(location, relativePath, filename);
            String data;
            try{
                CharSequence cs = resource.getCharContent(false);
                data = cs.toString();
                resource.delete();
            }catch (FileNotFoundException ignored){
                data = "";
            }
            resource = filer.createResource(location, relativePath, filename);

            try(OutputStream outputStream = resource.openOutputStream()){
                consumer.accept(data,new PrintWriter(outputStream));
            }
        } catch (IOException e) {
            note("do with resource file failed"+relativePath+filename+" Exception: " + e.toString());
        }
    }
}
