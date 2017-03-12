import org.zstack.core.Platform

/**
 * Created by xing5 on 2017/2/17.
 */
class Test2 extends org.zstack.testlib.Test {
    @Override
    void setup() {

    }

    @Override
    void environment() {

    }

    void test() {
        println(Platform.operr("appliance vm[uuid:%s] is in status of %s that cannot make http call to %s",
                Platform.getUuid(), "Stopped", "/v1/prc"))
    }
}
