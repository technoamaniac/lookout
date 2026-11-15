package dev.aman.lookout.playback;

import android.service.notification.NotificationListenerService;

/**
 * Exists only so Android lets Lookout read the active media sessions: that access is granted to
 * notification listeners. It overrides nothing, so no notification is ever read.
 */
public final class MediaAccessService extends NotificationListenerService {}
