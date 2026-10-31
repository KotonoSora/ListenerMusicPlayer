package io.hefuyi.listener.injector.module;

import dagger.Module;
import dagger.Provides;
import io.hefuyi.listener.ListenerApp;
import io.hefuyi.listener.injector.scope.PerApplication;
import io.hefuyi.listener.respository.RepositoryImpl;
import io.hefuyi.listener.respository.interfaces.Repository;

/**
 * Created by hefuyi on 2016/11/1.
 */
@Module
public class NetworkModule {
    private final ListenerApp mListenerApp;

    public NetworkModule(ListenerApp listenerApp) {
        this.mListenerApp = listenerApp;
    }

    @Provides
    @PerApplication
    Repository provideRepository() {
        return new RepositoryImpl(mListenerApp);
    }


}
