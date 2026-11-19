package dev.aman.lookout.delivery;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Android adapter: says when the TV has a working network again, so a receiver that missed something catches up at
 * once instead of at the next heartbeat. Waits for Android to validate the network, so a Wi-Fi login page does not
 * count.
 */
public final class NetworkWatcher {
    private final ConnectivityManager connectivity;
    private final ConnectivityManager.NetworkCallback callback;

    /** @param onAvailable runs on a system thread */
    public NetworkWatcher(Context context, Runnable onAvailable) {
        connectivity = context.getSystemService(ConnectivityManager.class);
        callback = new ConnectivityManager.NetworkCallback() {
            // Capabilities are reported again on unrelated changes such as signal strength; act once per network
            private Network validated;

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                // Wi-Fi can stay connected while the internet behind it drops, so losing validation counts as
                // going away and regaining it as coming back
                if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    if (network.equals(validated)) validated = null;
                    return;
                }
                if (network.equals(validated)) return;
                validated = network;
                onAvailable.run();
            }

            @Override
            public void onLost(Network network) {
                if (network.equals(validated)) validated = null;
            }
        };
    }

    public void start() {
        if (connectivity != null) connectivity.registerDefaultNetworkCallback(callback);
    }

    public void stop() {
        if (connectivity == null) return;
        try {
            connectivity.unregisterNetworkCallback(callback);
        } catch (IllegalArgumentException e) {
            // Never registered
        }
    }
}
