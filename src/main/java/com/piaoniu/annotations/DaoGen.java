package com.piaoniu.annotations;

public @interface DaoGen {
    String[] insertPrefix() default {"insert"};

    String[] batchInsertPrefix() default {"batchInsert"};

    String[] findPrefix() default {"findBy"};

    String[] removePrefix() default {"removeBy"};

    String[] queryPrefix() default {"queryBy"};

    String[] updatePrefix() default {"update"};

    String[] countPrefix() default {"countBy"};

    String[] countAllPrefix() default {"countAll"};

    String[] queryAllPrefix() default {"queryAll"};

    //String[] count() default {"countBy"};
    String separator() default "And";

    String tablePrefix() default "PN_";

    String primaryKey() default "id";

    String[] createTime() default {"createdAt"};

    String[] updateTime() default {"updatedAt"};

    String tableName() default "";
}
