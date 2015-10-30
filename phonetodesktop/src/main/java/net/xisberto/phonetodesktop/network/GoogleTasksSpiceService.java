package net.xisberto.phonetodesktop.network;

import android.app.Application;

import com.octo.android.robospice.SpiceService;
import com.octo.android.robospice.persistence.CacheManager;
import com.octo.android.robospice.persistence.exception.CacheCreationException;

/**
 * Created by xisberto on 20/05/15.
 */
public class GoogleTasksSpiceService extends SpiceService {
    @Override
    public CacheManager createCacheManager(Application application) throws CacheCreationException {
        return new CacheManager();
    }
}
