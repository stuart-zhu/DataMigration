package dev.tornaco.vangogh.request;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import org.newstand.logger.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.tornaco.vangogh.display.ImageEffect;
import dev.tornaco.vangogh.media.Image;

/**
 * Created by guohao4 on 2017/8/25.
 * Email: Tornaco@163.com
 */

public class DisplayRequestDispatcherTornaco implements DisplayRequestDispatcher {

    private final Handler mainThreadHandler;

    private final ExecutorService executorService;

    private final Set<Integer> DIRTY_REQUESTS = new HashSet<>();

    public DisplayRequestDispatcherTornaco() {
        this.mainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int id = msg.what;
                if (DIRTY_REQUESTS.contains(id)) {
                    Logger.v("DisplayRequestDispatcherTornaco, Request of ID:%s is canceled", id);
                    return;
                }
                Logger.v("DisplayRequestDispatcherTornaco, Request of ID:%s will run", id);
                DisplayRequest request = (DisplayRequest) msg.obj;
                request.run();
            }
        };
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void dispatch(@NonNull final DisplayRequest displayRequest) {
        Logger.v("DisplayRequestDispatcherTornaco, dispatch: %s", displayRequest);
        this.executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (DIRTY_REQUESTS.contains(displayRequest.getId())) {
                    Logger.v("DisplayRequestDispatcherTornaco, Request of ID:%s is canceled", displayRequest.getId());
                    return;
                }
                ImageEffect[] effect = displayRequest.getEffect();
                Image effectedImage = displayRequest.getImage();
                if (effect != null) {
                    for (ImageEffect e : effect) {
                        effectedImage = e.process(effectedImage);
                    }
                    displayRequest.setImage(effectedImage);
                }
                mainThreadHandler.obtainMessage(displayRequest.getId(), displayRequest).sendToTarget();
            }
        });
    }


    @Override
    public boolean cancel(@NonNull DisplayRequest displayRequest, boolean interruptRunning) {
        Logger.i("DisplayRequestDispatcherTornaco, cancel: %s", displayRequest.getId());
        DIRTY_REQUESTS.add(displayRequest.getId());
        mainThreadHandler.removeMessages(displayRequest.getId());
        return true;
    }

    @Override
    public void cancelAll(boolean interruptRunning) {
        mainThreadHandler.removeCallbacksAndMessages(null);
    }
}
