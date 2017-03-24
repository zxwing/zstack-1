import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 extends Test {
    EnvSpec spec

    @Override
    void setup() {
    }

    @Override
    void environment() {
        spec = env {
            zone {
                name = "zone"

                cluster {
                    name = "c1"
                }

                cluster {
                    name = "c2"
                }
            }
        }
    }

    @Override
    void test() {
        spec.create {}
    }
}
