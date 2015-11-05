package net.xisberto.phonetodesktop.network;

import com.octo.android.robospice.request.SpiceRequest;

import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.model.LocalTask;

import java.io.IOException;

/**
 * Created by xisberto on 20/05/15.
 */
public class TaskOptionsRequest extends SpiceRequest<LocalTask> {
    private LocalTask localTask;

    public TaskOptionsRequest(LocalTask task) {
        super(LocalTask.class);
        this.localTask = task;
    }

    @Override
    public LocalTask loadDataFromNetwork() throws IOException {
        String[] parts;
        URLOptions options = new URLOptions();

        switch (localTask.getStatus()) {
            case ADDED:
            case READY:
                if (localTask.hasOption(LocalTask.Options.OPTION_UNSHORTEN)) {
                    localTask.setStatus(LocalTask.Status.PROCESSING_UNSHORTEN).persist();
                    String links = Utils.filterLinks(localTask.getTitle().trim());
                    parts = options.unshorten(links.split(" "));
                    localTask.cache_unshorten = parts.clone();
                    localTask.setTitle(Utils.replace(localTask.getTitle(), parts))
                            .removeOption(LocalTask.Options.OPTION_UNSHORTEN);
                }
                if (localTask.hasOption(LocalTask.Options.OPTION_GETTITLES)) {
                    localTask.setStatus(LocalTask.Status.PROCESSING_TITLE).persist();
                    String links = Utils.filterLinks(localTask.getTitle()).trim();
                    parts = options.getTitles(links.split(" "));
                    localTask.cache_titles = parts.clone();
                    localTask.setTitle(Utils.appendInBrackets(localTask.getTitle(), parts))
                            .removeOption(LocalTask.Options.OPTION_GETTITLES);
                }
                localTask.setStatus(LocalTask.Status.READY)
                        .persist();
                break;
            case PROCESSING_UNSHORTEN:
                parts = Utils.filterLinks(localTask.getTitle()).split(" ");
                parts = options.unshorten(parts);
                localTask.cache_unshorten = parts.clone();
                localTask.setTitle(Utils.replace(localTask.getTitle(), parts))
                        .removeOption(LocalTask.Options.OPTION_UNSHORTEN);
                if (!localTask.hasOption(LocalTask.Options.OPTION_GETTITLES)) {
                    localTask.setStatus(LocalTask.Status.READY)
                            .persist();
                    break;
                }
                break;
            case PROCESSING_TITLE:
                parts = Utils.filterLinks(localTask.getTitle()).split(" ");
                parts = options.getTitles(parts);
                localTask.cache_titles = parts.clone();
                localTask.setTitle(Utils.appendInBrackets(localTask.getTitle(), parts))
                        .removeOption(LocalTask.Options.OPTION_GETTITLES)
                        .setStatus(LocalTask.Status.READY)
                        .persist();
                break;
        }

        return localTask;
    }
}
