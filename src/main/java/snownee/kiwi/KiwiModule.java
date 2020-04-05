package snownee.kiwi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KiwiModule
{
    String modid();

    String name() default "";

    String dependency() default "";

    boolean optional() default false;

    boolean disabledByDefault() default false;
}
