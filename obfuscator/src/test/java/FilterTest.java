import com.yumegod.obfuscator.utils.filter.ClassFilterExpr;
import com.yumegod.obfuscator.utils.filter.NodeMatcher;

public class FilterTest {

    public static void main(String[] args) {
        System.out.println(new ClassFilterExpr("**.P extend java.lang.Object implement java.lang.Function #public static void main(java.lang.String[])"));

        System.out.println(new ClassFilterExpr("  **.Q*  implements wtf.IsAInterface # void holyShit"));

        System.out.println(new ClassFilterExpr("**.R* impl wtf.IsItAExpression?,wtf.WhatATotalMess # final int aFinalField guess? # protected aFieldMethod(**) #@**.AMethodAnnotation void *(**) a annotated method xD"));
    }
}
