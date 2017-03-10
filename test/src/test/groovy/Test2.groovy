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
        def fin = new FileInputStream("/root/VmInstanceBase.java")

        new VoidVisitorAdapter() {
            @Override
            void visit(MethodCallExpr n, Object arg) {
                super.visit(n, arg)
                if (n.getNameAsString() == "format") {
                    System.out.println(n.getArguments())
                }
                //System.out.println(n.getNameAsString() + " ${arg?.class}")
            }
        }.visit(JavaParser.parse(fin), null)
    }
}
