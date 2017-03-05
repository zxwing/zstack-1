package org.zstack.core.asyncbatch;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.utils.DebugUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.BiConsumer;

/**
 * Created by xing5 on 2017/3/5.
 */
public class While<T> {
    private Collection<T> items;
    private Do consumer;

    public interface Do<T> {
        void accept(T item, NoErrorCompletion completion);
    }

    private While(Collection<T> items) {
        this.items = items;
    }

    public While each(Do<T> consumer) {
        this.consumer = consumer;
        return this;
    }

    private void run(Iterator<T> it, NoErrorCompletion completion) {
        if (!it.hasNext()) {
            completion.done();
            return;
        }

        T t = it.next();
        consumer.accept(t, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                run(it, completion);
            }
        });
    }

    public void run(NoErrorCompletion completion) {
        DebugUtils.Assert(consumer != null, "each must be called before run()");

        run(items.iterator(), completion);
    }

    public static <T> While New(Collection<T> c) {
        return new While<>(c);
    }
}
