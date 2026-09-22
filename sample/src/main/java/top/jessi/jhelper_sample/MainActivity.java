package top.jessi.jhelper_sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import top.jessi.jhelper.time.Time;

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

        Button button = findViewById(R.id.btn_time);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 无需传 context，直接使用
                long time = Time.currentTimeMillis();
                boolean synced = Time.isTimeSynced();
                long offset = Time.currentTimeOffset();

                Log.d("MainActivity", "网络时间: " + time);
                Log.d("MainActivity", "是否已同步: " + synced);
                Log.d("MainActivity", "时间偏移: " + offset + "ms");
            }
        });
    }
}
