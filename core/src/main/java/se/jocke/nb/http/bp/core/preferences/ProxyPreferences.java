package se.jocke.nb.http.bp.core.preferences;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import se.jocke.nb.http.bp.core.config.ProxyConfig;

public class ProxyPreferences {

    private static final Preferences PREFERENCES = NbPreferences.forModule(ProxyPreferences.class);

    public static String getBindAddress() {
        return PREFERENCES.get(ProxyConfig.BIND_ADDRESS.getKey(), "localhost");
    }

    public static int getPort() {
        return PREFERENCES.getInt(ProxyConfig.PORT.getKey(), 8686);
    }

}
