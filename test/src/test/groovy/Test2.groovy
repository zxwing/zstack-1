import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import org.junit.Test

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 {
    @Test
    void test() {
        /*
        def fin = new FileInputStream("/root/VmInstanceBase.java")

        new VoidVisitorAdapter() {
            @Override
            void visit(MethodCallExpr n, Object arg) {
                super.visit(n, arg)
                if (n.getNameAsString() == "format") {
                    List<String> args = n.arguments.collect {
                        "${it.toString()} : ${it.class}"
                    }
                    System.out.println(args)
                }
                //System.out.println(n.getNameAsString() + " ${arg?.class}")
            }
        }.visit(JavaParser.parse(fin), null)
        */

        def s = "The image[uuid:%s] is on the backup storage[uuid:%s, type:%s] that requires to work with primary storage[uuids:%s],however, no host found suitable to work with those primary storage"

    }
}
