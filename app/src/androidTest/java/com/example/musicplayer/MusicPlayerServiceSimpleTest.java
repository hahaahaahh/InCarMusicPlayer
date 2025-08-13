package com.example.musicplayer;

import android.content.Intent;
import android.os.IBinder;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class MusicPlayerServiceSimpleTest {
    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @Test
    public void testServiceStart() throws Exception {
        Intent serviceIntent = new Intent(
            InstrumentationRegistry.getInstrumentation().getTargetContext(),
            MusicPlayerService.class);

        IBinder binder = serviceRule.bindService(serviceIntent);
        MusicPlayerService.MusicPlayerBinder musicBinder = (MusicPlayerService.MusicPlayerBinder) binder;
        MusicPlayerService service = musicBinder.getService();

        assertNotNull(service);
        // 可以直接调用service的方法进行测试
    }
} 