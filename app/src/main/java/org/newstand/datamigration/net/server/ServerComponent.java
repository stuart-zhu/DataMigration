package org.newstand.datamigration.net.server;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.common.base.Optional;

import org.newstand.datamigration.common.Consumer;
import org.newstand.logger.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Created by Nick@NewStand.org on 2017/3/22 15:21
 * E-Mail: NewStand@163.com
 * All right reserved.
 */
@Getter
@NoArgsConstructor
@ToString(of = {"host", "port"})
public abstract class ServerComponent implements Component {

    static final int MAX_RETRY_TIMES = 3;

    @Setter(AccessLevel.PROTECTED)
    private OutputStream outputStream;
    @Setter(AccessLevel.PROTECTED)
    private InputStream inputStream;

    @Setter
    private String host;
    @Setter
    private int port;

    @Override
    public String name() {
        return getClass().getSimpleName();
    }

    @VisibleForTesting
    public Runnable asRunnable(final Consumer<Exception> exceptionConsumer) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    start();
                } catch (IOException ignored) {
                    Optional.of(exceptionConsumer).or(new Consumer<Exception>() {
                        @Override
                        public void consume(@NonNull Exception e) {
                            Logger.e(e.getLocalizedMessage());
                        }
                    }).consume(ignored);
                }
            }
        };
    }
}