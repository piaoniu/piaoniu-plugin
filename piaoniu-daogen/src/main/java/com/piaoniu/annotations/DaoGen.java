package com.piaoniu.annotations;

public @interface DaoGen {
    String[] insertPrefix() default {"insert"};

    String[] batchInsertPrefix() default {"batchInsert"};

    String[] findPrefix() default {"findBy"};

    String[] removePrefix() default {"removeBy"};

    String[] queryPrefix() default {"queryBy"};

    String[] queryInPrefix() default {"queryIn"};

    String[] updatePrefix() default {"update"};

    String[] updateForPrefix() default {"updateFor"};

    String[] countPrefix() default {"countBy"};

    String[] countInPrefix() default {"countIn"};

    String[] countAllPrefix() default {"countAll"};

    String[] queryAllPrefix() default {"queryAll"};

    //String[] count() default {"countBy"};
    String separator() default "And";

    String tablePrefix() default "";

    String primaryKey() default "id";

    String createTime() default "addTime";

    String updateTime() default "updateTime";

    String tableName() default "";

    String orderBy() default "OrderBy";

    String orderByWith() default "With";
}
