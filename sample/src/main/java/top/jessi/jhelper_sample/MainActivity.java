package top.jessi.jhelper_sample;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.DispatchersKt;
import top.jessi.jhelper.thread.ThreadPool;

/**
 * Created by Jessi on 2024/8/5 11:26
 * Email：17324719944@189.cn
 * Describe：
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String plaintext = "Hello, World";
        String key = "994400LeoJessi10";
        String iv = "LeoJessi99440010";

        ThreadPool.execute(Dispatchers.getMain(), new Function1<Continuation<? super Unit>, Object>() {
            @Override
            public Object invoke(Continuation<? super Unit> continuation) {
                return null;
            }
        });
    }
}
